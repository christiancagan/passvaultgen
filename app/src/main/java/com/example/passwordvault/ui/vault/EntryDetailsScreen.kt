package com.example.passwordvault.ui.vault

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.passwordvault.domain.model.VaultEntry
import com.example.passwordvault.ui.components.ColoredPasswordText
import com.example.passwordvault.ui.components.PassVaultCard
import com.example.passwordvault.ui.components.PassVaultTopBar
import com.example.passwordvault.ui.components.rememberSecureClipboard
import com.example.passwordvault.ui.settings.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun EntryDetailsScreen(
    entryId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    initialTitle: String = "",
    viewModel: VaultViewModel = hiltViewModel(),
) {
    var entry by remember { mutableStateOf<VaultEntry?>(null) }
    var revealPassword by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = rememberSecureClipboard()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

    // Matched with the vault card title (same key) so it flies in instead
    // of swapping from the "Details" placeholder mid-transition.
    val sharedTitleModifier = with(sharedTransitionScope) {
        Modifier.sharedElement(
            rememberSharedContentState(key = "entry-$entryId"),
            animatedVisibilityScope = animatedVisibilityScope,
        )
    }

    LaunchedEffect(entryId) {
        entry = viewModel.getEntry(entryId)
    }

    // Auto re-hide the password for security UX: revealed text never lingers.
    LaunchedEffect(revealPassword) {
        if (revealPassword) {
            delay(30_000)
            revealPassword = false
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            PassVaultTopBar(
                title = entry?.title ?: initialTitle.ifBlank { "Details" },
                titleModifier = sharedTitleModifier,
                onBack = onBack,
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
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text("Loading…", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PassVaultCard(title = "Credential") {
                    DetailField("Account name", e.name)
                    DetailField("Username", e.username)
                    DetailField("URL", e.url)
                    DetailField("Category", e.category)
                }

                PassVaultCard(
                    title = "Password",
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ColoredPasswordText(
                            password = e.password,
                            masked = !revealPassword,
                            size = 18.sp,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { revealPassword = !revealPassword }) {
                            Icon(
                                imageVector = if (revealPassword) {
                                    Icons.Filled.VisibilityOff
                                } else {
                                    Icons.Filled.Visibility
                                },
                                contentDescription = if (revealPassword) "Hide password" else "Show password",
                            )
                        }
                        IconButton(
                            onClick = {
                                clipboard.copy(context, e.password, settings.clipboardTimeoutSeconds)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Copied! Clears in ${settings.clipboardTimeoutSeconds}s",
                                    )
                                }
                            },
                        ) {
                            Icon(
                                Icons.Filled.ContentCopy,
                                contentDescription = "Copy password",
                            )
                        }
                    }
                }

                if (e.notes.isNotBlank()) {
                    PassVaultCard(title = "Notes") {
                        Text(e.notes, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this credential?") },
            text = { Text("This cannot be undone. Export a backup first if you might need it.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        viewModel.delete(entryId)
                        onBack()
                    },
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
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
private fun DetailField(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value.ifBlank { "—" },
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
        )
    }
}
