package com.example.passwordvault.service.autofill

import android.app.PendingIntent
import android.app.assist.AssistStructure
import android.content.Intent
import android.os.CancellationSignal
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.view.View
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import com.example.passwordvault.MainActivity
import com.example.passwordvault.data.repository.VaultRepository
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
 * System autofill provider (fill-only in v1; saving new logins is roadmap).
 *
 * Flow: the system sends the requesting app's package and its login form's
 * autofill fields. If the vault is unlocked, matching entries are offered as
 * datasets that fill username + password. If locked, datasets are gated behind
 * an authentication tap that opens the app to unlock (the user then re-invokes
 * autofill). Nothing is ever offered inside our own package, and entry titles
 * are the only thing shown — passwords stay invisible until filled.
 *
 * Note: the RemoteViews dataset APIs are deprecated in favor of inline
 * (keyboard-strip) suggestions, but dropdown presentations remain fully
 * functional on all API levels and are the correct baseline for minSdk 26.
 * Inline suggestions are a future enhancement, not a correctness issue.
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
        // v1 does not advertise save (no SaveInfo is set), so this is a no-op.
        callback.onSuccess()
    }

    private fun packageName(): String = applicationContext.packageName

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
            // Locked with no title matches: still offer an unlock row so the user
            // can open the vault; unlocked with no matches: stay silent (titles
            // of unrelated entries must not leak into a foreign app).
            if (!unlocked) {
                return FillResponse.Builder()
                    .setAuthentication(
                        fields.allIds().toTypedArray(),
                        unlockIntentSender(),
                        presentation("Unlock PassVaultGen"),
                    )
                    .build()
            }
            return null
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
