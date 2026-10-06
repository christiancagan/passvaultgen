package com.example.passwordvault.domain.usecase

import com.example.passwordvault.security.crypto.VaultSession
import javax.inject.Inject

/** Locks the vault, zeroing the in-memory DEK. */
class LockVaultUseCase @Inject constructor(
    private val session: VaultSession,
) {
    operator fun invoke() = session.lock()
}
