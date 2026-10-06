package com.example.passwordvault.ui.vault

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.passwordvault.domain.model.VaultEntry
import com.example.passwordvault.ui.settings.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDetailsScreen(
    entryId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    viewModel: VaultViewModel = hiltViewModel(),
) {
    var entry by remember { mutableStateOf<VaultEntry?>(null) }
    var revealPassword by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

    LaunchedEffect(entryId) {
        entry = viewModel.getEntry(entryId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(entry?.title ?: "Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                },
            )
        },
    ) { padding ->
        val e = entry
        if (e == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Loading…")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DetailField("Account name", e.name)
                DetailField("Username", e.username)
                DetailField("URL", e.url)
                DetailField("Category", e.category)

                Text("Password", style = MaterialTheme.typography.labelMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (revealPassword) e.password else "••••••••••••",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { revealPassword = !revealPassword }) {
                        Icon(
                            imageVector = if (revealPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (revealPassword) "Hide password" else "Show password",
                        )
                    }
                    IconButton(onClick = {
                        copyToClipboard(
                            context,
                            scope,
                            e.password,
                            settings.clipboardTimeoutSeconds,
                        )
                    }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy password")
                    }
                }

                if (e.notes.isNotBlank()) {
                    DetailField("Notes", e.notes)
                }
            }
        }
    }

    if (confirmDelete) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this credential?") },
            text = { Text("This cannot be undone. Export a backup first if you might need it.") },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        confirmDelete = false
                        viewModel.delete(entryId)
                        onBack()
                    },
                ) { Text("Delete") }
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
private fun DetailField(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun copyToClipboard(
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope,
    value: String,
    timeoutSeconds: Int,
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    // Label is the app name, not "password": Android 13+ shows the label in the
    // system copy overlay, and it must not reveal what was copied.
    clipboard.setPrimaryClip(ClipData.newPlainText("PassVaultGen", value))
    // Auto-clear the clipboard after the user-configured period.
    scope.launch {
        delay(timeoutSeconds.coerceIn(5, 300) * 1000L)
        if (clipboard.primaryClip?.getItemAt(0)?.text?.toString() == value) {
            clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
        }
    }
}
