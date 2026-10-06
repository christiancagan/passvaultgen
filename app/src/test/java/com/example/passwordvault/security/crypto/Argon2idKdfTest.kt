package com.example.passwordvault.security.crypto

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class Argon2idKdfTest {

    private val kdf = Argon2idKdf()

    // Small parameters to keep unit tests fast.
    private val params = Argon2Params(memoryKib = 1024, iterations = 1, parallelism = 1)

    @Test
    fun `derive returns requested output length`() {
        val salt = ByteArray(16) { it.toByte() }
        val key = kdf.derive("password".toCharArray(), salt, params)
        assertThat(key.size).isEqualTo(32)
    }

    @Test
    fun `same inputs produce same key`() {
        val salt = ByteArray(16) { it.toByte() }
        val a = kdf.derive("password".toCharArray(), salt, params)
        val b = kdf.derive("password".toCharArray(), salt, params)
        assertThat(a).isEqualTo(b)
    }

    @Test
    fun `different salt produces different key`() {
        val saltA = ByteArray(16) { it.toByte() }
        val saltB = ByteArray(16) { (it + 1).toByte() }
        val a = kdf.derive("password".toCharArray(), saltA, params)
        val b = kdf.derive("password".toCharArray(), saltB, params)
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `different password produces different key`() {
        val salt = ByteArray(16) { it.toByte() }
        val a = kdf.derive("password".toCharArray(), salt, params)
        val b = kdf.derive("password2".toCharArray(), salt, params)
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `params validation rejects invalid values`() {
        try {
            Argon2Params(memoryKib = 0, iterations = 1, parallelism = 1)
            throw AssertionError("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // expected
        }
    }
}
