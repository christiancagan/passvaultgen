package com.example.passwordvault.data.repository

import com.example.passwordvault.data.database.VaultEntryDao
import com.example.passwordvault.data.database.VaultEntryEntity
import com.example.passwordvault.domain.model.VaultEntry
import com.example.passwordvault.domain.model.VaultEntryDraft
import com.example.passwordvault.domain.model.VaultEntrySummary
import com.example.passwordvault.security.backup.BackupEntry
import com.example.passwordvault.security.crypto.AesGcmCipher
import com.example.passwordvault.security.crypto.VaultSession
import com.example.passwordvault.security.random.SecureRandomProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Bridges the encrypted Room database and the domain layer.
 *
 * Sensitive fields are encrypted/decrypted per-field with the session DEK. List
 * operations use only plaintext metadata (title/category/favorite), so no
 * decryption happens for the list; full decryption occurs only on detail view.
 */
class VaultRepository(
    private val dao: VaultEntryDao,
    private val cipher: AesGcmCipher,
    private val session: VaultSession,
    private val random: SecureRandomProvider,
) {

    fun observeAll(): Flow<List<VaultEntrySummary>> =
        dao.observeAll().map { list -> list.map { it.toSummary() } }

    fun observeFavorites(): Flow<List<VaultEntrySummary>> =
        dao.observeFavorites().map { list -> list.map { it.toSummary() } }

    fun observeByCategory(category: String): Flow<List<VaultEntrySummary>> =
        dao.observeByCategory(category).map { list -> list.map { it.toSummary() } }

    fun observeCategories(): Flow<List<String>> = dao.observeCategories()

    fun search(query: String): Flow<List<VaultEntrySummary>> =
        dao.search(query).map { list -> list.map { it.toSummary() } }

/** Decrypts and returns a single entry. Throws [VaultLockedException] if locked. */
    suspend fun getEntry(id: String): VaultEntry? {
        val entity = dao.getById(id) ?: return null
        return decrypt(entity)
    }

    /** Decrypts all entries. Used only for explicit, user-initiated security analysis. */
    suspend fun getAllDecrypted(): List<VaultEntry> = dao.getAll().map { decrypt(it) }

    suspend fun add(draft: VaultEntryDraft): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val entity = encrypt(id, draft, now, now)
        dao.insert(entity)
        return id
    }

    suspend fun update(id: String, draft: VaultEntryDraft) {
        val existing = dao.getById(id) ?: return
        val entity = encrypt(id, draft, existing.createdAt, System.currentTimeMillis())
        dao.update(entity)
    }

    suspend fun delete(id: String) {
        dao.getById(id)?.let { dao.delete(it) }
    }

    suspend fun deleteAll() = dao.deleteAll()

    /**
     * Atomically replaces all entries with imported [entries]. Each entry gets a
     * fresh random ID (backup files carry no IDs, avoiding cross-device
     * collisions); timestamps are preserved. Requires the vault to be unlocked.
     */
    suspend fun replaceAll(entries: List<BackupEntry>) {
        val entities = entries.map { entry ->
            val draft = VaultEntryDraft(
                title = entry.title,
                category = entry.category,
                name = entry.name,
                username = entry.username,
                password = entry.password,
                url = entry.url,
                notes = entry.notes,
                favorite = entry.favorite,
            )
            encrypt(UUID.randomUUID().toString(), draft, entry.createdAt, entry.updatedAt)
        }
        dao.replaceAll(entities)
    }

    private fun encrypt(id: String, draft: VaultEntryDraft, createdAt: Long, updatedAt: Long): VaultEntryEntity {
        val dek = session.requireDek()
        val aad = id.toByteArray(Charsets.UTF_8)
        val name = cipher.encrypt(dek, draft.name.toByteArray(Charsets.UTF_8), aad)
        val username = cipher.encrypt(dek, draft.username.toByteArray(Charsets.UTF_8), aad)
        val password = cipher.encrypt(dek, draft.password.toByteArray(Charsets.UTF_8), aad)
        val url = cipher.encrypt(dek, draft.url.toByteArray(Charsets.UTF_8), aad)
        val notes = cipher.encrypt(dek, draft.notes.toByteArray(Charsets.UTF_8), aad)
        return VaultEntryEntity(
            id = id,
            title = draft.title,
            category = draft.category,
            nameCipher = name.ciphertext,
            nameNonce = name.nonce,
            usernameCipher = username.ciphertext,
            usernameNonce = username.nonce,
            passwordCipher = password.ciphertext,
            passwordNonce = password.nonce,
            urlCipher = url.ciphertext,
            urlNonce = url.nonce,
            notesCipher = notes.ciphertext,
            notesNonce = notes.nonce,
            createdAt = createdAt,
            updatedAt = updatedAt,
            favorite = draft.favorite,
            version = 1,
        )
    }

    private fun decrypt(entity: VaultEntryEntity): VaultEntry {
        val dek = session.requireDek()
        val aad = entity.id.toByteArray(Charsets.UTF_8)
        return VaultEntry(
            id = entity.id,
            title = entity.title,
            category = entity.category,
            name = cipher.decrypt(dek, entity.nameNonce, entity.nameCipher, aad).toString(Charsets.UTF_8),
            username = cipher.decrypt(dek, entity.usernameNonce, entity.usernameCipher, aad).toString(Charsets.UTF_8),
            password = cipher.decrypt(dek, entity.passwordNonce, entity.passwordCipher, aad).toString(Charsets.UTF_8),
            url = cipher.decrypt(dek, entity.urlNonce, entity.urlCipher, aad).toString(Charsets.UTF_8),
            notes = cipher.decrypt(dek, entity.notesNonce, entity.notesCipher, aad).toString(Charsets.UTF_8),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            favorite = entity.favorite,
        )
    }

    private fun VaultEntryEntity.toSummary() = VaultEntrySummary(
        id = id,
        title = title,
        category = category,
        favorite = favorite,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
