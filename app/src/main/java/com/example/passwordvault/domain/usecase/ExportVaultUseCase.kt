package com.example.passwordvault.domain.usecase

import com.example.passwordvault.data.repository.VaultMetadataStore
import com.example.passwordvault.data.repository.VaultRepository
import com.example.passwordvault.security.backup.BackupEntry
import com.example.passwordvault.security.backup.VaultExporter
import com.example.passwordvault.security.crypto.VaultSession
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * Exports the unlocked vault to encrypted `.pvault` bytes. The caller writes
 * the bytes to the user-chosen location (Storage Access Framework), so this
 * use case performs no file I/O.
 */
class ExportVaultUseCase @Inject constructor(
    private val exporter: VaultExporter,
    private val repository: VaultRepository,
    private val metadataStore: VaultMetadataStore,
    private val session: VaultSession,
) {
    suspend operator fun invoke(): ByteArray {
        val metadata = metadataStore.metadata.firstOrNull()
            ?: throw IllegalStateException("No vault exists")
        val entries = repository.getAllDecrypted().map {
            BackupEntry(
                title = it.title,
                category = it.category,
                name = it.name,
                username = it.username,
                password = it.password,
                url = it.url,
                notes = it.notes,
                favorite = it.favorite,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt,
            )
        }
        return session.withDek { dek -> exporter.export(dek, entries, metadata) }
    }
}
