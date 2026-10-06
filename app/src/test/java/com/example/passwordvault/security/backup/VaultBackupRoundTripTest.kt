package com.example.passwordvault.security.backup

import com.example.passwordvault.security.crypto.AesGcmCipher
import com.example.passwordvault.security.crypto.Argon2Params
import com.example.passwordvault.security.crypto.Argon2idKdf
import com.example.passwordvault.security.crypto.VaultMetadata
import com.example.passwordvault.security.random.SecureRandomProviderImpl
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import org.junit.Test

class VaultBackupRoundTripTest {

    private val b64 = object : BackupBase64 {
        private val delegate = java.util.Base64.getEncoder()
        private val decoder = java.util.Base64.getDecoder()
        override fun encode(bytes: ByteArray): String = delegate.encodeToString(bytes)
        override fun decode(s: String): ByteArray = decoder.decode(s)
    }

    private val random = SecureRandomProviderImpl()
    private val cipher = AesGcmCipher(random)
    private val codec = VaultBackupCodec(Gson())
    private val exporter = VaultExporter(cipher, codec, b64)
    private val importer = VaultImporter(Argon2idKdf(), cipher, codec, b64)

    // Tiny KDF params keep the test fast; the crypto path is identical.
    private val params = Argon2Params(memoryKib = 8, iterations = 1, parallelism = 1)

    @Test
    fun `export then import restores entries and metadata`() {
        val password = "correct horse battery staple".toCharArray()
        val salt = random.nextBytes(16)
        val dek = random.nextBytes(32)
        val kek = Argon2idKdf().derive(password, salt, params)
        val wrapped = try {
            cipher.encrypt(kek, dek)
        } finally {
            kek.fill(0)
        }
        val metadata = VaultMetadata(
            version = 1,
            kdfParams = params,
            salt = salt,
            wrappedDek = wrapped.ciphertext,
            dekNonce = wrapped.nonce,
        )
        val entries = listOf(
            BackupEntry("Email", "Personal", "n", "u@d", "s3cret", "https://x", "", false, 1L, 2L),
            BackupEntry("Bank", "Finance", "n2", "u2", "p2", "", "note", true, 3L, 4L),
        )

        val bytes = exporter.export(dek, entries, metadata)
        // The backup must be self-contained: parseable without the original objects.
        val backup = codec.decodeBackup(bytes)
        assertThat(backup.version).isEqualTo(1)

        val result = importer.import(bytes, password.copyOf())
        try {
            assertThat(result.entries).isEqualTo(entries)
            assertThat(result.metadata.salt).isEqualTo(salt)
            assertThat(result.metadata.wrappedDek).isEqualTo(wrapped.ciphertext)
            assertThat(result.metadata.kdfParams).isEqualTo(params)
            // The restored DEK must actually decrypt record data.
            assertThat(result.dek).isEqualTo(dek)
        } finally {
            result.dek.fill(0)
            password.fill('\u0000')
            dek.fill(0)
        }
    }

    @Test
    fun `import with wrong password fails`() {
        val password = "correct horse battery staple".toCharArray()
        val salt = random.nextBytes(16)
        val dek = random.nextBytes(32)
        val kek = Argon2idKdf().derive(password, salt, params)
        val wrapped = try {
            cipher.encrypt(kek, dek)
        } finally {
            kek.fill(0)
        }
        val metadata = VaultMetadata(1, params, salt, wrapped.ciphertext, wrapped.nonce)
        val bytes = exporter.export(dek, emptyList(), metadata)

        try {
            importer.import(bytes, "wrong password".toCharArray())
            throw AssertionError("expected import to fail")
        } catch (expected: Exception) {
            // expected: GCM auth failure on the wrapped DEK
        } finally {
            password.fill('\u0000')
            dek.fill(0)
        }
    }

    @Test
    fun `tampered backup payload fails authentication`() {
        val password = "correct horse battery staple".toCharArray()
        val salt = random.nextBytes(16)
        val dek = random.nextBytes(32)
        val kek = Argon2idKdf().derive(password, salt, params)
        val wrapped = try {
            cipher.encrypt(kek, dek)
        } finally {
            kek.fill(0)
        }
        val metadata = VaultMetadata(1, params, salt, wrapped.ciphertext, wrapped.nonce)
        val entries = listOf(
            BackupEntry("T", "C", "n", "u", "p", "", "", false, 1L, 1L),
        )
        val bytes = exporter.export(dek, entries, metadata)

        val backup = codec.decodeBackup(bytes)
        val data = b64.decode(backup.data)
        data[0] = (data[0] + 1).toByte()
        val tampered = codec.encodeBackup(backup.copy(data = b64.encode(data)))

        try {
            importer.import(tampered, password.copyOf())
            throw AssertionError("expected import to fail")
        } catch (expected: Exception) {
            // expected: GCM auth failure on the payload
        } finally {
            password.fill('\u0000')
            dek.fill(0)
        }
    }
}
