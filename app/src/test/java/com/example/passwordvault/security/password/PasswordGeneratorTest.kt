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
        // Split on space: hyphenated EFF entries ("t-shirt") contain "-".
        val passphrase = generator.generatePassphrase(wordCount = 6, separator = " ")
        assertThat(passphrase.split(" ")).hasSize(6)
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

    @Test
    fun `word list has no duplicates`() {
        val list = PasswordGenerator.WORD_LIST
        assertThat(list.toSet().size).isEqualTo(list.size)
    }

    @Test
    fun `word list entries are lowercase diceware words`() {
        val invalid = PasswordGenerator.WORD_LIST.filter { !it.matches(Regex("[a-z]+(-[a-z]+)*")) }
        assertThat(invalid).isEmpty()
    }

    @Test
    fun `word list is the EFF large list`() {
        assertThat(PasswordGenerator.WORD_LIST).hasSize(7776)
        assertThat(generator.passphraseEntropyBits(6)).isAtLeast(77.0)
    }

    @Test
    fun `word list is large enough for meaningful entropy`() {
        // Floor of 900 words => >= 9.8 bits/word, so a 6-word passphrase
        // carries >= 58 bits even at the low end.
        assertThat(PasswordGenerator.WORD_LIST.size).isAtLeast(900)
        assertThat(generator.passphraseEntropyBits(6)).isAtLeast(55.0)
    }

    @Test
    fun `passphrase words all come from the word list`() {
        val passphrase = generator.generatePassphrase(wordCount = 12, separator = " ")
        val words = passphrase.split(" ")
        assertThat(words).hasSize(12)
        for (word in words) {
            assertThat(PasswordGenerator.WORD_LIST).contains(word)
        }
    }

    @Test
    fun `passphrase entropy scales with word count`() {
        assertThat(generator.passphraseEntropyBits(6))
            .isGreaterThan(generator.passphraseEntropyBits(5))
    }
}
