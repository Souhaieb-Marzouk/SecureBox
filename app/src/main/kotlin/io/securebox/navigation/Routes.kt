package io.securebox.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import io.securebox.R
import io.securebox.core.ui.icon.SecureBoxIcons

/** Route names shared by the phone and TV shells. */
object Routes {
    const val DASHBOARD: String = "dashboard"
    const val NETWORK: String = "network"
    const val APPS: String = "apps"
    const val THREATS: String = "threats"
    const val SETTINGS: String = "settings"
    const val PRIVACY: String = "privacy"
    const val HARDENING: String = "hardening"
    const val THREAT_INTEL: String = "threat_intel"
}

data class NavItem(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
)

val navItems: List<NavItem> =
    listOf(
        NavItem(Routes.DASHBOARD, R.string.nav_dashboard, SecureBoxIcons.Shield),
        NavItem(Routes.NETWORK, R.string.nav_network, SecureBoxIcons.Network),
        NavItem(Routes.APPS, R.string.nav_apps, SecureBoxIcons.Apps),
        NavItem(Routes.THREATS, R.string.nav_threats, SecureBoxIcons.Alert),
        NavItem(Routes.SETTINGS, R.string.nav_settings, SecureBoxIcons.Settings),
    )
