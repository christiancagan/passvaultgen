package com.example.passwordvault.security.backup

import com.example.passwordvault.security.crypto.VaultMetadata

/**
 * A single vault entry in the backup file. Plaintext within the encrypted
 * payload; never written to disk unencrypted.
 */
data class BackupEntry(
    val title: String,
    val category: String,
    val name: String,
    val username: String,
    val password: String,
    val url: String,
    val notes: String,
    val favorite: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

/** Argon2id parameters serialized in the backup header. */
data class BackupKdfParams(
    val memoryKib: Int,
    val iterations: Int,
    val parallelism: Int,
)

/**
 * The `.pvault` backup file format. All binary fields are Base64-encoded.
 * Contains NO plaintext secrets: the DEK is wrapped and the entries are
 * encrypted with the DEK.
 */
data class VaultBackup(
    val version: Int,
    val kdf: String,
    val kdfParams: BackupKdfParams,
    val salt: String,
    val wrappedDek: String,
    val dekNonce: String,
    val dataNonce: String,
    val data: String,
)

/** Result of a successful import: the restored metadata, decrypted entries, and the DEK. */
data class ImportResult(
    val metadata: VaultMetadata,
    val entries: List<BackupEntry>,
    val dek: ByteArray,
)
