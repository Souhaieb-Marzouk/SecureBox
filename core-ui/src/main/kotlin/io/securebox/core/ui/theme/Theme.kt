package io.securebox.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val DarkScheme =
    darkColorScheme(
        primary = SecureBoxColors.Teal400,
        onPrimary = SecureBoxColors.Teal900,
        primaryContainer = SecureBoxColors.Navy700,
        onPrimaryContainer = SecureBoxColors.Teal300,
        secondary = SecureBoxColors.Teal300,
        onSecondary = SecureBoxColors.Teal900,
        tertiary = SecureBoxColors.Amber400,
        onTertiary = SecureBoxColors.Amber900,
        error = SecureBoxColors.Red400,
        onError = SecureBoxColors.Red900,
        background = SecureBoxColors.Navy950,
        onBackground = SecureBoxColors.TextPrimary,
        surface = SecureBoxColors.Navy900,
        onSurface = SecureBoxColors.TextPrimary,
        surfaceVariant = SecureBoxColors.Navy800,
        onSurfaceVariant = SecureBoxColors.TextSecondary,
        outline = SecureBoxColors.Navy600,
        outlineVariant = SecureBoxColors.Navy700,
    )

private val LightScheme =
    lightColorScheme(
        primary = SecureBoxColors.LightTeal,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFCDEFE9),
        onPrimaryContainer = Color(0xFF00201C),
        secondary = Color(0xFF00695C),
        onSecondary = Color.White,
        tertiary = SecureBoxColors.LightAmber,
        onTertiary = Color.White,
        error = SecureBoxColors.LightRed,
        onError = Color.White,
        background = SecureBoxColors.LightBackground,
        onBackground = SecureBoxColors.LightText,
        surface = SecureBoxColors.LightSurface,
        onSurface = SecureBoxColors.LightText,
        surfaceVariant = Color(0xFFE4EBF1),
        onSurfaceVariant = SecureBoxColors.LightTextSecondary,
        outline = Color(0xFF7A8B9B),
        outlineVariant = Color(0xFFC3CFD9),
    )

@Composable
fun SecureBoxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    typography: Typography = SecureBoxTypography,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = typography,
            content = content,
        )
    }
}
