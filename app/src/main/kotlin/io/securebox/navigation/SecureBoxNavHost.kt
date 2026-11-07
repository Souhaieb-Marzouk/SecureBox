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
import io.securebox.feature.settings.PrivacyScreen
import io.securebox.feature.settings.SettingsRoute
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
            )
        }
        composable(Routes.NETWORK) { NetworkRoute() }
        composable(Routes.APPS) { AppsRoute() }
        composable(Routes.THREATS) { ThreatsRoute() }
        composable(Routes.SETTINGS) {
            SettingsRoute(onOpenPrivacy = { navController.navigate(Routes.PRIVACY) })
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
