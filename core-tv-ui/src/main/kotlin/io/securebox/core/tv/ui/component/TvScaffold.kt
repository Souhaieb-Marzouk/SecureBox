package io.securebox.core.tv.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.securebox.core.tv.ui.theme.TvDimens
import io.securebox.core.ui.icon.SecureBoxIcons

data class TvNavEntry(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

/**
 * Sidebar layout used by every TV screen: persistent navigation on the left, scrollable content
 * on the right, generous 48dp margins.
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

    Row(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier =
                Modifier
                    .width(TvDimens.SidebarWidth)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 20.dp, vertical = 32.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                Icon(
                    imageVector = SecureBoxIcons.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = appName, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(modifier = Modifier.padding(top = 28.dp))

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
                    )
                    Spacer(modifier = Modifier.padding(top = 4.dp))
                }
            }
        }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(TvDimens.ScreenPadding)
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(24.dp)),
        ) {
            content()
        }
    }
}
