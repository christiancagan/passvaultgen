package com.example.passwordvault.ui.generator

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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.passwordvault.security.password.PasswordOptions
import com.example.passwordvault.ui.components.PassVaultTopBar
import com.example.passwordvault.ui.settings.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(
    onBack: () -> Unit,
    viewModel: GeneratorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.generate()
    }

    Scaffold(
        topBar = {
            PassVaultTopBar(title = "Password Generator", onBack = onBack)
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
            OutlinedTextField(
                value = uiState.password,
                onValueChange = {},
                label = { Text("Generated password") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
            )
            Text(
                "Estimated entropy: ${"%.1f".format(uiState.entropyBits)} bits",
                style = MaterialTheme.typography.bodySmall,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { viewModel.generate() }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(Modifier.height(0.dp))
                    Text("Regenerate")
                }
                IconButton(onClick = {
                    copyToClipboard(
                        context,
                        scope,
                        uiState.password,
                        settings.clipboardTimeoutSeconds,
                    )
                }) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy")
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = uiState.passphraseMode,
                            onCheckedChange = { viewModel.setPassphraseMode(it) },
                        )
                        Text("Passphrase mode")
                    }

                    if (uiState.passphraseMode) {
                        Text("Word count: ${uiState.wordCount}")
                        Slider(
                            value = uiState.wordCount.toFloat(),
                            onValueChange = { viewModel.setWordCount(it.roundToInt()) },
                            valueRange = 3f..10f,
                            steps = 6,
                        )
                    } else {
                        Text("Length: ${uiState.options.length}")
                        Slider(
                            value = uiState.options.length.toFloat(),
                            onValueChange = { viewModel.updateOptions(uiState.options.copy(length = it.roundToInt())) },
                            valueRange = 8f..64f,
                            steps = 55,
                        )
                        val o = uiState.options
                        OptionCheckbox("Uppercase", o.includeUppercase) { viewModel.updateOptions(o.copy(includeUppercase = it)) }
                        OptionCheckbox("Lowercase", o.includeLowercase) { viewModel.updateOptions(o.copy(includeLowercase = it)) }
                        OptionCheckbox("Numbers", o.includeNumbers) { viewModel.updateOptions(o.copy(includeNumbers = it)) }
                        OptionCheckbox("Symbols", o.includeSymbols) { viewModel.updateOptions(o.copy(includeSymbols = it)) }
                        OptionCheckbox("Exclude ambiguous", o.excludeAmbiguous) { viewModel.updateOptions(o.copy(excludeAmbiguous = it)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionCheckbox(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label)
    }
}

private fun copyToClipboard(
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope,
    value: String,
    timeoutSeconds: Int,
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    // See EntryDetailsScreen: the label must not reveal what was copied.
    clipboard.setPrimaryClip(ClipData.newPlainText("PassVaultGen", value))
    scope.launch {
        delay(timeoutSeconds.coerceIn(5, 300) * 1000L)
        if (clipboard.primaryClip?.getItemAt(0)?.text?.toString() == value) {
            clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
        }
    }
}
