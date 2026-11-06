package io.securebox.shell

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.securebox.R
import io.securebox.core.ui.component.BannerSeverity
import io.securebox.core.ui.component.StatusBanner
import io.securebox.navigation.Routes
import io.securebox.navigation.SecureBoxNavHost
import io.securebox.navigation.navItems
import io.securebox.navigation.navigateToTopLevel

/**
 * Touch-first shell: bottom navigation with five tabs, same screens as the TV shell.
 */
@Composable
fun PhoneShell(showIntegrityWarning: Boolean) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                navItems.forEach { item ->
                    NavigationBarItem(
                        selected =
                            currentRoute == item.route ||
                                (item.route == Routes.SETTINGS && currentRoute == Routes.PRIVACY),
                        onClick = { navController.navigateToTopLevel(item.route) },
                        icon = { Icon(imageVector = item.icon, contentDescription = null) },
                        label = { Text(text = stringResource(item.labelRes)) },
                    )
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (showIntegrityWarning) {
                StatusBanner(
                    severity = BannerSeverity.DANGER,
                    message = stringResource(R.string.integrity_warning),
                    modifier = Modifier.padding(16.dp),
                )
            }
            SecureBoxNavHost(navController = navController, modifier = Modifier.fillMaxSize())
        }
    }
}
