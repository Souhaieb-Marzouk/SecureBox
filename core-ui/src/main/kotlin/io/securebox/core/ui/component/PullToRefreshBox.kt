package io.securebox.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp

/**
 * Pull-to-refresh for lazily scrolling content.
 *
 * Uses a nested-scroll connection instead of a raw pointer detector so the list keeps its normal
 * scrolling. Only the unconsumed overscroll at the top of the list is treated as a pull gesture.
 */
@Composable
fun PullToRefreshBox(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val threshold = with(density) { 72.dp.toPx() }
    var pull by remember { mutableFloatStateOf(0f) }

    val connection =
        remember(refreshing) {
            object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (source == NestedScrollSource.Drag && available.y > 0f && !refreshing) {
                        pull = (pull + available.y * 0.5f).coerceAtMost(threshold * 2f)
                    }
                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (pull >= threshold && !refreshing) {
                        pull = 0f
                        onRefresh()
                    } else {
                        pull = 0f
                    }
                    return Velocity.Zero
                }
            }
        }

    Box(
        modifier = modifier.fillMaxSize().nestedScroll(connection),
    ) {
        content()
        if (pull > 0f || refreshing) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = with(density) { pull.coerceAtMost(threshold).toDp() })
                        .padding(8.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
            }
        }
    }
}
