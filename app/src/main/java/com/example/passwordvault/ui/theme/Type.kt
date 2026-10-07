package com.example.passwordvault.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

private val AppFont = FontFamily.SansSerif

val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = AppFont, fontWeight = FontWeight.Bold,
        fontSize = 34.sp, lineHeight = 42.sp, letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = AppFont, fontWeight = FontWeight.Bold,
        fontSize = 27.sp, lineHeight = 34.sp, letterSpacing = (-0.3).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = AppFont, fontWeight = FontWeight.SemiBold,
        fontSize = 23.sp, lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = AppFont, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = 0.1.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = AppFont, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = AppFont, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = AppFont, fontSize = 16.sp, lineHeight = 23.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = AppFont, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = AppFont, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = AppFont, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = AppFont, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = AppFont, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.4.sp,
    ),
)

/**
 * Monospace style for password output. Sizes are sp so they scale with the
 * user's font setting.
 */
@Composable
fun passwordStyle(size: TextUnit = 15.sp): TextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = if (size >= 20.sp) FontWeight.SemiBold else FontWeight.Medium,
    fontSize = size,
    lineHeight = if (size >= 20.sp) 30.sp else 22.sp,
    letterSpacing = 1.sp,
    color = MaterialTheme.colorScheme.onSurface,
)
