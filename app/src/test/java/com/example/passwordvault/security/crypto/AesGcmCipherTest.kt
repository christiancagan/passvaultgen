package com.example.passwordvault.security.crypto

import com.example.passwordvault.security.random.SecureRandomProviderImpl
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AesGcmCipherTest {

    private val random = SecureRandomProviderImpl()
    private val cipher = AesGcmCipher(random)

    private val key = ByteArray(32) { it.toByte() }

    @Test
    fun `encrypt then decrypt returns original plaintext`() {
        val plaintext = "secret password".toByteArray(Charsets.UTF_8)
        val encrypted = cipher.encrypt(key, plaintext)
        val decrypted = cipher.decrypt(key, encrypted.nonce, encrypted.ciphertext)
        assertThat(decrypted).isEqualTo(plaintext)
    }

    @Test
    fun `decrypt with wrong key fails`() {
        val plaintext = "secret".toByteArray(Charsets.UTF_8)
        val encrypted = cipher.encrypt(key, plaintext)
        val wrongKey = ByteArray(32) { (it + 1).toByte() }
        try {
            cipher.decrypt(wrongKey, encrypted.nonce, encrypted.ciphertext)
            throw AssertionError("expected decryption to fail")
        } catch (expected: Exception) {
            // expected
        }
    }

    @Test
    fun `tampered ciphertext fails authentication`() {
        val plaintext = "secret".toByteArray(Charsets.UTF_8)
        val encrypted = cipher.encrypt(key, plaintext)
        val tampered = encrypted.ciphertext.copyOf()
        tampered[0] = (tampered[0] + 1).toByte()
        try {
            cipher.decrypt(key, encrypted.nonce, tampered)
            throw AssertionError("expected decryption to fail")
        } catch (expected: Exception) {
            // expected
        }
    }

    @Test
    fun `nonces are unique across encryptions`() {
        val plaintext = "secret".toByteArray(Charsets.UTF_8)
        val a = cipher.encrypt(key, plaintext)
        val b = cipher.encrypt(key, plaintext)
        assertThat(a.nonce).isNotEqualTo(b.nonce)
    }

    @Test
    fun `aad is authenticated`() {
        val plaintext = "secret".toByteArray(Charsets.UTF_8)
        val aad = "row-id".toByteArray(Charsets.UTF_8)
        val encrypted = cipher.encrypt(key, plaintext, aad)
        // Correct AAD decrypts.
        assertThat(cipher.decrypt(key, encrypted.nonce, encrypted.ciphertext, aad)).isEqualTo(plaintext)
        // Wrong AAD fails.
        try {
            cipher.decrypt(key, encrypted.nonce, encrypted.ciphertext, "other".toByteArray(Charsets.UTF_8))
            throw AssertionError("expected decryption to fail")
        } catch (expected: Exception) {
            // expected
        }
    }
}
