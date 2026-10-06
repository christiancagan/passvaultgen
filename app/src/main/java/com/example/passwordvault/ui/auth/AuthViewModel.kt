package com.example.passwordvault.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passwordvault.data.repository.AuthAttemptStore
import com.example.passwordvault.data.repository.VaultMetadataStore
import com.example.passwordvault.domain.usecase.CreateVaultUseCase
import com.example.passwordvault.domain.usecase.LockVaultUseCase
import com.example.passwordvault.domain.usecase.UnlockVaultUseCase
import com.example.passwordvault.security.biometric.BiometricManager
import com.example.passwordvault.security.crypto.VaultSession
import com.example.passwordvault.ui.biometric.BiometricAuthenticator
import com.example.passwordvault.util.SecureLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AuthUiState(
    val hasVault: Boolean? = null,
    val isUnlocked: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val metadataStore: VaultMetadataStore,
    private val session: VaultSession,
    private val createVault: CreateVaultUseCase,
    private val unlockVault: UnlockVaultUseCase,
    private val lockVault: LockVaultUseCase,
    private val biometricManager: BiometricManager,
    private val attemptStore: AuthAttemptStore,
) : ViewModel() {

    val hasVault: StateFlow<Boolean?> = metadataStore.metadata
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isUnlocked: StateFlow<Boolean> = session.isUnlocked

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun createVault(password: String, confirm: String) {
        if (password != confirm) {
            _uiState.value = _uiState.value.copy(error = "Passwords do not match")
            return
        }
        if (password.length < 8) {
            _uiState.value = _uiState.value.copy(error = "Master password must be at least 8 characters")
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val chars = password.toCharArray()
            try {
                withContext(Dispatchers.Default) { createVault(chars) }
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                SecureLogger.e("createVault failed", e)
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Unable to create the vault.")
            } finally {
                chars.fill('\u0000')
            }
        }
    }

    fun unlock(password: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val blockedMs = attemptStore.lockoutRemainingMs()
            if (blockedMs > 0) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Too many failed attempts. Try again in ${blockedMs / 1000}s.",
                )
                return@launch
            }
            val chars = password.toCharArray()
            try {
                withContext(Dispatchers.Default) { unlockVault(chars) }
                attemptStore.recordSuccess()
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                SecureLogger.e("unlock failed", e)
                val lockoutMs = attemptStore.recordFailure()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = if (lockoutMs > 0) {
                        "Too many failed attempts. Try again in ${lockoutMs / 1000}s."
                    } else {
                        "Unable to unlock the vault. The password may be incorrect or the vault may be corrupted."
                    },
                )
            } finally {
                chars.fill('\u0000')
            }
        }
    }

    fun lock() = lockVault()

    fun unlockWithBiometrics(activity: FragmentActivity, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val cipher = try {
                biometricManager.prepareUnlock()
            } catch (e: Exception) {
                SecureLogger.e("prepare biometric unlock failed", e)
                onResult(false)
                return@launch
            }
            val authenticator = BiometricAuthenticator(activity)
            authenticator.authenticate(
                title = "Unlock vault",
                cipher = cipher,
                onSuccess = { c ->
                    viewModelScope.launch {
                        val ok = try {
                            withContext(Dispatchers.Default) { biometricManager.completeUnlock(c) }
                            attemptStore.recordSuccess()
                            true
                        } catch (e: Exception) {
                            SecureLogger.e("complete biometric unlock failed", e)
                            false
                        }
                        onResult(ok)
                    }
                },
                onError = { onResult(false) },
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
