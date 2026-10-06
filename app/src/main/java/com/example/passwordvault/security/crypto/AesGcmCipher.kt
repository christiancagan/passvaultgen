package com.example.passwordvault.security.crypto

import com.example.passwordvault.security.random.SecureRandomProvider
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Result of an authenticated encryption operation.
 *
 * @param nonce      12-byte GCM nonce (must be unique per key).
 * @param ciphertext ciphertext + 128-bit authentication tag appended by GCM.
 */
data class EncryptedData(
    val nonce: ByteArray,
    val ciphertext: ByteArray,
)

/**
 * AES-256-GCM authenticated encryption using the platform [Cipher].
 *
 * We never implement AES or GCM manually. The platform provider is
 * hardware-accelerated and audited. A fresh 12-byte nonce is generated for
 * every encryption, which is the hard invariant that makes GCM safe.
 */
class AesGcmCipher(
    private val random: SecureRandomProvider,
) {

    private val nonceLength = 12
    private val tagLengthBits = 128
    private val transformation = "AES/GCM/NoPadding"

    /**
     * Encrypts [plaintext] under [key] (256-bit). [aad] is optional associated
     * data that is authenticated but not encrypted.
     */
    fun encrypt(key: ByteArray, plaintext: ByteArray, aad: ByteArray? = null): EncryptedData {
        require(key.size == 32) { "AES-256 requires a 32-byte key" }
        val nonce = random.nextBytes(nonceLength)
        val cipher = Cipher.getInstance(transformation)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(key, "AES"),
            GCMParameterSpec(tagLengthBits, nonce),
        )
        aad?.let { cipher.updateAAD(it) }
        val ciphertext = cipher.doFinal(plaintext)
        return EncryptedData(nonce, ciphertext)
    }

    /**
     * Decrypts [ciphertext] under [key]. Throws [javax.crypto.AEADBadTagException]
     * (or a general [Exception]) if the ciphertext or AAD was modified.
     */
    fun decrypt(key: ByteArray, nonce: ByteArray, ciphertext: ByteArray, aad: ByteArray? = null): ByteArray {
        require(key.size == 32) { "AES-256 requires a 32-byte key" }
        val cipher = Cipher.getInstance(transformation)
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(key, "AES"),
            GCMParameterSpec(tagLengthBits, nonce),
        )
        aad?.let { cipher.updateAAD(it) }
        return cipher.doFinal(ciphertext)
    }
}
