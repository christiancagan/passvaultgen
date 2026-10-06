package com.example.passwordvault.security.biometric

import com.example.passwordvault.security.keystore.SecureKeyManager
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

/**
 * Result of wrapping the DEK with the biometric-bound Keystore key.
 */
data class BiometricWrappedDek(
    val nonce: ByteArray,
    val ciphertext: ByteArray,
)

/**
 * Low-level Keystore-backed cipher operations for the biometric unlock path.
 *
 * The biometric Keystore key is used to wrap/unwrap the DEK. The key is
 * configured with `setUserAuthenticationRequired(true)`, so the returned
 * [Cipher] is only usable after a successful BiometricPrompt authentication.
 */
class BiometricCrypto(
    private val keyManager: SecureKeyManager,
) {

    private val transformation = "AES/GCM/NoPadding"
    private val tagLengthBits = 128

    /**
     * Creates a Cipher initialized for ENCRYPT (used to wrap the DEK when
     * enabling biometric unlock). The IV is randomized by the Keystore.
     */
    fun createEncryptCipher(): Cipher {
        val key = keyManager.getOrCreateBiometricKey(SecureKeyManager.BIOMETRIC_KEY_ALIAS)
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher
    }

    /**
     * Creates a Cipher initialized for DECRYPT with the stored [nonce] (used to
     * unwrap the DEK when unlocking with biometrics).
     */
    fun createDecryptCipher(nonce: ByteArray): Cipher {
        val key = keyManager.getOrCreateBiometricKey(SecureKeyManager.BIOMETRIC_KEY_ALIAS)
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(tagLengthBits, nonce))
        return cipher
    }

    /** Wraps [dek] using an authenticated [cipher]. Returns ciphertext + IV. */
    fun wrapDek(cipher: Cipher, dek: ByteArray): BiometricWrappedDek {
        val ciphertext = cipher.doFinal(dek)
        return BiometricWrappedDek(nonce = cipher.iv, ciphertext = ciphertext)
    }

    /** Unwraps [wrapped] using an authenticated [cipher]. Returns the DEK. */
    fun unwrapDek(cipher: Cipher, wrapped: ByteArray): ByteArray = cipher.doFinal(wrapped)
}
