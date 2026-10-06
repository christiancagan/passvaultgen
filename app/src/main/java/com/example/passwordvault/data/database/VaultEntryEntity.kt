package com.example.passwordvault.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A vault entry. Sensitive fields (name, username, password, url, notes) are
 * stored as AES-256-GCM ciphertext with a per-field nonce. Only [title] and
 * [category] are stored in plaintext, and they are non-sensitive display/search
 * metadata (never the actual password or secret notes).
 */
@Entity(tableName = "vault_entries")
data class VaultEntryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val nameCipher: ByteArray,
    val nameNonce: ByteArray,
    val usernameCipher: ByteArray,
    val usernameNonce: ByteArray,
    val passwordCipher: ByteArray,
    val passwordNonce: ByteArray,
    val urlCipher: ByteArray,
    val urlNonce: ByteArray,
    val notesCipher: ByteArray,
    val notesNonce: ByteArray,
    val createdAt: Long,
    val updatedAt: Long,
    val favorite: Boolean,
    val version: Int,
)
