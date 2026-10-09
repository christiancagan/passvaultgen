package com.example.passwordvault.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

data class AppSettings(
    val autoLockEnabled: Boolean = true,
    val clipboardTimeoutSeconds: Int = 30,
    val biometricEnabled: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val useMaterialYou: Boolean = false,
)

/**
 * User preferences. Contains NO secrets — only non-sensitive configuration.
 */
class SettingsStore(private val context: Context) {

    private object Keys {
        val autoLockEnabled = booleanPreferencesKey("auto_lock_enabled")
        val clipboardTimeoutSeconds = intPreferencesKey("clipboard_timeout_seconds")
        val biometricEnabled = booleanPreferencesKey("biometric_enabled")
        val themeMode = stringPreferencesKey("theme_mode")
        val useMaterialYou = booleanPreferencesKey("use_material_you")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs ->
            val themeMode = prefs[Keys.themeMode]?.let {
                runCatching { ThemeMode.valueOf(it) }.getOrDefault(ThemeMode.DARK)
            } ?: ThemeMode.DARK
            AppSettings(
                autoLockEnabled = prefs[Keys.autoLockEnabled] ?: true,
                clipboardTimeoutSeconds = prefs[Keys.clipboardTimeoutSeconds] ?: 30,
                biometricEnabled = prefs[Keys.biometricEnabled] ?: false,
                themeMode = themeMode,
                // Unset follows the legacy behavior exactly: dynamic color was
                // only used in SYSTEM mode. Once the user touches the toggle,
                // their explicit choice wins for every theme mode.
                useMaterialYou = prefs[Keys.useMaterialYou] ?: (themeMode == ThemeMode.SYSTEM),
            )
        }

    suspend fun setAutoLockEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.autoLockEnabled] = enabled }
    }

    suspend fun setClipboardTimeoutSeconds(seconds: Int) {
        context.settingsDataStore.edit { it[Keys.clipboardTimeoutSeconds] = seconds }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.biometricEnabled] = enabled }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.themeMode] = mode.name }
    }

    suspend fun setUseMaterialYou(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.useMaterialYou] = enabled }
    }
}
