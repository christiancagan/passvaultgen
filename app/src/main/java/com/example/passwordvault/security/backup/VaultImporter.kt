package com.example.passwordvault.security.backup

import com.example.passwordvault.security.crypto.AesGcmCipher
import com.example.passwordvault.security.crypto.Argon2idKdf
import com.example.passwordvault.security.crypto.Argon2Params
import com.example.passwordvault.security.crypto.VaultMetadata

/**
 * Imports and validates a `.pvault` backup. The master password is required to
 * derive the KEK and unwrap the DEK. Any tampering or malformed data causes a
 * generic failure (no crypto oracle is exposed to the caller).
 */
class VaultImporter(
    private val kdf: Argon2idKdf,
    private val cipher: AesGcmCipher,
    private val codec: VaultBackupCodec,
    private val b64: BackupBase64,
) {

    fun import(bytes: ByteArray, masterPassword: CharArray): ImportResult {
        val backup = codec.decodeBackup(bytes)

        val params = Argon2Params(
            memoryKib = backup.kdfParams.memoryKib,
            iterations = backup.kdfParams.iterations,
            parallelism = backup.kdfParams.parallelism,
        )
        val salt = b64.decode(backup.salt)
        val wrappedDek = b64.decode(backup.wrappedDek)
        val dekNonce = b64.decode(backup.dekNonce)

        val kek = kdf.derive(masterPassword, salt, params)
        val dek = try {
            cipher.decrypt(kek, dekNonce, wrappedDek)
        } finally {
            kek.fill(0)
        }

        val dataNonce = b64.decode(backup.dataNonce)
        val data = b64.decode(backup.data)
        val plaintext = cipher.decrypt(dek, dataNonce, data)
        val entries = try {
            codec.decodeEntries(plaintext)
        } finally {
            plaintext.fill(0)
        }

        // Caller takes ownership of dek (must zero after use, e.g. after
        // copying it into VaultSession). Do NOT zero it here.
        return ImportResult(
            metadata = VaultMetadata(
                version = backup.version,
                kdfParams = params,
                salt = salt,
                wrappedDek = wrappedDek,
                dekNonce = dekNonce,
            ),
            entries = entries,
            dek = dek,
        )
    }
}
