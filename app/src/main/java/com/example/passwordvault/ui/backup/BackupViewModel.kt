package com.example.passwordvault.ui.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Patterns
import com.example.passwordvault.domain.usecase.ExportVaultUseCase
import com.example.passwordvault.domain.usecase.ImportVaultUseCase
import com.example.passwordvault.security.crypto.VaultSession
import com.example.passwordvault.util.SecureLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class BackupDeliveryMode { FILE, EMAIL }

data class BackupUiState(
    val isBusy: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    /** Selected delivery mode. FILE (device or cloud via SAF) is the default. */
    val deliveryMode: BackupDeliveryMode = BackupDeliveryMode.FILE,
    /** Recipient email for the EMAIL mode. Never persisted. */
    val email: String = "",
    /** Validation error for [email], if any. */
    val emailError: String? = null,
    /** Bytes staged for the SAF create-document flow. Consumed (cleared) after writing. */
    val stagedExport: ByteArray? = null,
    /** Bytes staged for the email attachment flow. Consumed (cleared) after writing. */
    val stagedEmailExport: ByteArray? = null,
    /** Bytes read from the user-picked import file, awaiting the master password. */
    val stagedImport: ByteArray? = null,
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val exportVault: ExportVaultUseCase,
    private val importVault: ImportVaultUseCase,
    private val session: VaultSession,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    /** Observable vault lock state; backup generation requires an unlocked vault. */
    val isVaultUnlocked: StateFlow<Boolean> = session.isUnlocked

    fun setDeliveryMode(mode: BackupDeliveryMode) {
        _uiState.value = _uiState.value.copy(
            deliveryMode = mode,
            error = null,
            message = null,
            emailError = if (mode == BackupDeliveryMode.EMAIL) _uiState.value.emailError else null,
        )
    }

    fun setEmail(value: String) {
        _uiState.value = _uiState.value.copy(
            email = value,
            emailError = validateEmail(value),
            error = null,
            message = null,
        )
    }

    private fun validateEmail(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        return if (Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) null
        else "Enter a valid email address."
    }

    /** Produces encrypted backup bytes and stages them for the selected delivery mode. */
    fun prepareExport() {
        val state = _uiState.value
        if (state.deliveryMode == BackupDeliveryMode.EMAIL) {
            val email = state.email.trim()
            val emailError = if (email.isEmpty()) "Enter an email address." else validateEmail(email)
            if (emailError != null) {
                _uiState.value = state.copy(emailError = emailError)
                return
            }
            if (!session.isUnlocked.value) {
                _uiState.value = state.copy(error = "Unlock the vault before generating a backup.")
                return
            }
        }

        _uiState.value = _uiState.value.copy(isBusy = true, error = null, message = null)
        viewModelScope.launch {
            try {
                val bytes = withContext(Dispatchers.Default) { exportVault() }
                val current = _uiState.value
                _uiState.value = if (current.deliveryMode == BackupDeliveryMode.EMAIL) {
                    current.copy(isBusy = false, stagedEmailExport = bytes)
                } else {
                    current.copy(isBusy = false, stagedExport = bytes)
                }
            } catch (e: Exception) {
                SecureLogger.e("export failed", e)
                _uiState.value = _uiState.value.copy(
                    isBusy = false,
                    error = "Export failed. Unlock the vault and try again.",
                )
            }
        }
    }

    fun consumeStagedExport(): ByteArray? {
        val bytes = _uiState.value.stagedExport
        _uiState.value = _uiState.value.copy(stagedExport = null)
        return bytes
    }

    fun consumeStagedEmailExport(): ByteArray? {
        val bytes = _uiState.value.stagedEmailExport
        _uiState.value = _uiState.value.copy(stagedEmailExport = null)
        return bytes
    }

    fun onEmailHandoff(success: Boolean) {
        _uiState.value = _uiState.value.copy(
            message = if (success) "Backup generated successfully." else null,
            error = if (success) null else "Could not prepare the backup email.",
        )
    }

    fun onEmailError(message: String) {
        _uiState.value = _uiState.value.copy(
            isBusy = false,
            error = "Email backup failed: $message",
        )
    }

    fun onExportWritten(success: Boolean) {
        _uiState.value = _uiState.value.copy(
            message = if (success) "Backup generated successfully." else null,
            error = if (success) null else "Could not write the backup file.",
        )
    }

    fun stageImport(bytes: ByteArray) {
        _uiState.value = _uiState.value.copy(
            stagedImport = bytes,
            error = null,
            message = null,
        )
    }

    /** Restores the staged import with [password]. Clears the password on completion. */
    fun restore(password: String) {
        val bytes = _uiState.value.stagedImport ?: run {
            _uiState.value = _uiState.value.copy(error = "Choose a backup file first.")
            return
        }
        _uiState.value = _uiState.value.copy(isBusy = true, error = null, message = null)
        viewModelScope.launch {
            val chars = password.toCharArray()
            try {
                val count = withContext(Dispatchers.Default) { importVault(bytes, chars) }
                _uiState.value = _uiState.value.copy(
                    isBusy = false,
                    stagedImport = null,
                    message = "Restored $count ${if (count == 1) "entry" else "entries"}.",
                )
            } catch (e: Exception) {
                SecureLogger.e("import failed", e)
                val message = e.message ?: e::class.simpleName ?: "unknown error"
                val friendly = if (message.contains("BAD_DECRYPT", ignoreCase = true) ||
                    message.contains("Tag mismatch", ignoreCase = true)
                ) {
                    "Restore failed: wrong master password or corrupted/tampered backup."
                } else {
                    "Restore failed: $message"
                }
                _uiState.value = _uiState.value.copy(
                    isBusy = false,
                    error = friendly,
                )
            } finally {
                chars.fill('\u0000')
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(error = null, message = null)
    }
}
