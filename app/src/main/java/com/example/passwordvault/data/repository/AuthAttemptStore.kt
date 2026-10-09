package com.example.passwordvault.data.repository

import android.content.Context
import android.os.SystemClock
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
    /** Elapsed-realtime deadline; unaffected by wall-clock changes. */
    val lockoutUntilElapsed: Long = 0,
)

/**
 * Brute-force mitigation for master-password unlock.
 *
 * Argon2id already makes each guess expensive (~0.5s), but without a counter an
 * attacker with the device could guess indefinitely. After [FREE_ATTEMPTS]
 * failures, escalating lockouts apply (30s → 1min → 5min → 15min cap). A
 * successful unlock resets the counter. The state persists across process
 * restarts so killing the app does not reset it.
 *
 * The lockout deadline is stored twice — wall-clock and elapsed-realtime — and
 * the remaining block is the max of both, so neither moving the system clock
 * forward nor a combination trick shortens the wait.
 */
class AuthAttemptStore(private val context: Context) {

    private object Keys {
        val failedAttempts = intPreferencesKey("failed_attempts")
        val lockoutUntil = longPreferencesKey("lockout_until")
        val lockoutUntilElapsed = longPreferencesKey("lockout_until_elapsed")
    }

    val state: Flow<AuthAttemptState> = context.authAttemptsDataStore.data.map { prefs ->
        AuthAttemptState(
            failedAttempts = prefs[Keys.failedAttempts] ?: 0,
            lockoutUntil = prefs[Keys.lockoutUntil] ?: 0,
            lockoutUntilElapsed = prefs[Keys.lockoutUntilElapsed] ?: 0,
        )
    }

    /**
     * Returns remaining block time in millis, or 0 if unlocking is allowed.
     * Takes the larger of the wall-clock and elapsed-realtime readings.
     */
    suspend fun lockoutRemainingMs(nowMs: Long = System.currentTimeMillis()): Long {
        val prefs = context.authAttemptsDataStore.data.first()
        val until = prefs[Keys.lockoutUntil] ?: 0
        val untilElapsed = prefs[Keys.lockoutUntilElapsed] ?: 0
        val wallRemaining = (until - nowMs).coerceAtLeast(0)
        val elapsedRemaining =
            (untilElapsed - SystemClock.elapsedRealtime()).coerceAtLeast(0)
        return maxOf(wallRemaining, elapsedRemaining)
    }

    /**
     * Records a failed attempt. Returns the lockout imposed by this failure in
     * millis (0 = no lockout, caller shows the generic error).
     */
    suspend fun recordFailure(nowMs: Long = System.currentTimeMillis()): Long {
        val prefs = context.authAttemptsDataStore.data.first()
        val failed = (prefs[Keys.failedAttempts] ?: 0) + 1
        val lockoutMs = lockoutForFailures(failed)
        val elapsedBase = SystemClock.elapsedRealtime()
        context.authAttemptsDataStore.edit {
            it[Keys.failedAttempts] = failed
            it[Keys.lockoutUntil] = if (lockoutMs > 0) nowMs + lockoutMs else 0
            it[Keys.lockoutUntilElapsed] =
                if (lockoutMs > 0) elapsedBase + lockoutMs else 0
        }
        return lockoutMs
    }

    suspend fun recordSuccess() {
        context.authAttemptsDataStore.edit {
            it[Keys.failedAttempts] = 0
            it[Keys.lockoutUntil] = 0
            it[Keys.lockoutUntilElapsed] = 0
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
