package com.example.passwordvault.ui.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passwordvault.domain.usecase.ExportVaultUseCase
import com.example.passwordvault.domain.usecase.ImportVaultUseCase
import com.example.passwordvault.util.SecureLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class BackupUiState(
    val isBusy: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    /** Bytes staged for the SAF create-document flow. Consumed (cleared) after writing. */
    val stagedExport: ByteArray? = null,
    /** Bytes read from the user-picked import file, awaiting the master password. */
    val stagedImport: ByteArray? = null,
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val exportVault: ExportVaultUseCase,
    private val importVault: ImportVaultUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    /** Produces encrypted backup bytes and stages them for the SAF save dialog. */
    fun prepareExport() {
        _uiState.value = _uiState.value.copy(isBusy = true, error = null, message = null)
        viewModelScope.launch {
            try {
                val bytes = withContext(Dispatchers.Default) { exportVault() }
                _uiState.value = _uiState.value.copy(isBusy = false, stagedExport = bytes)
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

    fun onExportWritten(success: Boolean) {
        _uiState.value = _uiState.value.copy(
            message = if (success) "Backup saved." else null,
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
                _uiState.value = _uiState.value.copy(
                    isBusy = false,
                    error = "Restore failed. The password may be incorrect or the file may be corrupted.",
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
