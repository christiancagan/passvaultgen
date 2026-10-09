package com.example.passwordvault.data.database

/**
 * Row projection containing only the columns needed to build autofill
 * candidates: id, title, and the encrypted URL (cipher + nonce).
 *
 * Deliberately excludes password/username/notes columns so a fill request
 * never pulls secret material out of the database — only the URL field is
 * decrypted, and only when the vault is unlocked.
 */
data class VaultEntryUrlProjection(
    val id: String,
    val title: String,
    val category: String,
    val urlCipher: ByteArray,
    val urlNonce: ByteArray,
    val favorite: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as VaultEntryUrlProjection
        if (id != other.id) return false
        if (title != other.title) return false
        if (category != other.category) return false
        if (!urlCipher.contentEquals(other.urlCipher)) return false
        if (!urlNonce.contentEquals(other.urlNonce)) return false
        if (favorite != other.favorite) return false
        if (createdAt != other.createdAt) return false
        if (updatedAt != other.updatedAt) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + category.hashCode()
        result = 31 * result + urlCipher.contentHashCode()
        result = 31 * result + urlNonce.contentHashCode()
        result = 31 * result + favorite.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        return result
    }
}
