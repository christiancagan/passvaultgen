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
        /** A modest built-in word list for passphrases. */
        val WORD_LIST = listOf(
            "apple", "bridge", "candle", "dolphin", "eagle", "forest", "garden", "harbor",
            "island", "jungle", "kettle", "lantern", "meadow", "nectar", "ocean", "pebble",
            "quartz", "river", "saddle", "timber", "umber", "valley", "willow", "xenon",
            "yellow", "zephyr", "anchor", "breeze", "cedar", "dune", "ember", "falcon",
            "glacier", "hazel", "ivory", "jasmine", "kayak", "lilac", "maple", "north",
            "orchid", "prairie", "quill", "raven", "sierra", "tundra", "urchin", "violet",
            "walnut", "yonder", "zinc", "acorn", "birch", "coral", "delta", "elm",
            "fern", "granite", "heron", "iris", "juniper", "koala", "larch", "moss",
            "nimbus", "otter", "pine", "quarry", "reef", "spruce", "thistle", "vapor",
            "wren", "yarrow", "zebra", "alder", "basalt", "canyon", "daisy", "echo",
            "flint", "grove", "hollow", "indigo", "jade", "kelp", "lumen", "marble",
            "nettle", "onyx", "petal", "quiver", "ridge", "stone", "tide", "valley",
            "wheat", "yew", "zodiac", "amber", "bloom", "cliff", "drift", "ember",
            "fjord", "gully", "heath", "inlet", "jewel", "knoll", "lagoon", "mist",
            "nook", "oasis", "plain", "quill", "rune", "slate", "tor", "vale",
            "wisp", "yarn", "zest", "arid", "bog", "crag", "dell", "eave",
            "fen", "glen", "hill", "isle", "jot", "knob", "loam", "moor",
            "nest", "oak", "pond", "quay", "rill", "sand", "tarn", "vale",
            "wood", "yoke", "zone", "ash", "bay", "cove", "dune", "edge",
            "fir", "gap", "hut", "ice", "jet", "key", "log", "mud",
            "nut", "ore", "pit", "rag", "sap", "tin", "urn", "vat",
            "wax", "yam", "zip", "arc", "bar", "cap", "dot", "ear",
            "fan", "gem", "hat", "ink", "jar", "kit", "lid", "map",
            "net", "oar", "pen", "rod", "sun", "tag", "urn", "van",
            "web", "yak", "zoo", "ant", "bee", "cat", "dog", "eel",
            "fox", "goat", "hen", "ibis", "jay", "kiwi", "lynx", "mole",
            "newt", "owl", "pig", "quail", "rat", "seal", "toad", "vole",
            "wolf", "yak", "zebu",
        )
    }
}
