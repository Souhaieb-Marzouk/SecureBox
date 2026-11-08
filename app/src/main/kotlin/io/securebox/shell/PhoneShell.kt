package io.securebox.shell

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.securebox.R
import io.securebox.core.ui.component.AuroraBackground
import io.securebox.core.ui.component.BannerSeverity
import io.securebox.core.ui.component.BrandMark
import io.securebox.core.ui.component.StatusBanner
import io.securebox.core.ui.theme.SecureBoxColors
import io.securebox.navigation.NavItem
import io.securebox.navigation.Routes
import io.securebox.navigation.SecureBoxNavHost
import io.securebox.navigation.navItems
import io.securebox.navigation.navigateToTopLevel

/**
 * Touch-first shell. Floating glass navigation on phones and a slim rail on tablets/foldables,
 * both over the Aurora backdrop shared with the TV shell.
 */
@Composable
fun PhoneShell(
    showIntegrityWarning: Boolean,
    expanded: Boolean = false,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    fun selected(route: String): Boolean =
        currentRoute == route ||
            (route == Routes.SETTINGS &&
                (currentRoute == Routes.HARDENING ||
                    currentRoute == Routes.THREAT_INTEL ||
                    currentRoute == Routes.HELP ||
                    currentRoute == Routes.SHIELD ||
                    currentRoute == Routes.BROWSER ||
                    currentRoute == Routes.DNS_PROTECTION))

    val content: @Composable () -> Unit = {
        Column(modifier = Modifier.fillMaxSize()) {
            if (showIntegrityWarning) {
                StatusBanner(
                    severity = BannerSeverity.DANGER,
                    message = stringResource(R.string.integrity_warning),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
            SecureBoxNavHost(navController = navController, modifier = Modifier.fillMaxSize())
        }
    }

    AuroraBackground {
        if (expanded) {
            Row(modifier = Modifier.fillMaxSize()) {
                Surface(
                    modifier = Modifier.width(232.dp).fillMaxHeight(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                        ) {
                            BrandMark(size = 42.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                        }
                        navItems.forEach { item ->
                            RailItem(
                                item = item,
                                selected = selected(item.route),
                                onClick = { navController.navigateToTopLevel(item.route) },
                            )
                        }
                    }
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) { content() }
            }
        } else {
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    AuroraBottomBar(
                        selected = ::selected,
                        onNavigate = { navController.navigateToTopLevel(it) },
                    )
                },
            ) { padding ->
                Column(modifier = Modifier.padding(padding).fillMaxSize()) { content() }
            }
        }
    }
}

@Composable
private fun AuroraBottomBar(
    selected: (String) -> Boolean,
    onNavigate: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
        shadowElevation = 18.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            navItems.forEach { item ->
                val isSelected = selected(item.route)
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .selectable(
                                selected = isSelected,
                                role = Role.Tab,
                                onClick = { onNavigate(item.route) },
                            ).padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(34.dp)
                                .background(
                                    if (isSelected) {
                                        Brush.linearGradient(
                                            listOf(SecureBoxColors.AuroraTeal, SecureBoxColors.AuroraCyan),
                                        )
                                    } else {
                                        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                    },
                                    CircleShape,
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = if (isSelected) SecureBoxColors.Ink950 else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = stringResource(item.labelRes),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun RailItem(
    item: NavItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color =
            if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            } else {
                Color.Transparent
            },
        border =
            if (selected) {
                BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
            } else {
                null
            },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = stringResource(item.labelRes),
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
