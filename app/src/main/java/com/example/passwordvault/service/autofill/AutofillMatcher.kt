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
 * An entry is a candidate for [packageName] when the package appears in the
 * entry's URL (e.g. host `mybank.com` vs package `com.mybank.app`), or when the
 * visible [appLabel] appears in the entry's title. URL matches rank first.
 */
object AutofillMatcher {

    fun findMatches(
        candidates: List<AutofillCandidate>,
        packageName: String,
        appLabel: String,
    ): List<AutofillCandidate> {
        if (packageName.isBlank()) return emptyList()
        val pkg = packageName.lowercase()
        // Second-level token of the package (e.g. "mybank" in "com.mybank.app").
        val token = pkg.split('.').getOrNull(1).orEmpty()
        val label = appLabel.lowercase().trim()

        data class Scored(val candidate: AutofillCandidate, val score: Int)
        return candidates.mapNotNull { candidate ->
            val host = hostOf(candidate.url).lowercase()
            val title = candidate.title.lowercase()
            val score = when {
                host.isNotEmpty() && (host.contains(pkg) || pkg.contains(host)) -> 3
                token.length >= 3 && host.contains(token) -> 2
                label.length >= 3 && title.contains(label) -> 1
                else -> return@mapNotNull null
            }
            Scored(candidate, score)
        }.sortedByDescending { it.score }.map { it.candidate }
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
