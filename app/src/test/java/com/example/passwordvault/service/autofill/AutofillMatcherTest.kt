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

    @Test
    fun `web domain exact match ranks first`() {
        val web = listOf(
            AutofillCandidate("1", "MyBank login", "https://mybank.com/login"),
            AutofillCandidate("2", "Other bank", "https://otherbank.com"),
        )
        val matches = AutofillMatcher.findMatches(
            web, "com.android.chrome", "Chrome", webDomain = "mybank.com",
        )
        assertThat(matches.map { it.id }).containsExactly("1")
    }

    @Test
    fun `web domain matches subdomains both ways`() {
        val web = listOf(
            AutofillCandidate("1", "GitHub", "https://login.github.com"),
            AutofillCandidate("2", "Gist", "https://github.com"),
        )
        // Entry host is a subdomain of the page.
        assertThat(
            AutofillMatcher.findMatches(web, "com.android.chrome", "Chrome", webDomain = "github.com")
                .map { it.id },
        ).contains("1")
        // Page is a subdomain of the entry host.
        assertThat(
            AutofillMatcher.findMatches(web, "com.android.chrome", "Chrome", webDomain = "gist.github.com")
                .map { it.id },
        ).contains("2")
    }

    @Test
    fun `web domain strips www and port`() {
        val matches = AutofillMatcher.findMatches(
            listOf(AutofillCandidate("1", "MyBank login", "mybank.com")),
            "com.android.chrome", "Chrome", webDomain = "www.mybank.com",
        )
        assertThat(matches.map { it.id }).containsExactly("1")
    }

    @Test
    fun `web domain non-match falls back to package matching`() {
        val matches = AutofillMatcher.findMatches(
            candidates, "com.mybank.app", "MyBank", webDomain = "unrelated.example",
        )
        assertThat(matches.map { it.id }).containsExactly("1")
    }

    @Test
    fun `blank web domain behaves like before`() {
        val matches = AutofillMatcher.findMatches(
            candidates, "com.mybank.app", "MyBank", webDomain = "",
        )
        assertThat(matches.map { it.id }).containsExactly("1")
    }

    @Test
    fun `hostsMatch needs a dot boundary`() {
        assertThat(AutofillMatcher.hostsMatch("evilexample.com", "example.com")).isFalse()
        assertThat(AutofillMatcher.hostsMatch("login.example.com", "example.com")).isTrue()
        assertThat(AutofillMatcher.hostsMatch("example.com", "m.example.com")).isTrue()
        assertThat(AutofillMatcher.hostsMatch("", "example.com")).isFalse()
    }
}
