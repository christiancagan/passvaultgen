package com.example.passwordvault.service.autofill

/** Minimal data needed to match a vault entry against a requesting app. */
data class AutofillCandidate(
    val id: String,
    val title: String,
    val url: String,
)

/**
 * Pure matching logic for the autofill service (unit-testable, no Android APIs).
 *
 * An entry is a candidate when the requesting page's [webDomain] matches the
 * entry's URL host (browser logins, e.g. `github.com` on Chrome) — these rank
 * first. Otherwise the package appears in the entry's URL (e.g. host
 * `mybank.com` vs package `com.mybank.app`), or the visible [appLabel]
 * appears in the entry's title.
 */
object AutofillMatcher {

    fun findMatches(
        candidates: List<AutofillCandidate>,
        packageName: String,
        appLabel: String,
        webDomain: String? = null,
    ): List<AutofillCandidate> {
        if (packageName.isBlank()) return emptyList()
        val pkg = packageName.lowercase()
        // Second-level token of the package (e.g. "mybank" in "com.mybank.app").
        val token = pkg.split('.').getOrNull(1).orEmpty()
        val label = appLabel.lowercase().trim()
        val domain = webDomain?.trim()?.lowercase()?.removePrefix("www.").orEmpty()

        data class Scored(val candidate: AutofillCandidate, val score: Int)
        return candidates.mapNotNull { candidate ->
            val host = hostOf(candidate.url).lowercase()
            val title = candidate.title.lowercase()
            val score = when {
                domain.isNotEmpty() && hostsMatch(host, domain) -> 4
                host.isNotEmpty() && (host.contains(pkg) || pkg.contains(host)) -> 3
                token.length >= 3 && host.contains(token) -> 2
                label.length >= 3 && title.contains(label) -> 1
                else -> return@mapNotNull null
            }
            Scored(candidate, score)
        }.sortedByDescending { it.score }.map { it.candidate }
    }

    /**
     * Host equality allowing subdomains either way (`login.example.com` vs
     * `example.com`). The `.` boundary matters: `evilexample.com` never matches
     * `example.com`.
     */
    fun hostsMatch(entryHost: String, webDomain: String): Boolean {
        if (entryHost.isEmpty() || webDomain.isEmpty()) return false
        if (entryHost == webDomain) return true
        return entryHost.endsWith(".$webDomain") || webDomain.endsWith(".$entryHost")
    }

    /** Strips scheme, port, path, and a leading `www.` — matching needs the bare host. */
    fun hostOf(url: String): String {
        var s = url.trim().lowercase()
        s = s.substringAfter("://", s)
        s = s.substringBefore('/', s)
        s = s.substringBefore(':', s)
        if (s.startsWith("www.")) s = s.removePrefix("www.")
        return s
    }
}
