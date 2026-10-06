package com.example.passwordvault.security.backup

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import org.junit.Test

class VaultBackupCodecTest {

    private val codec = VaultBackupCodec(Gson())

    private fun sampleBackup() = VaultBackup(
        version = 1,
        kdf = "argon2id",
        kdfParams = BackupKdfParams(memoryKib = 65536, iterations = 3, parallelism = 1),
        salt = "c2FsdA==",
        wrappedDek = "d3JhcHBlZA==",
        dekNonce = "bm9uY2U=",
        dataNonce = "ZGF0YW5vbmNl",
        data = "ZGF0YQ==",
    )

    @Test
    fun `encode then decode returns original backup`() {
        val backup = sampleBackup()
        assertThat(codec.decodeBackup(codec.encodeBackup(backup))).isEqualTo(backup)
    }

    @Test
    fun `entries round-trip preserves all fields`() {
        val entries = listOf(
            BackupEntry("Email", "Personal", "n", "u", "p", "https://x", "notes", true, 1L, 2L),
        )
        assertThat(codec.decodeEntries(codec.encodeEntries(entries))).isEqualTo(entries)
    }

    @Test
    fun `unsupported version is rejected`() {
        val bad = codec.encodeBackup(sampleBackup().copy(version = 99))
        try {
            codec.decodeBackup(bad)
            throw AssertionError("expected validation to fail")
        } catch (expected: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun `unknown kdf is rejected`() {
        val bad = codec.encodeBackup(sampleBackup().copy(kdf = "scrypt"))
        try {
            codec.decodeBackup(bad)
            throw AssertionError("expected validation to fail")
        } catch (expected: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun `malformed json is rejected`() {
        try {
            codec.decodeBackup("{not json".toByteArray(Charsets.UTF_8))
            throw AssertionError("expected validation to fail")
        } catch (expected: IllegalArgumentException) {
            // expected
        }
    }
}
