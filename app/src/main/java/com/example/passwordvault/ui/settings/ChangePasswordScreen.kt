package com.example.passwordvault.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.passwordvault.security.password.PasswordStrengthAnalyzer
import com.example.passwordvault.ui.components.PassVaultCard
import com.example.passwordvault.ui.components.PassVaultTopBar
import com.example.passwordvault.ui.components.SecurePasswordField
import com.example.passwordvault.ui.components.StrengthMeter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    onDone: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()
    // NOT rememberSaveable: saved state is persisted to disk; passwords
    // must never be written there.
    var current by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var isBusy by rememberSaveable { mutableStateOf(false) }

    val strength = remember(newPassword) {
        if (newPassword.isEmpty()) null else PasswordStrengthAnalyzer().analyze(newPassword)
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            PassVaultTopBar(title = "Change master password", onBack = onDone)
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Your entries are not re-encrypted: only the vault key is re-wrapped. " +
                    "Biometric unlock keeps working.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PassVaultCard(title = "Re-wrap the vault key") {
                SecurePasswordField(
                    value = current,
                    onValueChange = { current = it },
                    label = "Current master password",
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                SecurePasswordField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = "New master password (min 8 characters)",
                    modifier = Modifier.fillMaxWidth(),
                    onGenerateClick = {
                        val generated = viewModel.generatePassword()
                        newPassword = generated
                        confirm = generated
                    },
                )
                strength?.let {
                    Spacer(Modifier.height(4.dp))
                    StrengthMeter(score = it.score, label = it.label)
                    Spacer(Modifier.height(4.dp))
                }
                SecurePasswordField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    label = "Confirm new password",
                    modifier = Modifier.fillMaxWidth(),
                )
                actionError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = {
                        isBusy = true
                        viewModel.changePassword(current, newPassword, confirm) { ok ->
                            isBusy = false
                            if (ok) {
                                current = ""
                                newPassword = ""
                                confirm = ""
                                onDone()
                            }
                        }
                    },
                    enabled = !isBusy && current.isNotEmpty() && newPassword.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(modifier = Modifier.height(20.dp))
                    } else {
                        Text("Change password")
                    }
                }
            }
        }
    }
}
