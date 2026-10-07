package com.example.passwordvault.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Secure clipboard copy shared by every screen: the clip label is the app
 * name (Android 13+ shows it in the system overlay, so it must never say
 * "password"), and the clip auto-clears after the configured timeout.
 */
@Composable
fun rememberSecureClipboard(): SecureClipboard {
    val scope = rememberCoroutineScope()
    return remember(scope) { SecureClipboard(scope) }
}

class SecureClipboard(private val scope: CoroutineScope) {

    fun copy(context: Context, value: String, timeoutSeconds: Int) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("PassVaultGen", value))
        scope.launch {
            delay(timeoutSeconds.coerceIn(5, 300) * 1000L)
            if (clipboard.primaryClip?.getItemAt(0)?.text?.toString() == value) {
                clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        }
    }
}
