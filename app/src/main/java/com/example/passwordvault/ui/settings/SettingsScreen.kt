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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.passwordvault.ui.components.PassVaultTopBar
import com.example.passwordvault.ui.components.ThemeModeSelector

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
            PassVaultTopBar(title = "Settings", onBack = onBack)
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingsSection(
                title = "Appearance",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Text(
                    "Theme",
                    style = MaterialTheme.typography.bodyMedium,
                )
                ThemeModeSelector(
                    selected = settings.themeMode,
                    onSelect = viewModel::setThemeMode,
                )
            }

            SettingsSection(title = "Security") {
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
                OutlinedButton(onClick = onChangePassword, modifier = Modifier.fillMaxWidth()) {
                    Text("Change master password")
                }
            }

            SettingsSection(title = "Data") {
                OutlinedButton(onClick = onBackup, modifier = Modifier.fillMaxWidth()) {
                    Text("Backup & restore")
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
                        TextButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).apply {
                                    data = android.net.Uri.parse("package:${context.packageName}")
                                }
                                autofillLauncher.launch(intent)
                            },
                        ) { Text("Enable") }
                    }
                }
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
                        ) {
                            Icon(Icons.Filled.Remove, contentDescription = "Decrease timeout")
                        }
                        IconButton(
                            onClick = {
                                viewModel.setClipboardTimeoutSeconds(
                                    (settings.clipboardTimeoutSeconds + 5).coerceAtMost(120),
                                )
                            },
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Increase timeout")
                        }
                    }
                }
            }

            SettingsSection(
                title = "Danger zone",
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ) {
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Delete vault & all entries", color = MaterialTheme.colorScheme.error)
                }
            }

            actionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Text(
                "Screenshots are disabled app-wide. No analytics. No cloud sync. Your data never leaves this device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                TextButton(
                    onClick = {
                        confirmDelete = false
                        viewModel.deleteVault(onVaultDeleted)
                    },
                ) { Text("Delete everything", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            content()
        }
    }
}

@Composable
private fun SettingToggle(
    label: String,
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
