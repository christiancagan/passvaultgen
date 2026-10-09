package com.example.passwordvault.security.password

import com.example.passwordvault.security.random.SecureRandomProvider
import kotlin.math.log2

/**
 * Options for random password generation.
 */
data class PasswordOptions(
    val length: Int = 20,
    val includeUppercase: Boolean = true,
    val includeLowercase: Boolean = true,
    val includeNumbers: Boolean = true,
    val includeSymbols: Boolean = true,
    val excludeAmbiguous: Boolean = false,
)

/**
 * Cryptographically secure password and passphrase generator.
 *
 * Uses [SecureRandomProvider] exclusively. Never [Math.random], timestamps,
 * device identifiers, usernames, or any predictable seed.
 */
class PasswordGenerator(
    private val random: SecureRandomProvider,
) {

    private val lowercase = "abcdefghijklmnopqrstuvwxyz"
    private val uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private val numbers = "0123456789"
    private val symbols = "!@#$%^&*()-_=+[]{};:,.<>?/~"
    private val ambiguous = "Il1O0o5S2Z8B6G"

    /**
     * Generates a random password from the selected character classes.
     */
    fun generate(options: PasswordOptions): String {
        val charset = buildCharset(options)
        require(charset.isNotEmpty()) { "at least one character class must be selected" }
        val sb = StringBuilder(options.length)
        repeat(options.length) {
            sb.append(charset[random.nextInt(charset.length)])
        }
        return sb.toString()
    }

    /**
     * Generates a random passphrase from a fixed word list. Word count and
     * separator are configurable. Entropy is `wordCount * log2(wordListSize)`.
     */
    fun generatePassphrase(wordCount: Int = 6, separator: String = "-"): String {
        require(wordCount >= 3) { "wordCount must be >= 3" }
        val words = (0 until wordCount).joinToString(separator) {
            WORD_LIST[random.nextInt(WORD_LIST.size)]
        }
        return words
    }

    /** Estimated entropy in bits: `length * log2(charsetSize)`. */
    fun estimateEntropy(length: Int, charsetSize: Int): Double {
        require(length > 0 && charsetSize > 1)
        return length * log2(charsetSize.toDouble())
    }

    /** Size of the character set that [options] would produce. */
    fun charsetSize(options: PasswordOptions): Int = buildCharset(options).length

    private fun buildCharset(options: PasswordOptions): String {
        val sb = StringBuilder()
        if (options.includeLowercase) sb.append(lowercase)
        if (options.includeUppercase) sb.append(uppercase)
        if (options.includeNumbers) sb.append(numbers)
        if (options.includeSymbols) sb.append(symbols)
        var charset = sb.toString()
        if (options.excludeAmbiguous) {
            charset = charset.filter { it !in ambiguous }
        }
        return charset
    }

    companion object {
        /**
         * Built-in passphrase word list: the EFF large diceware list
         * (7,776 entries — see [EffWordList]), deduplicated, lowercase a–z
         * with hyphenated forms (e.g. "t-shirt"). Entropy for `n` words is
         * `n * log2(WORD_LIST.size)` ≈ `n * 12.92` bits (6 words ≈ 77.5 bits).
         */
        val WORD_LIST: List<String> = EffWordList.ALL.distinct()
    }

    /** Entropy of a passphrase of [wordCount] words from [WORD_LIST]. */
    fun passphraseEntropyBits(wordCount: Int): Double =
        wordCount * log2(WORD_LIST.size.toDouble())
}
