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

    @Test
    fun `trivially cheap kdf is rejected on import`() {
        // A crafted 8 KiB Argon2 would make offline brute-force instant;
        // the import floor must reject it before any key derivation runs.
        val bad = codec.encodeBackup(
            sampleBackup().copy(kdfParams = BackupKdfParams(memoryKib = 8, iterations = 3, parallelism = 1)),
        )
        try {
            codec.decodeBackup(bad)
            throw AssertionError("expected validation to fail")
        } catch (expected: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun `oversized kdf memory is rejected on import`() {
        // A crafted 2 GiB request must not be able to OOM the device.
        val bad = codec.encodeBackup(
            sampleBackup().copy(
                kdfParams = BackupKdfParams(memoryKib = 2 * 1024 * 1024, iterations = 3, parallelism = 1),
            ),
        )
        try {
            codec.decodeBackup(bad)
            throw AssertionError("expected validation to fail")
        } catch (expected: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun `zero iterations is rejected on import`() {
        val bad = codec.encodeBackup(
            sampleBackup().copy(
                kdfParams = BackupKdfParams(memoryKib = 65536, iterations = 0, parallelism = 1),
            ),
        )
        try {
            codec.decodeBackup(bad)
            throw AssertionError("expected validation to fail")
        } catch (expected: IllegalArgumentException) {
            // expected
        }
    }
}
