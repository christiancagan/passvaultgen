package com.example.passwordvault.security.backup

import javax.inject.Inject

/**
 * Base64 abstraction for the backup format.
 *
 * Production code uses Android's `android.util.Base64` ([AndroidBackupBase64]);
 * JVM unit tests inject a `java.util.Base64` implementation instead, which is
 * what makes the exporter→importer round trip testable without Robolectric.
 */
interface BackupBase64 {
    fun encode(bytes: ByteArray): String
    fun decode(s: String): ByteArray
}

class AndroidBackupBase64 @Inject constructor() : BackupBase64 {
    override fun encode(bytes: ByteArray): String =
        android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)

    override fun decode(s: String): ByteArray =
        android.util.Base64.decode(s, android.util.Base64.NO_WRAP)
}
