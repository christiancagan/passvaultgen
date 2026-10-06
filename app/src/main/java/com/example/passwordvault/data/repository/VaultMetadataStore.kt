package com.example.passwordvault.data.repository

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.passwordvault.security.crypto.Argon2Params
import com.example.passwordvault.security.crypto.VaultMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.vaultMetaDataStore by preferencesDataStore(name = "vault_meta")

/**
 * Persists [VaultMetadata] (salt, KDF params, wrapped DEK) to DataStore.
 *
 * The metadata contains NO plaintext secrets: the DEK is stored only in wrapped
 * form and the salt is not secret. The master password is never stored here.
 */
class VaultMetadataStore(private val context: Context) {

    private object Keys {
        val version = intPreferencesKey("version")
        val memoryKib = intPreferencesKey("memory_kib")
        val iterations = intPreferencesKey("iterations")
        val parallelism = intPreferencesKey("parallelism")
        val salt = stringPreferencesKey("salt")
        val wrappedDek = stringPreferencesKey("wrapped_dek")
        val dekNonce = stringPreferencesKey("dek_nonce")
        val biometricWrappedDek = stringPreferencesKey("biometric_wrapped_dek")
        val biometricDekNonce = stringPreferencesKey("biometric_dek_nonce")
    }

    val metadata: Flow<VaultMetadata?> = context.vaultMetaDataStore.data.map { prefs ->
        read(prefs)
    }

    suspend fun hasVault(): Boolean = context.vaultMetaDataStore.data.first().contains(Keys.version)

    suspend fun save(metadata: VaultMetadata) {
        context.vaultMetaDataStore.edit { prefs ->
            prefs[Keys.version] = metadata.version
            prefs[Keys.memoryKib] = metadata.kdfParams.memoryKib
            prefs[Keys.iterations] = metadata.kdfParams.iterations
            prefs[Keys.parallelism] = metadata.kdfParams.parallelism
            prefs[Keys.salt] = encode(metadata.salt)
            prefs[Keys.wrappedDek] = encode(metadata.wrappedDek)
            prefs[Keys.dekNonce] = encode(metadata.dekNonce)
            metadata.biometricWrappedDek?.let { prefs[Keys.biometricWrappedDek] = encode(it) }
                ?: prefs.remove(Keys.biometricWrappedDek)
            metadata.biometricDekNonce?.let { prefs[Keys.biometricDekNonce] = encode(it) }
                ?: prefs.remove(Keys.biometricDekNonce)
        }
    }

    suspend fun clear() {
        context.vaultMetaDataStore.edit { it.clear() }
    }

    private fun read(prefs: Preferences): VaultMetadata? {
        val version = prefs[Keys.version] ?: return null
        val memoryKib = prefs[Keys.memoryKib] ?: return null
        val iterations = prefs[Keys.iterations] ?: return null
        val parallelism = prefs[Keys.parallelism] ?: return null
        val salt = prefs[Keys.salt]?.let(::decode) ?: return null
        val wrappedDek = prefs[Keys.wrappedDek]?.let(::decode) ?: return null
        val dekNonce = prefs[Keys.dekNonce]?.let(::decode) ?: return null

        return VaultMetadata(
            version = version,
            kdfParams = Argon2Params(memoryKib, iterations, parallelism),
            salt = salt,
            wrappedDek = wrappedDek,
            dekNonce = dekNonce,
            biometricWrappedDek = prefs[Keys.biometricWrappedDek]?.let(::decode),
            biometricDekNonce = prefs[Keys.biometricDekNonce]?.let(::decode),
        )
    }

    private fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun decode(s: String): ByteArray = Base64.decode(s, Base64.NO_WRAP)
}
