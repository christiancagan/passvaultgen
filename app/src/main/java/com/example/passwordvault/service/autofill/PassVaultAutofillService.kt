package com.example.passwordvault.service.autofill

import android.app.PendingIntent
import android.app.assist.AssistStructure
import android.content.Intent
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveInfo
import android.service.autofill.SaveRequest
import android.view.View
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import com.example.passwordvault.MainActivity
import com.example.passwordvault.data.repository.VaultRepository
import com.example.passwordvault.domain.model.VaultEntryDraft
import com.example.passwordvault.security.crypto.VaultSession
import com.example.passwordvault.util.SecureLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * System autofill provider: fill AND save.
 *
 * Fill: the system sends the requesting app's package and its login form's
 * autofill fields. If the vault is unlocked, matching entries are offered as
 * datasets that fill username + password (this is the "suggest saved
 * passwords when logging into other apps" behavior). If locked, datasets are
 * gated behind an authentication tap that opens the app to unlock.
 *
 * Save: when the vault is unlocked and a form has a password field, the
 * response carries a SaveInfo, so after the user submits the form the system
 * prompts "Save to PassVaultGen". Confirming invokes onSaveRequest, which
 * reads the typed username/password and stores them as a new entry titled
 * with the other app's label. Nothing is ever offered inside our own
 * package, and entry titles are the only thing shown.
 *
 * Note: the RemoteViews dataset APIs are deprecated in favor of inline
 * (keyboard-strip) suggestions, but dropdown presentations remain fully
 * functional on all API levels and are the correct baseline for minSdk 26.
 */
@Suppress("DEPRECATION")
@AndroidEntryPoint
class PassVaultAutofillService : android.service.autofill.AutofillService() {

    @Inject
    lateinit var repository: VaultRepository

    @Inject
    lateinit var session: VaultSession

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback,
    ) {
        val structure = request.fillContexts.lastOrNull()?.structure
        if (structure == null) {
            callback.onSuccess(null)
            return
        }
        val packageName = structure.activityComponent?.packageName.orEmpty()
        if (packageName.isBlank() || packageName == packageName()) {
            callback.onSuccess(null)
            return
        }

        val fields = parseFields(structure)
        if (fields.usernameId == null && fields.passwordId == null) {
            callback.onSuccess(null)
            return
        }

        scope.launch {
            try {
                val response = withContext(Dispatchers.Default) {
                    buildResponse(packageName, fields)
                }
                if (!cancellationSignal.isCanceled) callback.onSuccess(response)
            } catch (e: Exception) {
                SecureLogger.e("autofill failed", e)
                if (!cancellationSignal.isCanceled) callback.onSuccess(null)
            }
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        val structure = request.fillContexts.lastOrNull()?.structure
        if (structure == null) {
            callback.onFailure("Nothing to save")
            return
        }
        val packageName = structure.activityComponent?.packageName.orEmpty()
        if (packageName.isBlank() || packageName == packageName()) {
            callback.onFailure("PassVaultGen cannot save into itself")
            return
        }
        val saved = collectSavedValues(structure)
        val password = saved.password?.takeIf { it.isNotBlank() }
        if (password == null) {
            callback.onFailure("No password found in this form")
            return
        }

        scope.launch {
            try {
                val unlocked = withContext(Dispatchers.Default) { session.isUnlocked.first() }
                if (!unlocked) {
                    callback.onFailure("Unlock PassVaultGen first, then save again")
                    return@launch
                }
                val appLabel = appLabelOf(packageName).ifBlank { packageName }
                withContext(Dispatchers.Default) {
                    repository.add(
                        VaultEntryDraft(
                            title = appLabel,
                            category = "Autofill",
                            name = appLabel,
                            username = saved.username.orEmpty(),
                            password = password,
                            url = "",
                            notes = "",
                            favorite = false,
                        ),
                    )
                }
                callback.onSuccess()
            } catch (e: Exception) {
                SecureLogger.e("autofill save failed", e)
                callback.onFailure("Could not save to PassVaultGen")
            }
        }
    }

    private fun packageName(): String = applicationContext.packageName

    private data class SavedValues(val username: String?, val password: String?)

    private fun collectSavedValues(structure: AssistStructure): SavedValues {
        var username: String? = null
        var password: String? = null
        for (i in 0 until structure.windowNodeCount) {
            val root = structure.getWindowNodeAt(i).rootViewNode ?: continue
            traverse(root) { node ->
                val hints = node.autofillHints ?: return@traverse
                val autofillValue = node.autofillValue ?: return@traverse
                val text = autofillValue.takeIf { it.isText }?.textValue?.toString()?.takeIf { it.isNotEmpty() }
                    ?: return@traverse
                when {
                    password == null &&
                        hints.any { it == View.AUTOFILL_HINT_PASSWORD || it == "newPassword" } ->
                        password = text
                    username == null &&
                        hints.any { it == View.AUTOFILL_HINT_USERNAME || it == View.AUTOFILL_HINT_EMAIL_ADDRESS } ->
                        username = text
                }
            }
        }
        return SavedValues(username, password)
    }

    private suspend fun buildResponse(
        packageName: String,
        fields: LoginFields,
    ): FillResponse? {
        val unlocked = session.isUnlocked.first()
        val appLabel = appLabelOf(packageName)

        val candidates = if (unlocked) {
            repository.getAllDecrypted().map {
                AutofillCandidate(id = it.id, title = it.title, url = it.url)
            }
        } else {
            // Locked: summaries carry no secrets, so title matching is safe.
            repository.observeAll().first().map {
                AutofillCandidate(id = it.id, title = it.title, url = "")
            }
        }
        val matches = AutofillMatcher.findMatches(candidates, packageName, appLabel)
            .take(MAX_DATASETS)
        if (matches.isEmpty()) {
            if (!unlocked) {
                // Locked: offer the unlock row so the user can open the vault.
                return FillResponse.Builder()
                    .setAuthentication(
                        fields.allIds().toTypedArray(),
                        unlockIntentSender(),
                        presentation("Unlock PassVaultGen"),
                    )
                    .build()
            }
            // Unlocked with no saved match: this may be a NEW login, so
            // advertise save without leaking any entry titles.
            if (fields.passwordId == null) return null
            return FillResponse.Builder()
                .setSaveInfo(saveInfo(fields))
                .build()
        }

        val builder = FillResponse.Builder()
        for (match in matches) {
            builder.addDataset(
                if (unlocked) {
                    val entry = repository.getEntry(match.id) ?: continue
                    Dataset.Builder(presentation(entry.title)).apply {
                        fields.usernameId?.let { setValue(it, AutofillValue.forText(entry.username)) }
                        fields.passwordId?.let { setValue(it, AutofillValue.forText(entry.password)) }
                    }.build()
                } else {
                    Dataset.Builder(presentation(match.title)).apply {
                        setAuthentication(unlockIntentSender())
                    }.build()
                },
            )
        }
        if (unlocked && fields.passwordId != null) {
            // Update / signup forms can produce new values: allow saving too.
            builder.setSaveInfo(saveInfo(fields))
        }
        return builder.build()
    }

    private fun saveInfo(fields: LoginFields): SaveInfo {
        val category = if (fields.usernameId != null) {
            SaveInfo.SAVE_DATA_TYPE_PASSWORD or SaveInfo.SAVE_DATA_TYPE_USERNAME
        } else {
            SaveInfo.SAVE_DATA_TYPE_PASSWORD
        }
        val builder = SaveInfo.Builder(category, fields.allIds().toTypedArray())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setDescription("Save to PassVaultGen")
        }
        return builder.build()
    }

    private fun appLabelOf(packageName: String): String {
        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            ""
        }
    }

    private fun unlockIntentSender() = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_IMMUTABLE,
    ).intentSender

    private fun presentation(text: String): RemoteViews =
        RemoteViews("android", android.R.layout.simple_list_item_1).apply {
            setTextViewText(android.R.id.text1, text)
        }

    private data class LoginFields(
        val usernameId: AutofillId?,
        val passwordId: AutofillId?,
    ) {
        fun allIds(): List<AutofillId> = listOfNotNull(usernameId, passwordId)
    }

    private fun parseFields(structure: AssistStructure): LoginFields {
        var usernameId: AutofillId? = null
        var passwordId: AutofillId? = null
        for (i in 0 until structure.windowNodeCount) {
            val root = structure.getWindowNodeAt(i).rootViewNode ?: continue
            traverse(root) { node ->
                val hints = node.autofillHints ?: return@traverse
                val id = node.autofillId ?: return@traverse
                when {
                    hints.any { it == View.AUTOFILL_HINT_PASSWORD || it == "newPassword" } ->
                        if (passwordId == null) passwordId = id
                    hints.any { it == View.AUTOFILL_HINT_USERNAME || it == View.AUTOFILL_HINT_EMAIL_ADDRESS } ->
                        if (usernameId == null) usernameId = id
                }
            }
        }
        return LoginFields(usernameId, passwordId)
    }

    private fun traverse(node: AssistStructure.ViewNode, visit: (AssistStructure.ViewNode) -> Unit) {
        visit(node)
        for (i in 0 until node.childCount) {
            node.getChildAt(i)?.let { traverse(it, visit) }
        }
    }

    companion object {
        const val MAX_DATASETS = 5
    }
}
