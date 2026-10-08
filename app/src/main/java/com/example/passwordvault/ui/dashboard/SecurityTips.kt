package com.example.passwordvault.ui.dashboard

/**
 * Built-in password-hygiene tips for the dashboard. The tip rotates daily:
 * the epoch day indexes the list, so every user sees the same tip all day
 * and a new one tomorrow. Pure logic, unit-tested, no Android APIs.
 */
object SecurityTips {

    val tips: List<String> = listOf(
        "Use a unique password for every account — one breach should never unlock another.",
        "Prefer passphrases: 4–5 random words beat short, complex passwords.",
        "Enable two-factor authentication wherever it is offered.",
        "Never reuse your master password anywhere else.",
        "Rotate passwords on sensitive accounts (email, banking) every few months.",
        "If two entries share a password, change one of them today.",
        "Lock your vault whenever you step away from your device.",
        "Check the website address before typing credentials — phishing copies look real.",
        "Generated passwords of 16+ characters resist brute-force attacks.",
        "Refresh passwords older than 180 days from the dashboard's Old tile.",
        "Never share passwords over chat, email, or SMS.",
        "Your master password is the only key — make it long and memorable only to you.",
    )

    /** Tip for a given epoch day; wraps around and tolerates negative values. */
    fun forEpochDay(epochDay: Long): String {
        val index = ((epochDay % tips.size) + tips.size) % tips.size
        return tips[index.toInt()]
    }
}
