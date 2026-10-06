package com.example.passwordvault.security.backup

import com.example.passwordvault.security.crypto.AesGcmCipher
import com.example.passwordvault.security.crypto.VaultMetadata

/**
 * Produces an encrypted `.pvault` backup. The vault must be unlocked (the DEK
 * is required). The backup is self-contained: it carries the KDF parameters,
 * salt, wrapped DEK, and the DEK-encrypted entries, so it can be restored on
 * another device using only the master password.
 */
class VaultExporter(
    private val cipher: AesGcmCipher,
    private val codec: VaultBackupCodec,
    private val b64: BackupBase64,
) {

    fun export(dek: ByteArray, entries: List<BackupEntry>, metadata: VaultMetadata): ByteArray {
        val plaintext = codec.encodeEntries(entries)
        val encrypted = cipher.encrypt(dek, plaintext)
        val backup = VaultBackup(
            version = VaultBackupCodec.SUPPORTED_VERSION,
            kdf = VaultBackupCodec.KDF_ARGON2ID,
            kdfParams = BackupKdfParams(
                memoryKib = metadata.kdfParams.memoryKib,
                iterations = metadata.kdfParams.iterations,
                parallelism = metadata.kdfParams.parallelism,
            ),
            salt = b64.encode(metadata.salt),
            wrappedDek = b64.encode(metadata.wrappedDek),
            dekNonce = b64.encode(metadata.dekNonce),
            dataNonce = b64.encode(encrypted.nonce),
            data = b64.encode(encrypted.ciphertext),
        )
        return codec.encodeBackup(backup)
    }
}
