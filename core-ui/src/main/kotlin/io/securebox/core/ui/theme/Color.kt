package io.securebox.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * SecureBox palette. Deep navy surfaces keep the app calm at night on a TV; teal is the single
 * accent used for "safe / primary action"; amber and red are reserved for warnings and danger.
 * Every foreground/background pair used in the UI meets WCAG AA (>= 4.5:1 for body text).
 */
object SecureBoxColors {
    val Navy950 = Color(0xFF070F18)
    val Navy900 = Color(0xFF0B1622)
    val Navy800 = Color(0xFF12202F)
    val Navy700 = Color(0xFF1B2C3F)
    val Navy600 = Color(0xFF2A4055)

    val Teal400 = Color(0xFF00BFA5)
    val Teal300 = Color(0xFF3CD9C2)
    val Teal900 = Color(0xFF00332D)

    val Amber400 = Color(0xFFFFB300)
    val Amber900 = Color(0xFF3D2A00)

    val Red400 = Color(0xFFFF6B6B)
    val Red900 = Color(0xFF3A1212)

    val TextPrimary = Color(0xFFE8EEF5)
    val TextSecondary = Color(0xFFA9BACB)

    val LightBackground = Color(0xFFF4F7FA)
    val LightSurface = Color(0xFFFFFFFF)
    val LightText = Color(0xFF0B1622)
    val LightTextSecondary = Color(0xFF44586B)
    val LightTeal = Color(0xFF00695C)
    val LightAmber = Color(0xFF8A5A00)
    val LightRed = Color(0xFFB3261E)
}
