package com.example.passwordvault.util

import android.util.Log
import com.example.passwordvault.BuildConfig

/**
 * The only logging entry point in the application.
 *
 * Security policy:
 *  - Logging is disabled in release builds ([BuildConfig.DEBUG] gates every call).
 *  - A redaction pass strips obvious secret material (key=value pairs whose key
 *    looks sensitive) as a last line of defense.
 *  - Callers must NEVER pass secrets (master password, vault passwords, keys,
 *    tokens, recovery codes, clipboard contents, notes) to any method here.
 *
 * The redaction is best-effort and is NOT a substitute for never logging secrets
 * in the first place.
 */
object SecureLogger {

    private const val TAG = "PassVaultGen"

    private val enabled: Boolean = BuildConfig.DEBUG

    private val sensitiveKeyPattern = Regex(
        "(?i)(password|passwd|pwd|secret|token|key|nonce|salt|dek|kek|recovery|clipboard|note)\\s*[=:]\\s*[^\\s,;]+"
    )

    fun d(message: String) {
        if (enabled) Log.d(TAG, redact(message))
    }

    fun i(message: String) {
        if (enabled) Log.i(TAG, redact(message))
    }

    fun w(message: String) {
        if (enabled) Log.w(TAG, redact(message))
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (enabled) Log.e(TAG, redact(message), throwable)
    }

    private fun redact(message: String): String =
        sensitiveKeyPattern.replace(message, "$1=[REDACTED]")
}
