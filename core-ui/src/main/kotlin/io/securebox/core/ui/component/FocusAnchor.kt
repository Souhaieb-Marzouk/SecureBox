package io.securebox.core.ui.component

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Invisible focus stop used at the top/bottom of scrollable content and inside cards.
 *
 * Android TV moves the viewport to the focused node. Text-only sections have no focusable child, so
 * without anchors a remote cannot reach content above the first button or below the last one. A
 * 1 dp focusable anchor gives D-pad a place to land and makes that text reachable.
 */
@Composable
fun FocusAnchor(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(1.dp)
                .focusable(),
    )
}
