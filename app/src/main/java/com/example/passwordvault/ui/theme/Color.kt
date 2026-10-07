package com.example.passwordvault.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// --- Semantic password-strength colors (scheme-independent) ----------------
val StrengthWeak = Color(0xFFE5484D)
val StrengthFair = Color(0xFFF76B15)
val StrengthGood = Color(0xFFE2A300)
val StrengthStrong = Color(0xFF30A46C)

fun strengthBarColor(score: Int): Color = when (score) {
    0, 1 -> StrengthWeak
    2 -> StrengthFair
    3 -> StrengthGood
    else -> StrengthStrong
}

/** Text-safe variant of the strength color for the active theme. */
@Composable
fun strengthTextColor(score: Int): Color {
    val dark = LocalIsDark.current
    return when (score) {
        0, 1 -> if (dark) Color(0xFFFF8A8D) else Color(0xFFB3261E)
        2 -> if (dark) Color(0xFFFFB68A) else Color(0xFF8F3B00)
        3 -> if (dark) Color(0xFFF5D060) else Color(0xFF7A5A00)
        else -> if (dark) Color(0xFF5CD69A) else Color(0xFF0F6B3C)
    }
}

// --- Brand dark palette: deep navy / charcoal, vivid teal accent -----------
val NavyBackdrop = Color(0xFF0B1220)
val BrandTealDark = Color(0xFF37D9B8)
val BrandIndigoDark = Color(0xFF7E97FF)
val BrandVioletDark = Color(0xFFC9A8FF)

// --- Brand light palette: cool paper, ink navy, darker teal accent ---------
val BrandTealLight = Color(0xFF006B55)
val BrandIndigoLight = Color(0xFF3554C8)
val BrandVioletLight = Color(0xFF6A3FB8)
