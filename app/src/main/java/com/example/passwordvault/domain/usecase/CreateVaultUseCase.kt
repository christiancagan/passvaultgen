package com.example.passwordvault.domain.usecase

import com.example.passwordvault.data.repository.VaultMetadataStore
import com.example.passwordvault.security.crypto.Argon2Params
import com.example.passwordvault.security.crypto.VaultCryptoService
import com.example.passwordvault.security.crypto.VaultSession
import javax.inject.Inject

/**
 * Creates a new vault from a master password. Generates the DEK, derives the
 * KEK via Argon2id, wraps the DEK, persists metadata, and unlocks the session.
 */
class CreateVaultUseCase @Inject constructor(
    private val crypto: VaultCryptoService,
    private val metadataStore: VaultMetadataStore,
    private val session: VaultSession,
) {
    suspend operator fun invoke(masterPassword: CharArray, params: Argon2Params = Argon2Params.DEFAULT) {
        val metadata = crypto.createVault(masterPassword, params)
        val dek = crypto.unlock(masterPassword, metadata)
        try {
            metadataStore.save(metadata)
            session.unlock(dek)
        } finally {
            dek.fill(0)
        }
    }
}
