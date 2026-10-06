package com.example.passwordvault.ui.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.passwordvault.security.backup.VaultBackupCodec
import com.example.passwordvault.ui.components.SecurePasswordField
import com.example.passwordvault.util.SecureLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var importPassword by rememberSaveable { mutableStateOf("") }

    // SAF: user picks where to save the encrypted backup (no storage permission needed).
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri: Uri? ->
        if (uri == null) {
            viewModel.consumeStagedExport()
            return@rememberLauncherForActivityResult
        }
        val bytes = viewModel.consumeStagedExport() ?: return@rememberLauncherForActivityResult
        // Write off the main thread; the bytes are already encrypted in memory.
        scope.launch(Dispatchers.IO) {
            val ok = writeBackupFile(context, uri, bytes)
            withContext(Dispatchers.Main) { viewModel.onExportWritten(ok) }
        }
    }

    // Trigger the SAF dialog once export bytes are staged.
    LaunchedEffect(uiState.stagedExport) {
        if (uiState.stagedExport != null) {
            exportLauncher.launch("vault-backup.pvault")
        }
    }

    // SAF: user picks a backup file to restore.
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val bytes = readBackupFile(context, uri)
            withContext(Dispatchers.Main) {
                if (bytes != null) viewModel.stageImport(bytes)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & restore") },
                navigationIcon = {
                    androidx.compose.material3.TextButton(onClick = onBack) { Text("Back") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Backups are encrypted with your master password. " +
                    "Restoring replaces all current entries.",
                style = MaterialTheme.typography.bodyMedium,
            )

            uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            uiState.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

            if (uiState.isBusy) {
                CircularProgressIndicator()
            }

            Button(
                onClick = { viewModel.prepareExport() },
                enabled = !uiState.isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Export encrypted backup")
            }

            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("*/*")) },
                enabled = !uiState.isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (uiState.stagedImport != null) "Choose a different file"
                    else "Choose backup file to restore",
                )
            }

            if (uiState.stagedImport != null) {
                SecurePasswordField(
                    value = importPassword,
                    onValueChange = { importPassword = it },
                    label = "Master password for this backup",
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { viewModel.restore(importPassword) },
                    enabled = !uiState.isBusy && importPassword.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Restore (replaces current entries)")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun writeBackupFile(
    context: android.content.Context,
    uri: Uri,
    bytes: ByteArray,
): Boolean {
    try {
        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
        return true
    } catch (e: Exception) {
        SecureLogger.e("backup write failed", e)
        return false
    } finally {
        bytes.fill(0)
    }
}

private fun readBackupFile(
    context: android.content.Context,
    uri: Uri,
): ByteArray? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val max = VaultBackupCodec.MAX_BACKUP_SIZE
            val out = java.io.ByteArrayOutputStream()
            val buf = ByteArray(8192)
            var total = 0
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                total += n
                if (total > max) throw IllegalArgumentException("Backup file too large")
                out.write(buf, 0, n)
            }
            out.toByteArray()
        }
    } catch (e: Exception) {
        SecureLogger.e("backup read failed", e)
        null
    }
}
