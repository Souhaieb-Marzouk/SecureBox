package io.securebox.shell

import android.app.UiModeManager
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.securebox.R
import io.securebox.core.tv.ui.component.TvNavEntry
import io.securebox.core.tv.ui.component.TvScaffold
import io.securebox.core.ui.component.BannerSeverity
import io.securebox.core.ui.component.StatusBanner
import io.securebox.navigation.Routes
import io.securebox.navigation.SecureBoxNavHost
import io.securebox.navigation.navItems
import io.securebox.navigation.navigateToTopLevel

/**
 * D-pad-first shell for Android TV boxes: persistent sidebar, large focus rings and 10-foot text.
 *
 * The mode can be overridden on a phone for testing. In that case the sidebar collapses to an icon
 * rail and an explicit "Use phone layout" action is shown, so the override can never trap the user.
 */
@Composable
fun TvShell(
    showIntegrityWarning: Boolean,
    onUsePhoneLayout: () -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.DASHBOARD
    val context = LocalContext.current
    val isTelevision =
        remember(context) {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION) ||
                context.getSystemService(UiModeManager::class.java)?.currentModeType ==
                Configuration.UI_MODE_TYPE_TELEVISION
        }

    val entries =
        navItems.map { item ->
            TvNavEntry(
                route = item.route,
                label = stringResource(item.labelRes),
                icon = item.icon,
            )
        }

    TvScaffold(
        appName = stringResource(R.string.app_name),
        entries = entries,
        selectedRoute = if (currentRoute == Routes.PRIVACY) Routes.SETTINGS else currentRoute,
        onNavigate = { route -> navController.navigateToTopLevel(route) },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (!isTelevision) {
                TextButton(
                    onClick = onUsePhoneLayout,
                    modifier = Modifier.padding(bottom = 8.dp),
                ) {
                    Text(text = stringResource(R.string.tv_use_phone_layout))
                }
            }
            if (showIntegrityWarning) {
                StatusBanner(
                    severity = BannerSeverity.DANGER,
                    message = stringResource(R.string.integrity_warning),
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
            SecureBoxNavHost(navController = navController, modifier = Modifier.fillMaxSize())
        }
    }
}
