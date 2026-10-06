package com.example.passwordvault.security.password

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PasswordStrengthAnalyzerTest {

    private val analyzer = PasswordStrengthAnalyzer()

    @Test
    fun `common password is flagged`() {
        val result = analyzer.analyze("password")
        assertThat(result.issues).contains("Common password")
        assertThat(result.score).isAtMost(1)
    }

    @Test
    fun `short password is flagged`() {
        val result = analyzer.analyze("abc")
        assertThat(result.issues.any { it.contains("short", ignoreCase = true) }).isTrue()
    }

    @Test
    fun `sequential pattern is flagged`() {
        val result = analyzer.analyze("abcdefghijklmnop")
        assertThat(result.issues).contains("Sequential pattern")
    }

    @Test
    fun `username inclusion is flagged`() {
        val result = analyzer.analyze("johnsmith123", username = "johnsmith")
        assertThat(result.issues).contains("Contains username")
    }

    @Test
    fun `strong password is not flagged`() {
        val result = analyzer.analyze("vX9#kL2mQ7wP4zR8tY5")
        assertThat(result.score).isAtLeast(3)
    }

    @Test
    fun `entropy estimate is positive for non-empty password`() {
        assertThat(analyzer.estimateEntropy("password")).isGreaterThan(0.0)
        assertThat(analyzer.estimateEntropy("")).isEqualTo(0.0)
    }
}
