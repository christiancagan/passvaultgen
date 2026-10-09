package com.example.passwordvault.ui.vault

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.passwordvault.domain.model.VaultEntryDraft
import com.example.passwordvault.security.password.PasswordStrengthAnalyzer
import com.example.passwordvault.ui.components.HapticSwitch
import com.example.passwordvault.ui.components.PassVaultCard
import com.example.passwordvault.ui.components.PassVaultTopBar
import com.example.passwordvault.ui.components.SecurePasswordField
import com.example.passwordvault.ui.components.StrengthMeter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditEntryScreen(
    onDone: () -> Unit,
    entryId: String? = null,
    viewModel: VaultViewModel = hiltViewModel(),
) {
    // NOT rememberSaveable: username/password/notes are credential secrets and
    // saved instance state is written to disk by the system. The whole form
    // uses plain remember for consistency — rotation clears it.
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var favorite by remember { mutableStateOf(false) }

    LaunchedEffect(entryId) {
        if (entryId != null) {
            viewModel.getEntry(entryId)?.let { entry ->
                title = entry.title
                category = entry.category
                name = entry.name
                username = entry.username
                password = entry.password
                url = entry.url
                notes = entry.notes
                favorite = entry.favorite
            }
        }
    }

    val strength = remember(password) {
        if (password.isBlank()) null
        else PasswordStrengthAnalyzer().analyze(password)
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier,
        topBar = {
            PassVaultTopBar(
                title = if (entryId == null) "Add Credential" else "Edit Credential",
                onBack = onDone,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PassVaultCard(
                title = "Identity",
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(Modifier.height(12.dp))
            }

            PassVaultCard(
                title = "Credentials",
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                SecurePasswordField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    modifier = Modifier.fillMaxWidth(),
                    onGenerateClick = { password = viewModel.generatePassword() },
                )
                strength?.let {
                    Spacer(Modifier.height(4.dp))
                    StrengthMeter(
                        score = it.score,
                        label = "${it.label} — tap generate for a stronger one",
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            PassVaultCard(
                title = "Extras",
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
            ) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Favorite", style = MaterialTheme.typography.bodyMedium)
                    HapticSwitch(checked = favorite, onCheckedChange = { favorite = it })
                }
                Spacer(Modifier.height(12.dp))
            }

            Button(
                onClick = {
                    val draft = VaultEntryDraft(
                        title = title,
                        category = category,
                        name = name,
                        username = username,
                        password = password,
                        url = url,
                        notes = notes,
                        favorite = favorite,
                    )
                    if (entryId == null) {
                        viewModel.add(draft, onDone)
                    } else {
                        viewModel.update(entryId, draft, onDone)
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(if (entryId == null) "Save" else "Save changes")
            }
        }
    }
}
