package com.example.passwordvault.security.crypto

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters

/**
 * Argon2id parameters. Stored with the vault so they can be upgraded later.
 *
 * @param memoryKib   memory cost `m` in KiB (memory-hardness).
 * @param iterations  time cost `t`.
 * @param parallelism lanes `p`.
 * @param outputLength derived key length in bytes (32 = 256-bit KEK).
 */
data class Argon2Params(
    val memoryKib: Int,
    val iterations: Int,
    val parallelism: Int,
    val outputLength: Int = 32,
) {
    init {
        require(memoryKib >= 8) { "memoryKib must be >= 8" }
        require(iterations >= 1) { "iterations must be >= 1" }
        require(parallelism >= 1) { "parallelism must be >= 1" }
        require(outputLength >= 16) { "outputLength must be >= 16" }
    }

    companion object {
        /** Conservative defaults for a low-end device (~64 MiB, ~0.5s). */
        val DEFAULT = Argon2Params(memoryKib = 64 * 1024, iterations = 3, parallelism = 1)

        /**
         * Parameters scaled to the device's capability.
         *
         * Modern guidance (2026) is 128–256 MiB with t=3–4 on capable
         * hardware. We target ~1/4 of the runtime's max heap, capped at
         * 256 MiB, never below [DEFAULT], and use up to 4 lanes.
         *
         * Callers pass `Runtime.getRuntime().maxMemory()` and
         * `Runtime.getRuntime().availableProcessors()`; kept as parameters
         * so it is unit-testable.
         */
        fun recommended(
            maxMemoryBytes: Long,
            availableCores: Int,
        ): Argon2Params {
            val byMemory = (maxMemoryBytes / 4 / 1024).toInt()
            val memoryKib = byMemory.coerceIn(DEFAULT.memoryKib, 256 * 1024)
            val parallelism = availableCores.coerceIn(1, 4)
            val iterations = if (memoryKib >= 128 * 1024) 3 else DEFAULT.iterations
            return Argon2Params(memoryKib, iterations, parallelism)
        }
    }
}

/**
 * Argon2id key derivation using Bouncy Castle's audited implementation.
 *
 * Argon2id is memory-hard and resistant to both GPU and side-channel attacks,
 * making it the correct choice for deriving a KEK from a user-chosen master
 * password. We never use MD5, SHA-1, or a bare SHA-256/512 hash for this.
 */
class Argon2idKdf {

    /**
     * Derives a key from [password] and [salt] using [params].
     *
     * The caller is responsible for zeroing [password] and the returned key
     * after use (best-effort on the JVM).
     */
    fun derive(password: CharArray, salt: ByteArray, params: Argon2Params): ByteArray {
        require(salt.size >= 8) { "salt must be at least 8 bytes" }

        val generator = Argon2BytesGenerator()
        val builder = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withSalt(salt)
            .withParallelism(params.parallelism)
            .withMemoryAsKB(params.memoryKib)
            .withIterations(params.iterations)

        generator.init(builder.build())

        val output = ByteArray(params.outputLength)
        generator.generateBytes(password, output)
        return output
    }
}
