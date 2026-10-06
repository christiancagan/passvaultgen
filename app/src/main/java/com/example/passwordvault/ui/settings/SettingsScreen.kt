package com.example.passwordvault.ui.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.autofill.AutofillManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onBackup: () -> Unit,
    onChangePassword: () -> Unit,
    onVaultDeleted: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val activity = LocalContext.current as? FragmentActivity
    val context = LocalContext.current

    var autofillEnabled by rememberSaveable { mutableStateOf(isAutofillEnabled(context)) }
    val autofillLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        // Enabling happens in system settings; refresh our status on return.
        autofillEnabled = isAutofillEnabled(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingToggle(
                label = "Auto-lock on background",
                checked = settings.autoLockEnabled,
                onCheckedChange = viewModel::setAutoLockEnabled,
            )
            SettingToggle(
                label = "Biometric unlock",
                checked = settings.biometricEnabled,
                onCheckedChange = { enabled ->
                    viewModel.setBiometricEnabled(enabled, activity)
                },
            )
            OutlinedButton(onClick = onBackup, modifier = Modifier.fillMaxWidth()) {
                Text("Backup & restore")
            }
            OutlinedButton(onClick = onChangePassword, modifier = Modifier.fillMaxWidth()) {
                Text("Change master password")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Autofill service: ${if (autofillEnabled) "on" else "off"}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (!autofillEnabled) {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).apply {
                                data = android.net.Uri.parse("package:${context.packageName}")
                            }
                            autofillLauncher.launch(intent)
                        },
                    ) { Text("Enable") }
                }
            }
            OutlinedButton(
                onClick = { confirmDelete = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Delete vault & all entries", color = MaterialTheme.colorScheme.error)
            }
            actionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Clipboard auto-clear: ${settings.clipboardTimeoutSeconds}s",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            viewModel.setClipboardTimeoutSeconds(
                                (settings.clipboardTimeoutSeconds - 5).coerceAtLeast(5),
                            )
                        },
                    ) { Text("−") }
                    IconButton(
                        onClick = {
                            viewModel.setClipboardTimeoutSeconds(
                                (settings.clipboardTimeoutSeconds + 5).coerceAtMost(120),
                            )
                        },
                    ) { Text("+") }
                }
            }
            Text(
                "Screenshots are disabled app-wide. No analytics. No cloud sync. Your data never leaves this device.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }

    if (confirmDelete) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete the entire vault?") },
            text = {
                Text(
                    "This erases the master password, biometric unlock, and ALL " +
                        "credentials. It cannot be undone. Export a backup first if " +
                        "you might need your data.",
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        confirmDelete = false
                        viewModel.deleteVault(onVaultDeleted)
                    },
                ) { Text("Delete everything", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { confirmDelete = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun SettingToggle(    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** True when the user selected PassVaultGen as the system autofill service. */
private fun isAutofillEnabled(context: Context): Boolean {
    val manager = context.getSystemService(AutofillManager::class.java) ?: return false
    return manager.hasEnabledAutofillServices()
}
