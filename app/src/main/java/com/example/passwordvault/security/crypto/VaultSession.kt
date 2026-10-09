package com.example.passwordvault.security.crypto

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Thrown when a decryption is attempted while the vault is locked. */
class VaultLockedException : IllegalStateException("Vault is locked")

/**
 * Holds the plaintext DEK in memory only while the vault is unlocked.
 *
 * The DEK is the only long-lived secret in memory. It is zeroed (best-effort)
 * on [lock]. Records are decrypted on demand and never cached in bulk.
 */
class VaultSession {

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    @Volatile
    private var dek: ByteArray? = null

    /**
     * Unlocks the session with [dek]. The array is copied, so the caller retains
     * ownership of its copy and must zero it after calling this.
     */
    fun unlock(dek: ByteArray) {
        lock()
        this.dek = dek.copyOf()
        _isUnlocked.value = true
    }

    fun lock() {
        dek?.fill(0)
        dek = null
        _isUnlocked.value = false
    }

    /** Returns the DEK or throws [VaultLockedException] if locked. */
    fun requireDek(): ByteArray = dek ?: throw VaultLockedException()

    /**
     * Runs [block] with a private, zeroed copy of the DEK.
     *
     * Preferred over [requireDek]: the caller never holds a reference to
     * the live DEK array, so it cannot leak or mutate long-lived key
     * material. The copy is zeroed on exit (even on exception).
     */
    fun <T> withDek(block: (ByteArray) -> T): T {
        val live = dek ?: throw VaultLockedException()
        val copy = live.copyOf()
        return try {
            block(copy)
        } finally {
            copy.fill(0)
        }
    }
}
