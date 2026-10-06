package com.example.passwordvault.security.crypto

import com.example.passwordvault.security.random.SecureRandomProvider

/**
 * Vault metadata persisted to disk. Contains NO plaintext secrets: the DEK is
 * stored only in wrapped (encrypted) form, and the salt is not secret.
 *
 * @param version    vault format version (for future migrations).
 * @param kdfParams  Argon2id parameters used to derive the KEK.
 * @param salt       unique per-vault KDF salt.
 * @param wrappedDek DEK encrypted under the KEK (AES-GCM ciphertext + tag).
 * @param dekNonce   nonce used to wrap the DEK.
 */
data class VaultMetadata(
    val version: Int,
    val kdfParams: Argon2Params,
    val salt: ByteArray,
    val wrappedDek: ByteArray,
    val dekNonce: ByteArray,
    val biometricWrappedDek: ByteArray? = null,
    val biometricDekNonce: ByteArray? = null,
)

/**
 * Envelope encryption orchestration.
 *
 * The master password never encrypts records directly. Instead:
 *   1. A random 256-bit DEK is generated.
 *   2. A KEK is derived from the master password via Argon2id.
 *   3. The KEK wraps (encrypts) the DEK.
 *   4. Records are encrypted with the DEK.
 *
 * This lets us change the master password by re-wrapping the DEK (no record
 * re-encryption) and lets biometrics wrap the same DEK via a Keystore key.
 */
class VaultCryptoService(
    private val kdf: Argon2idKdf,
    private val cipher: AesGcmCipher,
    private val random: SecureRandomProvider,
) {

    private val saltLength = 16
    private val dekLength = 32

    /**
     * Creates a new vault: generates a DEK and salt, derives a KEK from
     * [masterPassword], and wraps the DEK. Returns metadata safe to persist.
     */
    fun createVault(masterPassword: CharArray, params: Argon2Params): VaultMetadata {
        val salt = random.nextBytes(saltLength)
        val dek = random.nextBytes(dekLength)
        val kek = kdf.derive(masterPassword, salt, params)
        val wrapped = try {
            cipher.encrypt(kek, dek)
        } finally {
            kek.fill(0)
        }
        dek.fill(0)
        return VaultMetadata(
            version = 1,
            kdfParams = params,
            salt = salt,
            wrappedDek = wrapped.ciphertext,
            dekNonce = wrapped.nonce,
        )
    }

    /**
     * Unlocks the vault: derives the KEK from [masterPassword] and unwraps the
     * DEK. Returns the plaintext DEK (caller must hold it only in memory and
     * zero it on lock). Throws on an incorrect password or tampered metadata.
     */
    fun unlock(masterPassword: CharArray, metadata: VaultMetadata): ByteArray {
        val kek = kdf.derive(masterPassword, metadata.salt, metadata.kdfParams)
        return try {
            cipher.decrypt(kek, metadata.dekNonce, metadata.wrappedDek)
        } finally {
            kek.fill(0)
        }
    }

    /**
     * Re-wraps the DEK under a new master password (password change). The DEK
     * itself is unchanged, so records do not need re-encryption.
     */
    fun rewrapDek(dek: ByteArray, newMasterPassword: CharArray, params: Argon2Params): VaultMetadata {
        val salt = random.nextBytes(saltLength)
        val kek = kdf.derive(newMasterPassword, salt, params)
        val wrapped = try {
            cipher.encrypt(kek, dek)
        } finally {
            kek.fill(0)
        }
        return VaultMetadata(
            version = 1,
            kdfParams = params,
            salt = salt,
            wrappedDek = wrapped.ciphertext,
            dekNonce = wrapped.nonce,
        )
    }
}
