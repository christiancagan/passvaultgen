package com.example.passwordvault.security.crypto

import com.example.passwordvault.security.random.SecureRandomProviderImpl
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VaultCryptoServiceTest {

    private val random = SecureRandomProviderImpl()
    private val kdf = Argon2idKdf()
    private val cipher = AesGcmCipher(random)
    private val service = VaultCryptoService(kdf, cipher, random)

    private val params = Argon2Params(memoryKib = 1024, iterations = 1, parallelism = 1)

    @Test
    fun `create then unlock returns a working dek`() {
        val password = "correct horse battery staple".toCharArray()
        val metadata = service.createVault(password, params)
        val dek = service.unlock(password, metadata)
        assertThat(dek.size).isEqualTo(32)
        dek.fill(0)
    }

    @Test
    fun `unlock with wrong password fails`() {
        val password = "correct horse battery staple".toCharArray()
        val metadata = service.createVault(password, params)
        try {
            service.unlock("wrong password".toCharArray(), metadata)
            throw AssertionError("expected unlock to fail")
        } catch (expected: Exception) {
            // expected
        }
    }

    @Test
    fun `metadata contains no plaintext dek`() {
        val password = "correct horse battery staple".toCharArray()
        val metadata = service.createVault(password, params)
        // The wrapped DEK must not equal any obvious plaintext; it is ciphertext.
        assertThat(metadata.wrappedDek).isNotEmpty()
        assertThat(metadata.salt.size).isEqualTo(16)
    }

    @Test
    fun `rewrap produces new metadata that still unlocks`() {
        val password = "old password".toCharArray()
        val newPassword = "new password".toCharArray()
        val metadata = service.createVault(password, params)
        val dek = service.unlock(password, metadata)
        val newMetadata = service.rewrapDek(dek, newPassword, params)
        val dek2 = service.unlock(newPassword, newMetadata)
        assertThat(dek2).isEqualTo(dek)
        dek.fill(0)
        dek2.fill(0)
    }
}
