package com.example.passwordvault

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device UI tests (emulator or real device).
 *
 * Espresso-Compose drives the real app: navigation wiring, the Argon2id +
 * Room + DataStore stack, and the brute-force lockout UI. The host wipes app
 * data (`adb shell pm clear`) before each run, so every test starts at Welcome.
 * Text input uses semantics actions (exact strings, no keyboard injection).
 */
@RunWith(AndroidJUnit4::class)
class VaultFlowTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val password = "InstrumentedTest123"

    @Test
    fun createVault_reachesHome() {
        createVault(password)
        waitForText("Vault", 180_000)
        compose.onNodeWithText("No credentials yet.", substring = true).assertExists()
    }

    @Test
    fun addEntry_appearsInList() {
        createVault(password)
        waitForText("Vault", 180_000)

        compose.onNodeWithContentDescription("Add credential").performClick()
        waitForText("Add Credential", 10_000)

        setTextField(0, "Test Bank")
        setTextField(1, "Finance")
        setTextField(3, "user1")
        setTextField(4, "s3cret-pw")
        compose.onNodeWithText("Save").performClick()

        waitForText("Test Bank", 15_000)
    }

    @Test
    fun lockThenUnlock_returnsToHome() {
        createVault(password)
        waitForText("Vault", 180_000)

        compose.onNodeWithContentDescription("Lock vault").performClick()
        waitForText("Unlock Vault", 10_000)

        unlockWith(password)
        waitForText("Vault", 120_000)
    }

    @Test
    fun wrongPasswords_triggerLockoutMessage() {
        createVault(password)
        waitForText("Vault", 180_000)

        compose.onNodeWithContentDescription("Lock vault").performClick()
        waitForText("Unlock Vault", 10_000)

        repeat(6) {
            unlockWith("wrong-password")
            waitForUnlockIdle()
        }
        waitForText("Too many failed attempts", substring = true, timeoutMs = 300_000)
    }

    // --- helpers -----------------------------------------------------------

    private fun createVault(pw: String) {
        compose.onNodeWithText("Create a new vault").performClick()
        waitForText("Create Master Password", 10_000)
        setTextField(0, pw)
        setTextField(1, pw)
        compose.onNodeWithText("Create vault").performClick()
    }

    private fun unlockWith(pw: String) {
        setTextField(0, pw, clearFirst = true)
        compose.onNodeWithText("Unlock", substring = false).performClick()
    }

    /** Nth editable field on screen (composition order). */
    private fun setTextField(index: Int, text: String, clearFirst: Boolean = false) {
        val node = compose.onAllNodes(hasSetTextAction())[index]
        if (clearFirst) node.performTextClearance()
        node.performTextInput(text)
    }

    private fun waitForText(text: String, timeoutMs: Long, substring: Boolean = false) {
        compose.waitUntil(timeoutMs) {
            compose.onAllNodesWithText(text, substring = substring)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * An unlock attempt runs Argon2id off the main thread, so Espresso idling
     * does not cover it. The Unlock button shows a spinner while busy, so
     * waiting for it to leave and come back brackets exactly one attempt.
     */
    private fun waitForUnlockIdle() {
        try {
            compose.waitUntil(20_000) {
                compose.onAllNodesWithText("Unlock", substring = false)
                    .fetchSemanticsNodes().isEmpty()
            }
        } catch (_: Exception) {
            // Button never left (attempt finished instantly); fall through.
        }
        waitForText("Unlock", 180_000)
    }
}
