package io.securebox.core.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Aurora palette. Deep ink surfaces, an electric teal primary and a violet secondary keep the app
 * calm at night while giving actions and highlights a modern, high-contrast identity. Amber and
 * coral are reserved for warnings and danger.
 */
object SecureBoxColors {
    // Ink surfaces.
    val Ink950 = Color(0xFF05070F)
    val Ink900 = Color(0xFF0A1020)
    val Ink850 = Color(0xFF0D1428)
    val Ink800 = Color(0xFF111A31)
    val Ink700 = Color(0xFF1A2745)
    val Ink600 = Color(0xFF2A3A5C)

    // Accents.
    val AuroraTeal = Color(0xFF35F2D0)
    val AuroraCyan = Color(0xFF22C7E8)
    val AuroraViolet = Color(0xFF8B7CFF)
    val AuroraPink = Color(0xFFFF6FD8)
    val AuroraAmber = Color(0xFFFFC46B)
    val AuroraCoral = Color(0xFFFF6B81)
    val DeepTeal = Color(0xFF0B3B3A)

    // Text.
    val TextPrimary = Color(0xFFF2F7FF)
    val TextSecondary = Color(0xFFA7B6D4)
    val TextMuted = Color(0xFF7486A8)

    // Light scheme.
    val LightBackground = Color(0xFFF5F8FF)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceVariant = Color(0xFFE8EEFA)
    val LightText = Color(0xFF0A1020)
    val LightTextSecondary = Color(0xFF44526B)
    val LightTeal = Color(0xFF00796B)
    val LightViolet = Color(0xFF5B4BD6)
    val LightAmber = Color(0xFF8A5A00)
    val LightRed = Color(0xFFB3261E)

    // Legacy aliases used by existing components.
    val Navy950 = Ink950
    val Navy900 = Ink900
    val Navy800 = Ink800
    val Navy700 = Ink700
    val Navy600 = Ink600
    val Teal400 = AuroraTeal
    val Teal300 = Color(0xFF7CFBE3)
    val Teal900 = Color(0xFF00332D)
    val Amber400 = AuroraAmber
    val Amber900 = Color(0xFF3D2A00)
    val Red400 = AuroraCoral
    val Red900 = Color(0xFF3A1212)

    val AuroraGradient: Brush
        get() = Brush.linearGradient(listOf(AuroraTeal, AuroraCyan, AuroraViolet))

    val CardGradient: Brush
        get() = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.06f), Color.Transparent))
}
