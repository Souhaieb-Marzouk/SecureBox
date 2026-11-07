package io.securebox.core.tv.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import io.securebox.core.tv.ui.theme.TvDimens
import io.securebox.core.ui.component.AuroraBackground
import io.securebox.core.ui.component.BrandMark

data class TvNavEntry(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

/**
 * Sidebar layout used by every TV screen: a translucent left rail, a cinematic Aurora backdrop and
 * generous 48dp content margins for 10-foot readability.
 */
@Composable
fun TvScaffold(
    appName: String,
    entries: List<TvNavEntry>,
    selectedRoute: String,
    onNavigate: (String) -> Unit,
    content: @Composable () -> Unit,
) {
    val firstItemFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstItemFocus.requestFocus() } }

    // Manual override can put the TV shell on a phone. A wide sidebar would swallow the screen, so
    // narrow windows collapse it to an icon rail and shrink the content padding.
    val compact = LocalConfiguration.current.screenWidthDp < 600
    val sidebarWidth = if (compact) 92.dp else TvDimens.SidebarWidth
    val screenPadding = if (compact) 12.dp else TvDimens.ScreenPadding

    AuroraBackground {
        Row(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.width(sidebarWidth).fillMaxHeight(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.52f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
            ) {
                Column(
                    modifier =
                        Modifier
                            .padding(horizontal = if (compact) 6.dp else 18.dp, vertical = 28.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = if (compact) 0.dp else 8.dp),
                    ) {
                        BrandMark(size = if (compact) 38.dp else 44.dp)
                        if (!compact) {
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = appName,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.padding(top = 26.dp))

                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
                    ) {
                        entries.forEachIndexed { index, entry ->
                            TvNavItem(
                                icon = entry.icon,
                                label = entry.label,
                                selected = entry.route == selectedRoute,
                                onClick = { onNavigate(entry.route) },
                                modifier = if (index == 0) Modifier.focusRequester(firstItemFocus) else Modifier,
                                showLabel = !compact,
                            )
                            Spacer(modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }

            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(screenPadding),
            ) {
                content()
            }
        }
    }
}
