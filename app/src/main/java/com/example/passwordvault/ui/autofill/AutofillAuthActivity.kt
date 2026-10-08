package com.example.passwordvault.ui.autofill

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.service.autofill.Dataset
import android.view.WindowManager
import android.view.autofill.AutofillId
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.passwordvault.data.repository.VaultRepository
import com.example.passwordvault.ui.auth.AuthViewModel
import com.example.passwordvault.ui.components.PassVaultCard
import com.example.passwordvault.ui.components.SecurePasswordField
import com.example.passwordvault.ui.settings.SettingsViewModel
import com.example.passwordvault.ui.theme.PassVaultTheme
import com.example.passwordvault.util.SecureLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/**
 * Unlock gate for autofill disclosure. The framework fires this activity when
 * the user taps an auth-gated dataset row; the requesting app's UI never sees
 * a password. The vault is unlocked here (biometric first, master password as
 * fallback) and the filled [Dataset] is handed back to the framework via
 * [AutofillManager.EXTRA_AUTHENTICATION_RESULT]. A fresh unlock is required
 * every time, even when the vault is already open elsewhere.
 *
 * The tap that opened this activity already identified the entry, and the entry
 * title is a non-secret summary, so cancelled results disclose nothing.
 */
@AndroidEntryPoint
class AutofillAuthActivity : FragmentActivity() {

    @Inject
    lateinit var repository: VaultRepository

    private val finished = AtomicBoolean(false)

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val debuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (!debuggable) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE,
            )
        }
        val entryId = intent.getStringExtra(EXTRA_ENTRY_ID).orEmpty()
        val entryTitle = intent.getStringExtra(EXTRA_ENTRY_TITLE).orEmpty()
        val usernameId = intent.getParcelableExtra<AutofillId>(EXTRA_USERNAME_ID)
        val passwordId = intent.getParcelableExtra<AutofillId>(EXTRA_PASSWORD_ID)

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
            PassVaultTheme(mode = settings.themeMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center,
                ) {
                    AutofillUnlockDialog(
                        entryTitle = entryTitle,
                        activity = this@AutofillAuthActivity,
                        onAuthenticated = { finishWithDataset(entryId, usernameId, passwordId) },
                        onCancel = {
                            setResult(RESULT_CANCELED)
                            finish()
                        },
                    )
                }
            }
        }
    }

    // RemoteViews datasets are deprecated in favor of inline suggestions, but
    // remain the correct baseline for minSdk 26 (same as the service).
    @Suppress("DEPRECATION")
    private fun finishWithDataset(
        entryId: String,
        usernameId: AutofillId?,
        passwordId: AutofillId?,
    ) {
        if (!finished.compareAndSet(false, true)) return
        lifecycleScope.launch {
            try {
                val entry = withContext(Dispatchers.Default) { repository.getEntry(entryId) }
                if (entry == null) {
                    setResult(RESULT_CANCELED)
                } else {
                    val dataset = Dataset.Builder(presentation(entry.title)).apply {
                        usernameId?.let { setValue(it, AutofillValue.forText(entry.username)) }
                        passwordId?.let { setValue(it, AutofillValue.forText(entry.password)) }
                    }.build()
                    setResult(
                        RESULT_OK,
                        Intent().putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, dataset),
                    )
                }
            } catch (e: Exception) {
                SecureLogger.e("autofill auth failed", e)
                setResult(RESULT_CANCELED)
            }
            finish()
        }
    }

    private fun presentation(text: String): RemoteViews =
        RemoteViews("android", android.R.layout.simple_list_item_1).apply {
            setTextViewText(android.R.id.text1, text)
        }

    companion object {
        const val EXTRA_ENTRY_ID = "com.example.passwordvault.autofill.ENTRY_ID"
        const val EXTRA_ENTRY_TITLE = "com.example.passwordvault.autofill.ENTRY_TITLE"
        const val EXTRA_USERNAME_ID = "com.example.passwordvault.autofill.USERNAME_ID"
        const val EXTRA_PASSWORD_ID = "com.example.passwordvault.autofill.PASSWORD_ID"
    }
}

@Composable
private fun AutofillUnlockDialog(
    entryTitle: String,
    activity: FragmentActivity,
    onAuthenticated: () -> Unit,
    onCancel: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val unlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()
    var passwordMode by rememberSaveable { mutableStateOf(false) }
    var password by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }

    // Biometric first; any failure or dismissal falls back to master password.
    LaunchedEffect(passwordMode) {
        if (!passwordMode) {
            viewModel.unlockWithBiometrics(activity) { ok ->
                if (ok) onAuthenticated() else passwordMode = true
            }
        }
    }
    // Master-password path: unlock() reports through uiState/session, never a
    // callback, so complete only after an explicit submit unlocked the vault.
    LaunchedEffect(submitted, unlocked, uiState.isLoading) {
        if (submitted && unlocked && !uiState.isLoading) onAuthenticated()
    }

    PassVaultCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
    ) {
        Text("Unlock to autofill", style = MaterialTheme.typography.titleLarge)
        if (entryTitle.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                entryTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(16.dp))
        if (!passwordMode) {
            Text(
                "Confirm it is you to fill this password.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { passwordMode = true }) {
                Text("Use master password instead")
            }
        } else {
            SecurePasswordField(
                value = password,
                onValueChange = { password = it },
                label = "Master password",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            uiState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }
            Button(
                onClick = {
                    submitted = true
                    viewModel.unlock(password)
                },
                enabled = !uiState.isLoading && password.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                } else {
                    Text("Unlock")
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel")
        }
    }
}
