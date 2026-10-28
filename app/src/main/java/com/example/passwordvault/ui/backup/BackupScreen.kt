package com.example.passwordvault.ui.backup

import android.content.Intent
import android.net.Uri
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.passwordvault.security.backup.VaultBackupCodec
import com.example.passwordvault.ui.components.PassVaultCard
import com.example.passwordvault.ui.components.PassVaultTopBar
import com.example.passwordvault.ui.components.SecurePasswordField
import com.example.passwordvault.util.SecureLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isUnlocked by viewModel.isVaultUnlocked.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var importPassword by remember { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri: Uri? ->
        if (uri == null) {
            viewModel.consumeStagedExport()
            return@rememberLauncherForActivityResult
        }
        val bytes = viewModel.consumeStagedExport() ?: return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val ok = writeBackupFile(context, uri, bytes)
            withContext(Dispatchers.Main) { viewModel.onExportWritten(ok) }
        }
    }

    LaunchedEffect(uiState.stagedExport) {
        if (uiState.stagedExport != null) {
            val fileName = "vault-backup-${LocalDate.now()}.pvault"
            exportLauncher.launch(fileName)
        }
    }

    val emailLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.onEmailHandoff(true)
    }

    LaunchedEffect(uiState.stagedEmailExport) {
        val staged = uiState.stagedEmailExport ?: return@LaunchedEffect
        val bytes = viewModel.consumeStagedEmailExport() ?: return@LaunchedEffect
        val recipient = uiState.email.trim()
        scope.launch(Dispatchers.IO) {
            val file = writeEmailBackupFile(context, bytes)
            withContext(Dispatchers.Main) {
                if (file == null) {
                    viewModel.onEmailHandoff(false)
                    return@withContext
                }
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file,
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/octet-stream"
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
                    putExtra(Intent.EXTRA_SUBJECT, "PassVault encrypted backup")
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "Encrypted PassVault backup attached. Restore requires your master password.",
                    )
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try {
                    emailLauncher.launch(Intent.createChooser(intent, "Send backup"))
                    viewModel.onEmailHandoff(true)
                } catch (e: Exception) {
                    SecureLogger.e("email handoff failed", e)
                    viewModel.onEmailHandoff(false)
                }
            }
        }
    }

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
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            PassVaultTopBar(title = "Backup & restore", onBack = onBack)
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PassVaultCard(
                title = "Encrypted export",
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                titleColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Text(
                    "Backups are encrypted with your master password. " +
                        "Restoring replaces all current entries.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }

            uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            uiState.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

            if (uiState.isBusy) {
                CircularProgressIndicator()
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (uiState.deliveryMode == BackupDeliveryMode.FILE) {
                    Button(
                        onClick = { viewModel.setDeliveryMode(BackupDeliveryMode.FILE) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Save to device or cloud") }
                } else {
                    OutlinedButton(
                        onClick = { viewModel.setDeliveryMode(BackupDeliveryMode.FILE) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Save to device or cloud") }
                }
                if (uiState.deliveryMode == BackupDeliveryMode.EMAIL) {
                    Button(
                        onClick = { viewModel.setDeliveryMode(BackupDeliveryMode.EMAIL) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Send by email") }
                } else {
                    OutlinedButton(
                        onClick = { viewModel.setDeliveryMode(BackupDeliveryMode.EMAIL) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Send by email") }
                }
            }

            if (uiState.deliveryMode == BackupDeliveryMode.EMAIL) {
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = { viewModel.setEmail(it) },
                    label = { Text("Recipient email") },
                    isError = uiState.emailError != null,
                    supportingText = {
                        uiState.emailError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "The backup is encrypted. Email is not secure; use a strong master password.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!isUnlocked) {
                Text(
                    "Unlock the vault before generating a backup.",
                    color = MaterialTheme.colorScheme.error,
                )
            }

            val emailValid = uiState.email.isNotBlank() &&
                Patterns.EMAIL_ADDRESS.matcher(uiState.email.trim()).matches()
            val canGenerate = !uiState.isBusy && isUnlocked &&
                (uiState.deliveryMode == BackupDeliveryMode.FILE || emailValid)

            Button(
                onClick = { viewModel.prepareExport() },
                enabled = canGenerate,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Generate backup")
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

private fun writeEmailBackupFile(
    context: android.content.Context,
    bytes: ByteArray,
): File? {
    return try {
        val dir = File(context.cacheDir, "backups").apply { mkdirs() }
        cleanupOldBackupFiles(dir)
        val file = File(dir, "vault-backup-${LocalDate.now()}.pvault")
        FileOutputStream(file).use { it.write(bytes) }
        bytes.fill(0)
        file
    } catch (e: Exception) {
        SecureLogger.e("backup email write failed", e)
        bytes.fill(0)
        null
    }
}

private fun cleanupOldBackupFiles(dir: File) {
    val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
    dir.listFiles()?.forEach { if (it.lastModified() < cutoff) it.delete() }
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