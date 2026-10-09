package com.example.passwordvault.security.biometric

import com.example.passwordvault.data.repository.VaultMetadataStore
import com.example.passwordvault.security.crypto.VaultSession
import com.example.passwordvault.security.keystore.SecureKeyManager
import kotlinx.coroutines.flow.firstOrNull
import javax.crypto.Cipher

/**
 * Orchestrates the biometric unlock path.
 *
 * Biometrics never replace the master password; they unlock access to the DEK
 * via a Keystore key that wraps the DEK. The two-phase API (prepare/complete)
 * exists because the [Cipher] must be created, passed to BiometricPrompt, and
 * only used after the user authenticates.
 */
class BiometricManager(
    private val biometricCrypto: BiometricCrypto,
    private val metadataStore: VaultMetadataStore,
    private val session: VaultSession,
    private val keyManager: SecureKeyManager,
) {

    /** Returns true if biometric unlock is currently enabled. */
    suspend fun isEnabled(): Boolean =
        metadataStore.metadata.firstOrNull()?.biometricWrappedDek != null

    /** Phase 1 of enabling: create the encrypt cipher to pass to BiometricPrompt. */
    fun prepareEnable(): Cipher = biometricCrypto.createEncryptCipher()

    /** Phase 2 of enabling: wrap the DEK and persist it. */
    suspend fun completeEnable(cipher: Cipher) {
        // withDek hands wrapDek a zeroed-after-use copy of the live DEK;
        // the persistence below does not need key material, so it stays
        // outside the (non-suspend) withDek scope.
        val wrapped = session.withDek { dek -> biometricCrypto.wrapDek(cipher, dek) }
        val metadata = metadataStore.metadata.firstOrNull()
            ?: throw IllegalStateException("No vault exists")
        metadataStore.save(
            metadata.copy(
                biometricWrappedDek = wrapped.ciphertext,
                biometricDekNonce = wrapped.nonce,
            ),
        )
    }

    /** Phase 1 of unlocking: create the decrypt cipher to pass to BiometricPrompt. */
    suspend fun prepareUnlock(): Cipher {
        val metadata = metadataStore.metadata.firstOrNull()
            ?: throw IllegalStateException("No vault exists")
        val nonce = metadata.biometricDekNonce
            ?: throw IllegalStateException("Biometric unlock is not enabled")
        return biometricCrypto.createDecryptCipher(nonce)
    }

    /** Phase 2 of unlocking: unwrap the DEK and unlock the session. */
    suspend fun completeUnlock(cipher: Cipher) {
        val metadata = metadataStore.metadata.firstOrNull()
            ?: throw IllegalStateException("No vault exists")
        val wrapped = metadata.biometricWrappedDek
            ?: throw IllegalStateException("Biometric unlock is not enabled")
        val dek = biometricCrypto.unwrapDek(cipher, wrapped)
        try {
            session.unlock(dek)
        } finally {
            dek.fill(0)
        }
    }

    /** Disables biometric unlock: clears the wrapped DEK and deletes the key. */
    suspend fun disable() {
        val metadata = metadataStore.metadata.firstOrNull() ?: return
        metadataStore.save(
            metadata.copy(biometricWrappedDek = null, biometricDekNonce = null),
        )
        keyManager.delete(SecureKeyManager.BIOMETRIC_KEY_ALIAS)
    }
}
