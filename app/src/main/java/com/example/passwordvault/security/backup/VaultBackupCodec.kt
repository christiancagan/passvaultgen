package com.example.passwordvault.security.backup

import com.google.gson.Gson
import com.google.gson.JsonParseException

/**
 * Serializes/deserializes the `.pvault` backup format and validates it.
 *
 * Validation is strict: unsupported versions, unknown KDFs, invalid parameters,
 * and oversized payloads are rejected before any decryption is attempted. This
 * prevents malformed or malicious files from causing resource exhaustion or
 * code execution (the payload is data only, never executed).
 */
class VaultBackupCodec(private val gson: Gson) {

    fun encodeBackup(backup: VaultBackup): ByteArray =
        gson.toJson(backup).toByteArray(Charsets.UTF_8)

    fun decodeBackup(bytes: ByteArray): VaultBackup {
        require(bytes.size <= MAX_BACKUP_SIZE) { "Backup file too large" }
        val json = bytes.toString(Charsets.UTF_8)
        val backup = try {
            gson.fromJson(json, VaultBackup::class.java)
        } catch (e: JsonParseException) {
            throw IllegalArgumentException("Malformed backup file", e)
        } ?: throw IllegalArgumentException("Malformed backup file")
        validate(backup)
        return backup
    }

    fun encodeEntries(entries: List<BackupEntry>): ByteArray =
        gson.toJson(entries).toByteArray(Charsets.UTF_8)

    fun decodeEntries(bytes: ByteArray): List<BackupEntry> {
        val json = bytes.toString(Charsets.UTF_8)
        return try {
            gson.fromJson(json, Array<BackupEntry>::class.java).toList()
        } catch (e: JsonParseException) {
            throw IllegalArgumentException("Malformed backup payload", e)
        }
    }

    private fun validate(backup: VaultBackup) {
        require(backup.version == SUPPORTED_VERSION) { "Unsupported backup version" }
        require(backup.kdf == KDF_ARGON2ID) { "Unsupported KDF" }
        // A crafted backup must not be able to force a trivially cheap KDF
        // (an 8 KiB Argon2 makes offline master-password brute-force
        // effectively instant), nor a value that OOMs the device.
        require(backup.kdfParams.memoryKib in MIN_MEMORY_KIB..MAX_MEMORY_KIB) {
            "Invalid KDF memory parameter"
        }
        require(backup.kdfParams.iterations in 1..10) { "Invalid KDF iteration parameter" }
        require(backup.kdfParams.parallelism in 1..8) { "Invalid KDF parallelism parameter" }
        require(backup.salt.isNotBlank()) { "Missing salt" }
        require(backup.wrappedDek.isNotBlank()) { "Missing wrapped DEK" }
        require(backup.dekNonce.isNotBlank()) { "Missing DEK nonce" }
        require(backup.dataNonce.isNotBlank()) { "Missing data nonce" }
        require(backup.data.isNotBlank()) { "Missing data" }
    }

    companion object {
        const val SUPPORTED_VERSION = 1
        const val KDF_ARGON2ID = "argon2id"
        const val MAX_BACKUP_SIZE = 10 * 1024 * 1024 // 10 MiB

        /** Import floor/ceiling: matches Argon2Params.DEFAULT .. 1 GiB. */
        const val MIN_MEMORY_KIB = 64 * 1024
        const val MAX_MEMORY_KIB = 1024 * 1024
    }
}
