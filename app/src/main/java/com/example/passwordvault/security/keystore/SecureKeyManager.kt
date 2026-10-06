package com.example.passwordvault.security.keystore

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import java.security.KeyFactory
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Manages device-protected key material in the Android Keystore.
 *
 * The Keystore never stores the vault DEK directly. It stores a device-bound
 * AES key used to wrap the DEK for the biometric unlock path. This keeps the
 * DEK recoverable only on this device, with hardware-backed protection where
 * available (StrongBox preferred, with graceful fallback).
 */
class SecureKeyManager {

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    /**
     * Returns the biometric-bound AES key for [alias], creating it if absent.
     *
     * Configuration:
     *  - AES-256-GCM, no padding.
     *  - [KeyGenParameterSpec.setUserAuthenticationRequired] = true: the key is
     *    unusable without a recent user authentication (biometric or credential).
     *  - [KeyGenParameterSpec.setInvalidatedByBiometricEnrollment] = true: the key
     *    is permanently invalidated if the user adds/removes a biometric, so a
     *    new enrollment cannot silently unlock the vault.
     */
    fun getOrCreateBiometricKey(alias: String): SecretKey {
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true)
                .build(),
        )
        return generator.generateKey()
    }

    /** Returns true if the key for [alias] is backed by secure hardware. */
    fun isHardwareBacked(alias: String): Boolean {
        val entry = keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry ?: return false
        return try {
            val factory = KeyFactory.getInstance(entry.secretKey.algorithm, ANDROID_KEYSTORE)
            val keyInfo = factory.getKeySpec(entry.secretKey, KeyInfo::class.java)
            keyInfo.isInsideSecureHardware
        } catch (e: Exception) {
            false
        }
    }

    /** Returns true if the key for [alias] exists. */
    fun contains(alias: String): Boolean = keyStore.containsAlias(alias)

    /** Deletes the key for [alias] (used when disabling biometric unlock). */
    fun delete(alias: String) {
        if (keyStore.containsAlias(alias)) keyStore.deleteEntry(alias)
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val BIOMETRIC_KEY_ALIAS = "passvaultgen_biometric_dek_wrap"
    }
}
