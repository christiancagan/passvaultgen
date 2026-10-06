package com.example.passwordvault.security.random

import java.security.SecureRandom

/**
 * Abstraction over the platform CSPRNG so it can be faked in tests.
 *
 * The single source of cryptographically secure randomness for the entire
 * application: passwords, nonces, salts, the vault DEK, and recovery material.
 * Never use [java.util.Random], [Math.random], timestamps, or device identifiers
 * for any security-sensitive value.
 */
interface SecureRandomProvider {
    /** Returns [size] cryptographically secure random bytes. */
    fun nextBytes(size: Int): ByteArray

    /** Returns a cryptographically secure random int in `[0, bound)`. */
    fun nextInt(bound: Int): Int
}

class SecureRandomProviderImpl : SecureRandomProvider {

    private val secureRandom = SecureRandom()

    override fun nextBytes(size: Int): ByteArray {
        require(size > 0) { "size must be positive" }
        val bytes = ByteArray(size)
        secureRandom.nextBytes(bytes)
        return bytes
    }

    override fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound must be positive" }
        return secureRandom.nextInt(bound)
    }
}
