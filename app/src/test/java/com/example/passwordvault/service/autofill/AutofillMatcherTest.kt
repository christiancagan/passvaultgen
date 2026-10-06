package com.example.passwordvault.service.autofill

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AutofillMatcherTest {

    private val candidates = listOf(
        AutofillCandidate("1", "MyBank login", "https://mybank.com/login"),
        AutofillCandidate("2", "Work email", "https://mail.work.example"),
        AutofillCandidate("3", "Banking notes", ""),
    )

    @Test
    fun `package matching host wins`() {
        val matches = AutofillMatcher.findMatches(candidates, "com.mybank.app", "MyBank")
        // Token "mybank" matches host mybank.com; unrelated entries stay hidden.
        assertThat(matches.map { it.id }).containsExactly("1")
    }

    @Test
    fun `package token matches host`() {
        val matches = AutofillMatcher.findMatches(candidates, "com.example.mailapp", "Mail")
        // "mail" token matches host mail.work.example; label "Mail" matches title "Work email".
        assertThat(matches.map { it.id }).contains("2")
    }

    @Test
    fun `no match returns empty`() {
        assertThat(
            AutofillMatcher.findMatches(candidates, "com.unknown.game", "Unknown Game"),
        ).isEmpty()
    }

    @Test
    fun `blank package returns empty`() {
        assertThat(AutofillMatcher.findMatches(candidates, "", "MyBank")).isEmpty()
    }

    @Test
    fun `host parsing strips scheme www port and path`() {
        assertThat(AutofillMatcher.hostOf("https://www.mybank.com:443/login")).isEqualTo("mybank.com")
        assertThat(AutofillMatcher.hostOf("mybank.com/login")).isEqualTo("mybank.com")
        assertThat(AutofillMatcher.hostOf("")).isEqualTo("")
    }

    @Test
    fun `short tokens and labels do not match`() {
        // Token "go" (from com.go.app) and 2-char labels must not produce hits.
        val matches = AutofillMatcher.findMatches(
            listOf(AutofillCandidate("9", "Gorgeous stuff", "https://gorgeous.example")),
            "com.go.app",
            "Go",
        )
        assertThat(matches).isEmpty()
    }
}
