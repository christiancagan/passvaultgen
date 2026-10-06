package com.example.passwordvault.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.passwordvault.ui.components.SecurePasswordField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    onDone: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()
    var current by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var isBusy by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Change master password") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Your entries are not re-encrypted: only the vault key is re-wrapped. " +
                    "Biometric unlock keeps working.",
                style = MaterialTheme.typography.bodyMedium,
            )
            SecurePasswordField(
                value = current,
                onValueChange = { current = it },
                label = "Current master password",
                modifier = Modifier.fillMaxWidth(),
            )
            SecurePasswordField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = "New master password (min 8 characters)",
                modifier = Modifier.fillMaxWidth(),
            )
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
                modifier = Modifier.fillMaxWidth(),
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
