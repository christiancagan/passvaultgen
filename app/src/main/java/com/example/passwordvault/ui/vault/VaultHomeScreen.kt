package com.example.passwordvault.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.passwordvault.domain.model.VaultEntrySummary
import com.example.passwordvault.ui.components.ColoredPasswordText
import com.example.passwordvault.ui.components.EmptyState
import com.example.passwordvault.ui.components.PassVaultCard
import com.example.passwordvault.ui.components.SwipeableRow
import com.example.passwordvault.ui.components.rememberSecureClipboard
import com.example.passwordvault.ui.settings.SettingsViewModel
import kotlinx.coroutines.launch

/**
 * Vault list pane shown inside the bottom-navigation home screen.
 * Cards reveal masked passwords on tap, swipe right to copy, swipe left to
 * delete. No scaffold of its own.
 */
@Composable
fun VaultPane(
    onDetails: (String) -> Unit,
    snackbarHostState: SnackbarHostState? = null,
    viewModel: VaultViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQueryState.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = rememberSecureClipboard()

    val revealed = remember { mutableStateMapOf<String, String>() }
    var pendingDelete by remember { mutableStateOf<VaultEntrySummary?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        PassVaultCard(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentPadding = PaddingValues(12.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                label = { Text("Search") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        if (entries.isEmpty()) {
            EmptyState(
                title = "No credentials yet.",
                subtitle = "Tap + to store your first secret.",
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(entries, key = { it.id }) { entry ->
                    SwipeableRow(
                        onSwipeRight = {
                            scope.launch {
                                val full = viewModel.getEntry(entry.id) ?: return@launch
                                clipboard.copy(context, full.password, settings.clipboardTimeoutSeconds)
                                snackbarHostState?.showSnackbar(
                                    "Copied! Clears in ${settings.clipboardTimeoutSeconds}s",
                                )
                            }
                        },
                        onSwipeLeft = { pendingDelete = entry },
                    ) {
                        EntryCard(
                            entry = entry,
                            revealedPassword = revealed[entry.id],
                            onClick = { onDetails(entry.id) },
                            onToggleReveal = {
                                if (revealed.containsKey(entry.id)) {
                                    revealed.remove(entry.id)
                                } else {
                                    scope.launch {
                                        val full = viewModel.getEntry(entry.id) ?: return@launch
                                        revealed[entry.id] = full.password
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this credential?") },
            text = { Text("\"${target.title}\" will be removed permanently. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        revealed.remove(target.id)
                        viewModel.delete(target.id)
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun EntryCard(
    entry: VaultEntrySummary,
    revealedPassword: String?,
    onClick: () -> Unit,
    onToggleReveal: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val (avatarContainer, avatarOn) = avatarColors(entry.title, scheme)
    PassVaultCard(
        modifier = Modifier.clickable(onClick = onClick),
        containerColor = scheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(avatarContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = entry.title.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    color = avatarOn,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (entry.favorite) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = "Favorite",
                            tint = scheme.tertiary,
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .size(18.dp),
                        )
                    }
                }
                Spacer(Modifier.size(2.dp))
                ColoredPasswordText(
                    password = revealedPassword.orEmpty(),
                    masked = revealedPassword == null,
                    size = 13.sp,
                )
                if (entry.category.isNotBlank()) {
                    AssistChip(
                        onClick = onClick,
                        label = { Text(entry.category, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
            IconButton(onClick = onToggleReveal) {
                Icon(
                    imageVector = if (revealedPassword != null) {
                        Icons.Filled.VisibilityOff
                    } else {
                        Icons.Filled.Visibility
                    },
                    contentDescription = if (revealedPassword != null) {
                        "Hide password"
                    } else {
                        "Show password for ${entry.title}"
                    },
                )
            }
        }
    }
}

private fun avatarColors(title: String, scheme: ColorScheme): Pair<Color, Color> =
    when (title.hashCode() and 3) {
        0 -> scheme.primaryContainer to scheme.onPrimaryContainer
        1 -> scheme.secondaryContainer to scheme.onSecondaryContainer
        2 -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        else -> scheme.surfaceContainerHighest to scheme.onSurface
    }
