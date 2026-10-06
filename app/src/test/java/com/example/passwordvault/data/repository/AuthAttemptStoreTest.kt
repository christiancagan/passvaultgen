package com.example.passwordvault.data.repository

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AuthAttemptStoreTest {

    @Test
    fun `first five failures impose no lockout`() {
        for (i in 1..AuthAttemptStore.FREE_ATTEMPTS) {
            assertThat(AuthAttemptStore.lockoutForFailures(i)).isEqualTo(0)
        }
    }

    @Test
    fun `lockout escalates after free attempts`() {
        assertThat(AuthAttemptStore.lockoutForFailures(6)).isEqualTo(30_000L)
        assertThat(AuthAttemptStore.lockoutForFailures(7)).isEqualTo(60_000L)
        assertThat(AuthAttemptStore.lockoutForFailures(8)).isEqualTo(120_000L)
    }

    @Test
    fun `lockout is capped`() {
        assertThat(AuthAttemptStore.lockoutForFailures(100)).isEqualTo(AuthAttemptStore.MAX_LOCKOUT_MS)
        assertThat(AuthAttemptStore.lockoutForFailures(100)).isAtMost(15 * 60 * 1000L)
    }
}
