package com.example.passwordvault.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.fragment.app.FragmentActivity
import com.example.passwordvault.data.repository.AppSettings
import com.example.passwordvault.data.repository.AuthAttemptStore
import com.example.passwordvault.data.repository.SettingsStore
import com.example.passwordvault.data.repository.ThemeMode
import com.example.passwordvault.data.repository.VaultMetadataStore
import com.example.passwordvault.data.repository.VaultRepository
import com.example.passwordvault.domain.usecase.ChangeMasterPasswordUseCase
import com.example.passwordvault.domain.usecase.LockVaultUseCase
import com.example.passwordvault.security.biometric.BiometricManager
import com.example.passwordvault.security.password.PasswordGenerator
import com.example.passwordvault.security.password.PasswordOptions
import com.example.passwordvault.ui.biometric.BiometricAuthenticator
import com.example.passwordvault.util.SecureLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    private val biometricManager: BiometricManager,
    private val changeMasterPassword: ChangeMasterPasswordUseCase,
    private val metadataStore: VaultMetadataStore,
    private val repository: VaultRepository,
    private val lockVault: LockVaultUseCase,
    private val attemptStore: AuthAttemptStore,
    private val passwordGenerator: PasswordGenerator,
) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        settingsStore.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    fun setAutoLockEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setAutoLockEnabled(enabled) }
    }

    fun setClipboardTimeoutSeconds(seconds: Int) {
        viewModelScope.launch { settingsStore.setClipboardTimeoutSeconds(seconds) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsStore.setThemeMode(mode) }
    }

    /** Strong 20-character password for "generate" buttons. */
    fun generatePassword(): String = passwordGenerator.generate(PasswordOptions())

    fun setBiometricEnabled(enabled: Boolean, activity: FragmentActivity? = null, onResult: (Boolean) -> Unit = {}) {        if (enabled) {
            enableBiometrics(activity, onResult)
        } else {
            viewModelScope.launch {
                biometricManager.disable()
                settingsStore.setBiometricEnabled(false)
                onResult(true)
            }
        }
    }

    private fun enableBiometrics(activity: FragmentActivity?, onResult: (Boolean) -> Unit) {
        if (activity == null) {
            onResult(false)
            return
        }
        val authenticator = BiometricAuthenticator(activity)
        if (!authenticator.canAuthenticate()) {
            onResult(false)
            return
        }
        viewModelScope.launch {
            val cipher = try {
                biometricManager.prepareEnable()
            } catch (e: Exception) {
                SecureLogger.e("prepare enable biometrics failed", e)
                onResult(false)
                return@launch
            }
            authenticator.authenticate(
                title = "Enable biometric unlock",
                cipher = cipher,
                onSuccess = { c ->
                    viewModelScope.launch {
                        val ok = try {
                            withContext(Dispatchers.Default) { biometricManager.completeEnable(c) }
                            settingsStore.setBiometricEnabled(true)
                            true
                        } catch (e: Exception) {
                            SecureLogger.e("complete enable biometrics failed", e)
                            false
                        }
                        onResult(ok)
                    }
                },
                onError = { onResult(false) },
            )
        }
    }

    /**
     * Changes the master password. Calls [onResult] with true on success.
     * Failures (including a wrong current password) count toward the
     * brute-force counter, like unlock attempts.
     */
    fun changePassword(current: String, new: String, confirm: String, onResult: (Boolean) -> Unit) {
        if (new != confirm) {
            _actionError.value = "New passwords do not match"
            onResult(false)
            return
        }
        if (new.length < 8) {
            _actionError.value = "New password must be at least 8 characters"
            onResult(false)
            return
        }
        if (new == current) {
            _actionError.value = "New password must differ from the current one"
            onResult(false)
            return
        }
        _actionError.value = null
        viewModelScope.launch {
            val currentChars = current.toCharArray()
            val newChars = new.toCharArray()
            try {
                withContext(Dispatchers.Default) {
                    changeMasterPassword(currentChars, newChars)
                }
                attemptStore.recordSuccess()
                onResult(true)
            } catch (e: Exception) {
                SecureLogger.e("change password failed", e)
                attemptStore.recordFailure()
                _actionError.value = "Could not change the password. The current password may be incorrect."
                onResult(false)
            } finally {
                currentChars.fill('\u0000')
                newChars.fill('\u0000')
            }
        }
    }

    /**
     * Permanently deletes the vault: biometric enrollment, all entries,
     * metadata, and the session key. The app returns to the Welcome screen.
     */
    fun deleteVault(onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                try {
                    biometricManager.disable()
                } catch (e: Exception) {
                    SecureLogger.e("disable biometrics during vault delete failed", e)
                }
                repository.deleteAll()
                metadataStore.clear()
                settingsStore.setBiometricEnabled(false)
                attemptStore.recordSuccess()
            } catch (e: Exception) {
                SecureLogger.e("delete vault failed", e)
                _actionError.value = "Could not delete the vault. Try again."
                return@launch
            } finally {
                lockVault()
            }
            onDone()
        }
    }

    fun clearActionError() {
        _actionError.value = null
    }
}
