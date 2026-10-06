package com.example.passwordvault.ui.vault

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.passwordvault.domain.model.VaultEntryDraft
import com.example.passwordvault.security.password.PasswordStrengthAnalyzer
import com.example.passwordvault.ui.components.PassVaultTopBar
import com.example.passwordvault.ui.components.SecurePasswordField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditEntryScreen(
    onDone: () -> Unit,
    entryId: String? = null,
    viewModel: VaultViewModel = hiltViewModel(),
) {
    var title by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var url by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var favorite by rememberSaveable { mutableStateOf(false) }

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

    Scaffold(
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
            val strength = remember(password) {
                if (password.isBlank()) null
                else PasswordStrengthAnalyzer().analyze(password)
            }
            strength?.let {
                Text(
                    "Strength: ${it.label} — tap the generate button for a stronger one",
                    style = MaterialTheme.typography.bodySmall,
                    color = when (it.score) {
                        0, 1 -> MaterialTheme.colorScheme.error
                        2 -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.tertiary
                    },
                )
            }
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = favorite, onCheckedChange = { favorite = it })
                Text("Favorite", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(8.dp))
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
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save")
            }
        }
    }
}
