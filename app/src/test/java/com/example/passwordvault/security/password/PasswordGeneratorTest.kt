package com.example.passwordvault.security.password

import com.example.passwordvault.security.random.SecureRandomProviderImpl
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PasswordGeneratorTest {

    private val generator = PasswordGenerator(SecureRandomProviderImpl())

    @Test
    fun `generated password has requested length`() {
        val options = PasswordOptions(length = 20)
        val password = generator.generate(options)
        assertThat(password.length).isEqualTo(20)
    }

    @Test
    fun `generated password uses only selected charset`() {
        val options = PasswordOptions(
            length = 50,
            includeUppercase = false,
            includeLowercase = true,
            includeNumbers = false,
            includeSymbols = false,
        )
        val password = generator.generate(options)
        assertThat(password.all { it.isLowerCase() }).isTrue()
    }

    @Test
    fun `two generations differ`() {
        val options = PasswordOptions(length = 32)
        val a = generator.generate(options)
        val b = generator.generate(options)
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `entropy estimate is correct`() {
        // 20 chars from a 94-char set => 20 * log2(94)
        val entropy = generator.estimateEntropy(20, 94)
        assertThat(entropy).isWithin(0.01).of(20 * kotlin.math.log2(94.0))
    }

    @Test
    fun `passphrase has requested word count`() {
        val passphrase = generator.generatePassphrase(wordCount = 6, separator = "-")
        assertThat(passphrase.split("-").size).isEqualTo(6)
    }

    @Test
    fun `empty charset is rejected`() {
        val options = PasswordOptions(
            includeUppercase = false,
            includeLowercase = false,
            includeNumbers = false,
            includeSymbols = false,
        )
        try {
            generator.generate(options)
            throw AssertionError("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // expected
        }
    }
}
