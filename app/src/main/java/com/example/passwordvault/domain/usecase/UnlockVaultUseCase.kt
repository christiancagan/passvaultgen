package com.example.passwordvault.domain.usecase

import com.example.passwordvault.data.repository.VaultMetadataStore
import com.example.passwordvault.security.crypto.VaultCryptoService
import com.example.passwordvault.security.crypto.VaultSession
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * Unlocks the vault with the master password. Throws on an incorrect password
 * or tampered metadata (the caller maps this to a generic user-facing error).
 */
class UnlockVaultUseCase @Inject constructor(
    private val crypto: VaultCryptoService,
    private val metadataStore: VaultMetadataStore,
    private val session: VaultSession,
) {
    suspend operator fun invoke(masterPassword: CharArray) {
        val metadata = metadataStore.metadata.firstOrNull()
            ?: throw IllegalStateException("No vault exists")
        val dek = crypto.unlock(masterPassword, metadata)
        try {
            session.unlock(dek)
        } finally {
            dek.fill(0)
        }
    }
}
