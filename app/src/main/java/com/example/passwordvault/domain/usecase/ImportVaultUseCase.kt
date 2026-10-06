package com.example.passwordvault.domain.usecase

import com.example.passwordvault.data.repository.VaultMetadataStore
import com.example.passwordvault.data.repository.VaultRepository
import com.example.passwordvault.security.backup.VaultImporter
import com.example.passwordvault.security.crypto.VaultSession
import javax.inject.Inject

/**
 * Restores a `.pvault` backup. The backup is self-contained (KDF params, salt,
 * wrapped DEK, encrypted entries), so the master password alone unlocks it.
 *
 * On success the vault metadata is replaced (biometric enrollment is cleared
 * and must be re-enabled), the session is unlocked, and the entries atomically
 * replace the current ones. Returns the number of restored entries.
 */
class ImportVaultUseCase @Inject constructor(
    private val importer: VaultImporter,
    private val metadataStore: VaultMetadataStore,
    private val session: VaultSession,
    private val repository: VaultRepository,
) {
    suspend operator fun invoke(bytes: ByteArray, masterPassword: CharArray): Int {
        val result = importer.import(bytes, masterPassword)
        try {
            metadataStore.save(result.metadata)
            session.unlock(result.dek)
            repository.replaceAll(result.entries)
            return result.entries.size
        } finally {
            result.dek.fill(0)
        }
    }
}
