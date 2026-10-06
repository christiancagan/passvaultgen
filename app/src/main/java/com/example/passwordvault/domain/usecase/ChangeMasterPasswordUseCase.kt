package com.example.passwordvault.domain.usecase

import com.example.passwordvault.data.repository.VaultMetadataStore
import com.example.passwordvault.security.crypto.Argon2Params
import com.example.passwordvault.security.crypto.VaultCryptoService
import com.example.passwordvault.security.crypto.VaultSession
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * Changes the master password. The DEK itself is unchanged, so no record
 * re-encryption is needed — the DEK is simply re-wrapped under a KEK derived
 * from the new password. The biometric wrap (if any) stays valid because it
 * protects the same DEK, and is carried over to the new metadata.
 *
 * Throws on an incorrect current password or corrupted metadata; the caller
 * maps this to a generic error (no oracle).
 */
class ChangeMasterPasswordUseCase @Inject constructor(
    private val crypto: VaultCryptoService,
    private val metadataStore: VaultMetadataStore,
    private val session: VaultSession,
) {
    suspend operator fun invoke(currentPassword: CharArray, newPassword: CharArray) {
        val old = metadataStore.metadata.firstOrNull()
            ?: throw IllegalStateException("No vault exists")
        val dek = crypto.unlock(currentPassword, old)
        try {
            val updated = crypto.rewrapDek(dek, newPassword, Argon2Params.DEFAULT)
            metadataStore.save(
                updated.copy(
                    biometricWrappedDek = old.biometricWrappedDek,
                    biometricDekNonce = old.biometricDekNonce,
                ),
            )
            session.unlock(dek)
        } finally {
            dek.fill(0)
        }
    }
}
