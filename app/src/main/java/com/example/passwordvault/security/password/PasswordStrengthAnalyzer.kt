package com.example.passwordvault.security.password

import kotlin.math.log2

/**
 * Result of a local password strength analysis. Never sent off-device.
 */
data class PasswordStrength(
    val score: Int, // 0..4
    val entropyBits: Double,
    val issues: List<String>,
) {
    val label: String
        get() = when (score) {
            0 -> "Very weak"
            1 -> "Weak"
            2 -> "Fair"
            3 -> "Good"
            4 -> "Strong"
            else -> "Unknown"
        }
}

/**
 * Local, offline password strength analysis.
 *
 * Detects common weaknesses without ever uploading the password. This is a
 * heuristic estimate, not a guarantee of security.
 */
class PasswordStrengthAnalyzer {

    fun analyze(password: String, username: String? = null, domain: String? = null): PasswordStrength {
        val issues = mutableListOf<String>()
        val length = password.length

        if (length < 8) issues += "Too short (fewer than 8 characters)"
        if (length < 12) issues += "Short (fewer than 12 characters)"

        if (password.lowercase() in COMMON_PASSWORDS) issues += "Common password"

        if (password.all { it.isDigit() }) issues += "Only numbers"
        if (password.all { it.isLetter() }) issues += "Only letters"

        if (hasRepeatedCharacters(password)) issues += "Repeated characters"
        if (hasSequentialPattern(password)) issues += "Sequential pattern"
        if (hasKeyboardPattern(password)) issues += "Keyboard pattern"

        username?.let { u ->
            if (u.isNotBlank() && password.contains(u, ignoreCase = true)) {
                issues += "Contains username"
            }
        }
        domain?.let { d ->
            val host = d.substringBefore('/').substringBefore('?')
            if (host.isNotBlank() && password.contains(host, ignoreCase = true)) {
                issues += "Contains domain"
            }
        }

        val entropy = estimateEntropy(password)
        if (entropy < 40) issues += "Low estimated entropy"

        val score = when {
            issues.isEmpty() && length >= 16 -> 4
            issues.size <= 1 && length >= 12 -> 3
            issues.size <= 2 -> 2
            issues.size <= 4 -> 1
            else -> 0
        }

        return PasswordStrength(score = score, entropyBits = entropy, issues = issues)
    }

    /** Rough entropy estimate based on observed character classes. */
    fun estimateEntropy(password: String): Double {
        if (password.isEmpty()) return 0.0
        var pool = 0
        if (password.any { it.isLowerCase() }) pool += 26
        if (password.any { it.isUpperCase() }) pool += 26
        if (password.any { it.isDigit() }) pool += 10
        if (password.any { !it.isLetterOrDigit() }) pool += 33
        if (pool == 0) pool = 1
        return password.length * log2(pool.toDouble())
    }

    private fun hasRepeatedCharacters(password: String): Boolean {
        var run = 1
        for (i in 1 until password.length) {
            run = if (password[i] == password[i - 1]) run + 1 else 1
            if (run >= 3) return true
        }
        return false
    }

    private fun hasSequentialPattern(password: String): Boolean {
        val lower = password.lowercase()
        for (i in 0 until lower.length - 2) {
            val a = lower[i].code
            val b = lower[i + 1].code
            val c = lower[i + 2].code
            if (b - a == 1 && c - b == 1) return true
            if (a - b == 1 && b - c == 1) return true
        }
        return false
    }

    private fun hasKeyboardPattern(password: String): Boolean {
        val lower = password.lowercase()
        val rows = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
        for (row in rows) {
            for (i in 0 until row.length - 2) {
                val seq = row.substring(i, i + 3)
                if (lower.contains(seq) || lower.contains(seq.reversed())) return true
            }
        }
        return false
    }

    companion object {
        private val COMMON_PASSWORDS = setOf(
            "password", "password1", "123456", "12345678", "123456789", "qwerty",
            "abc123", "letmein", "welcome", "admin", "iloveyou", "monkey", "dragon",
            "111111", "123123", "1234567890", "000000", "654321", "football",
            "baseball", "superman", "batman", "trustno1", "sunshine", "master",
            "shadow", "passw0rd", "p@ssw0rd", "qwerty123", "1q2w3e4r", "login",
        )
    }
}
