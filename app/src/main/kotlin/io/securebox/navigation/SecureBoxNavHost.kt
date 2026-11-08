package io.securebox.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.securebox.feature.apps.AppsRoute
import io.securebox.feature.dashboard.DashboardRoute
import io.securebox.feature.network.NetworkRoute
import io.securebox.feature.settings.BrowserRoute
import io.securebox.feature.settings.DnsProtectionRoute
import io.securebox.feature.settings.HelpScreen
import io.securebox.feature.settings.PrivacyScreen
import io.securebox.feature.settings.SettingsRoute
import io.securebox.feature.settings.ShieldRoute
import io.securebox.feature.settings.ShieldStatsRoute
import io.securebox.feature.threatintel.ThreatIntelRoute
import io.securebox.feature.threats.HardeningRoute
import io.securebox.feature.threats.ThreatsRoute

/** One navigation graph, used unchanged by both the phone and TV shells. */
@Composable
fun SecureBoxNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD,
        modifier = modifier,
    ) {
        composable(Routes.DASHBOARD) {
            DashboardRoute(
                onOpenNetwork = { navController.navigateToTopLevel(Routes.NETWORK) },
                onOpenThreats = { navController.navigateToTopLevel(Routes.THREATS) },
                onOpenSettings = { navController.navigateToTopLevel(Routes.SETTINGS) },
                onOpenApps = { navController.navigateToTopLevel(Routes.APPS) },
                onOpenHelp = { navController.navigate(Routes.HELP) },
                onOpenShield = { navController.navigate(Routes.SHIELD) },
            )
        }
        composable(Routes.NETWORK) { NetworkRoute() }
        composable(Routes.APPS) { AppsRoute() }
        composable(Routes.THREATS) {
            ThreatsRoute(onOpenHardening = { navController.navigate(Routes.HARDENING) })
        }
        composable(Routes.HARDENING) {
            HardeningRoute(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsRoute(
                onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
                onOpenThreatIntel = { navController.navigate(Routes.THREAT_INTEL) },
                onOpenHelp = { navController.navigate(Routes.HELP) },
                onOpenShield = { navController.navigate(Routes.SHIELD) },
            )
        }
        composable(Routes.SHIELD) {
            ShieldRoute(
                onBack = { navController.popBackStack() },
                onOpenBrowser = { navController.navigate(Routes.BROWSER) },
                onOpenDns = { navController.navigate(Routes.DNS_PROTECTION) },
                onOpenStats = { navController.navigate(Routes.SHIELD_STATS) },
            )
        }
        composable(Routes.SHIELD_STATS) {
            ShieldStatsRoute(onBack = { navController.popBackStack() })
        }
        composable(Routes.BROWSER) {
            BrowserRoute(onBack = { navController.popBackStack() })
        }
        composable(Routes.DNS_PROTECTION) {
            DnsProtectionRoute(onBack = { navController.popBackStack() })
        }
        composable(Routes.HELP) {
            HelpScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.THREAT_INTEL) {
            ThreatIntelRoute(onBack = { navController.popBackStack() })
        }
        composable(Routes.PRIVACY) {
            PrivacyScreen(onBack = { navController.popBackStack() })
        }
    }
}

/** Navigates between top-level tabs without stacking duplicates. */
fun NavHostController.navigateToTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
