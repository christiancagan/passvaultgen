package com.example.passwordvault.ui.generator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.example.passwordvault.ui.components.BubbleSlider
import com.example.passwordvault.ui.components.ColoredPasswordText
import com.example.passwordvault.ui.components.HapticSwitch
import com.example.passwordvault.ui.components.PassVaultCard
import com.example.passwordvault.ui.components.PassVaultTopBar
import com.example.passwordvault.ui.components.StrengthMeter
import com.example.passwordvault.ui.components.rememberHapticTap
import com.example.passwordvault.ui.components.rememberSecureClipboard
import com.example.passwordvault.ui.settings.SettingsViewModel
import com.example.passwordvault.ui.theme.brandGradient
import com.example.passwordvault.ui.theme.passwordStyle

/**
 * Generator pane shown inside the bottom-navigation home screen, plus the
 * standalone route wrapper. Logic stays in [GeneratorViewModel]; this file is
 * pure presentation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(
    onBack: () -> Unit,
    viewModel: GeneratorViewModel = hiltViewModel(),
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = { PassVaultTopBar(title = "Password Generator", onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            GeneratorPane(viewModel = viewModel)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GeneratorPane(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState? = null,
    viewModel: GeneratorViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = rememberSecureClipboard()
    val tap = rememberHapticTap()

    LaunchedEffect(Unit) {
        viewModel.generate()
    }

    val score = scoreFromEntropy(uiState.entropyBits)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutputCard(
            password = uiState.password,
            entropyBits = uiState.entropyBits,
            onRegenerate = { viewModel.generate() },
            onCopy = {
                clipboard.copy(context, uiState.password, settings.clipboardTimeoutSeconds)
                tap()
            },
            snackbarHostState = snackbarHostState,
            clipboardTimeout = settings.clipboardTimeoutSeconds,
        )
        StrengthMeter(score = score, label = labelForScore(score))

        PassVaultCard(title = "Mode") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Passphrase mode", style = MaterialTheme.typography.bodyLarge)
                HapticSwitch(
                    checked = uiState.passphraseMode,
                    onCheckedChange = viewModel::setPassphraseMode,
                )
            }
            if (uiState.passphraseMode) {
                BubbleSlider(
                    label = "Word count",
                    value = uiState.wordCount,
                    onValueChange = viewModel::setWordCount,
                    valueRange = 3..10,
                    steps = 6,
                )
            } else {
                BubbleSlider(
                    label = "Length",
                    value = uiState.options.length,
                    onValueChange = { viewModel.updateOptions(uiState.options.copy(length = it)) },
                    valueRange = 8..64,
                    steps = 55,
                )
                val o = uiState.options
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OptionChip("ABC", o.includeUppercase) { viewModel.updateOptions(o.copy(includeUppercase = it)) }
                    OptionChip("abc", o.includeLowercase) { viewModel.updateOptions(o.copy(includeLowercase = it)) }
                    OptionChip("123", o.includeNumbers) { viewModel.updateOptions(o.copy(includeNumbers = it)) }
                    OptionChip("#$%", o.includeSymbols) { viewModel.updateOptions(o.copy(includeSymbols = it)) }
                    OptionChip("No 0Ob6", o.excludeAmbiguous) { viewModel.updateOptions(o.copy(excludeAmbiguous = it)) }
                }
            }
        }
        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun OptionChip(label: String, selected: Boolean, onToggle: (Boolean) -> Unit) {
    val tap = rememberHapticTap()
    FilterChip(
        selected = selected,
        onClick = {
            tap()
            onToggle(!selected)
        },
        label = { Text(label) },
    )
}

@Composable
private fun OutputCard(
    password: String,
    entropyBits: Double,
    onRegenerate: () -> Unit,
    onCopy: () -> Unit,
    snackbarHostState: SnackbarHostState?,
    clipboardTimeout: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(20.dp),
        ) {
            Text(
                "Generated password",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            ColoredPasswordText(
                password = password,
                size = 22.sp,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "≈${"%.1f".format(entropyBits)} bits of entropy",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        IconButton(onClick = onRegenerate) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = "Regenerate password",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        IconButton(
                            onClick = {
                                onCopy()
                                scope.launch {
                                    snackbarHostState?.showSnackbar(
                                        "Copied! Clears in ${clipboardTimeout}s",
                                    )
                                }
                            },
                        ) {
                            Icon(
                                Icons.Filled.ContentCopy,
                                contentDescription = "Copy password",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .background(brandGradient(), MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Local only",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

private fun labelForScore(score: Int): String = when (score) {
    0 -> "Very weak"
    1 -> "Weak"
    2 -> "Fair"
    3 -> "Good"
    else -> "Strong"
}

private fun scoreFromEntropy(bits: Double): Int =
    com.example.passwordvault.ui.components.scoreFromEntropy(bits)
