package io.securebox

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import io.securebox.core.domain.model.DeviceMode
import io.securebox.core.domain.model.ThemeMode
import io.securebox.core.tv.ui.theme.TvSecureBoxTheme
import io.securebox.core.ui.theme.SecureBoxTheme
import io.securebox.feature.onboarding.OnboardingRoute
import io.securebox.shell.PhoneShell
import io.securebox.shell.TvShell

/**
 * Chooses the whole visual identity at runtime: onboarding first, then the shell that matches the
 * stored device mode. Both shells share the same navigation graph and screens.
 */
@Composable
fun SecureBoxRoot(state: MainUiState) {
    val darkTheme =
        when (state.settings.themeMode) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
        }

    val showIntegrityWarning = !state.signatureOk

    when {
        !state.settings.onboardingCompleted -> {
            SecureBoxTheme(darkTheme = darkTheme) {
                OnboardingRoute()
            }
        }

        state.settings.deviceMode == DeviceMode.TV -> {
            TvSecureBoxTheme(darkTheme = darkTheme) {
                TvShell(showIntegrityWarning = showIntegrityWarning)
            }
        }

        else -> {
            SecureBoxTheme(darkTheme = darkTheme) {
                PhoneShell(showIntegrityWarning = showIntegrityWarning)
            }
        }
    }
}
