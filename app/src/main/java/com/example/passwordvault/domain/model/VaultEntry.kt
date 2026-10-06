package com.example.passwordvault.domain.model

/** Non-sensitive summary of a vault entry, safe to show in lists without decryption. */
data class VaultEntrySummary(
    val id: String,
    val title: String,
    val category: String,
    val favorite: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

/** Fully decrypted vault entry. Only materialized on demand (detail view). */
data class VaultEntry(
    val id: String,
    val title: String,
    val category: String,
    val name: String,
    val username: String,
    val password: String,
    val url: String,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
    val favorite: Boolean,
)

/** Input for creating or editing an entry. */
data class VaultEntryDraft(
    val title: String,
    val category: String,
    val name: String,
    val username: String,
    val password: String,
    val url: String,
    val notes: String,
    val favorite: Boolean,
)
