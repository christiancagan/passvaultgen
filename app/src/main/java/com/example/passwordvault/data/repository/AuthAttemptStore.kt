package com.example.passwordvault.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.authAttemptsDataStore by preferencesDataStore(name = "auth_attempts")

data class AuthAttemptState(
    val failedAttempts: Int = 0,
    /** Epoch millis until which unlocking is blocked. 0 = not blocked. */
    val lockoutUntil: Long = 0,
)

/**
 * Brute-force mitigation for master-password unlock.
 *
 * Argon2id already makes each guess expensive (~0.5s), but without a counter an
 * attacker with the device could guess indefinitely. After [FREE_ATTEMPTS]
 * failures, escalating lockouts apply (30s → 1min → 5min → 15min cap). A
 * successful unlock resets the counter. The state persists across process
 * restarts so killing the app does not reset it.
 */
class AuthAttemptStore(private val context: Context) {

    private object Keys {
        val failedAttempts = intPreferencesKey("failed_attempts")
        val lockoutUntil = longPreferencesKey("lockout_until")
    }

    val state: Flow<AuthAttemptState> = context.authAttemptsDataStore.data.map { prefs ->
        AuthAttemptState(
            failedAttempts = prefs[Keys.failedAttempts] ?: 0,
            lockoutUntil = prefs[Keys.lockoutUntil] ?: 0,
        )
    }

    /** Returns remaining block time in millis, or 0 if unlocking is allowed. */
    suspend fun lockoutRemainingMs(nowMs: Long = System.currentTimeMillis()): Long {
        val until = context.authAttemptsDataStore.data.first()[Keys.lockoutUntil] ?: 0
        return (until - nowMs).coerceAtLeast(0)
    }

    /**
     * Records a failed attempt. Returns the lockout imposed by this failure in
     * millis (0 = no lockout, caller shows the generic error).
     */
    suspend fun recordFailure(nowMs: Long = System.currentTimeMillis()): Long {
        val prefs = context.authAttemptsDataStore.data.first()
        val failed = (prefs[Keys.failedAttempts] ?: 0) + 1
        val lockoutMs = lockoutForFailures(failed)
        context.authAttemptsDataStore.edit {
            it[Keys.failedAttempts] = failed
            it[Keys.lockoutUntil] = if (lockoutMs > 0) nowMs + lockoutMs else 0
        }
        return lockoutMs
    }

    suspend fun recordSuccess() {
        context.authAttemptsDataStore.edit {
            it[Keys.failedAttempts] = 0
            it[Keys.lockoutUntil] = 0
        }
    }

    companion object {
        const val FREE_ATTEMPTS = 5
        const val MAX_LOCKOUT_MS = 15 * 60 * 1000L

        /**
         * Pure backoff policy (unit-testable): no lockout for the first
         * [FREE_ATTEMPTS] failures, then 30s doubling per failure, capped at
         * [MAX_LOCKOUT_MS].
         */
        fun lockoutForFailures(failedAttempts: Int): Long {
            if (failedAttempts <= FREE_ATTEMPTS) return 0
            var ms = 30_000L
            repeat(failedAttempts - FREE_ATTEMPTS - 1) {
                ms = (ms * 2).coerceAtMost(MAX_LOCKOUT_MS)
            }
            return ms.coerceAtMost(MAX_LOCKOUT_MS)
        }
    }
}
