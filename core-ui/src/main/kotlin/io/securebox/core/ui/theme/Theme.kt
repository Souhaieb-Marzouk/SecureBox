package io.securebox.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val DarkScheme =
    darkColorScheme(
        primary = SecureBoxColors.AuroraTeal,
        onPrimary = SecureBoxColors.Ink950,
        primaryContainer = SecureBoxColors.DeepTeal,
        onPrimaryContainer = Color(0xFF9FFFF0),
        secondary = SecureBoxColors.AuroraViolet,
        onSecondary = SecureBoxColors.Ink950,
        secondaryContainer = Color(0xFF241B4D),
        onSecondaryContainer = Color(0xFFDCD6FF),
        tertiary = SecureBoxColors.AuroraAmber,
        onTertiary = SecureBoxColors.Ink950,
        error = SecureBoxColors.AuroraCoral,
        onError = SecureBoxColors.Ink950,
        errorContainer = Color(0xFF3A1212),
        onErrorContainer = Color(0xFFFFD7DB),
        background = SecureBoxColors.Ink950,
        onBackground = SecureBoxColors.TextPrimary,
        surface = SecureBoxColors.Ink900,
        onSurface = SecureBoxColors.TextPrimary,
        surfaceVariant = SecureBoxColors.Ink800,
        onSurfaceVariant = SecureBoxColors.TextSecondary,
        outline = SecureBoxColors.Ink600,
        outlineVariant = SecureBoxColors.Ink700,
        scrim = Color(0xCC05070F),
        inverseSurface = SecureBoxColors.TextPrimary,
        inverseOnSurface = SecureBoxColors.Ink950,
        inversePrimary = SecureBoxColors.LightTeal,
        surfaceTint = SecureBoxColors.AuroraTeal,
    )

private val LightScheme =
    lightColorScheme(
        primary = SecureBoxColors.LightTeal,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFC9F7EF),
        onPrimaryContainer = Color(0xFF00201C),
        secondary = SecureBoxColors.LightViolet,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE7E2FF),
        onSecondaryContainer = Color(0xFF1A0E5A),
        tertiary = SecureBoxColors.LightAmber,
        onTertiary = Color.White,
        error = SecureBoxColors.LightRed,
        onError = Color.White,
        background = SecureBoxColors.LightBackground,
        onBackground = SecureBoxColors.LightText,
        surface = SecureBoxColors.LightSurface,
        onSurface = SecureBoxColors.LightText,
        surfaceVariant = SecureBoxColors.LightSurfaceVariant,
        onSurfaceVariant = SecureBoxColors.LightTextSecondary,
        outline = Color(0xFF7A8B9B),
        outlineVariant = Color(0xFFC3CFD9),
        surfaceTint = SecureBoxColors.LightTeal,
    )

@Composable
fun SecureBoxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    typography: Typography = SecureBoxTypography,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkScheme else LightScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
    ) {
        // MaterialTheme does not set LocalContentColor itself. Without this, Text outside a Surface
        // falls back to black, which is unreadable on the dark Aurora backdrop.
        CompositionLocalProvider(
            LocalContentColor provides colorScheme.onBackground,
            LocalTextStyle provides typography.bodyLarge,
        ) {
            content()
        }
    }
}
