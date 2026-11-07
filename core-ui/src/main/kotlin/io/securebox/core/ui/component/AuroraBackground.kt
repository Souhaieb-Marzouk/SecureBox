package io.securebox.core.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.MaterialTheme
import io.securebox.core.ui.theme.SecureBoxColors

/**
 * The Aurora backdrop used by both shells: a deep ink gradient with soft teal/violet light pools.
 * It is intentionally quiet so cards and text stay readable, including on a TV from a couch.
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val base =
        if (dark) {
            Brush.verticalGradient(
                listOf(
                    SecureBoxColors.Ink950,
                    SecureBoxColors.Ink900,
                    Color(0xFF090B1C),
                ),
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color(0xFFF7FAFF),
                    Color(0xFFEDF3FF),
                    Color(0xFFF8F1FF),
                ),
            )
        }
    val teal = SecureBoxColors.AuroraTeal.copy(alpha = if (dark) 0.20f else 0.14f)
    val violet = SecureBoxColors.AuroraViolet.copy(alpha = if (dark) 0.22f else 0.12f)
    val pink = SecureBoxColors.AuroraPink.copy(alpha = if (dark) 0.10f else 0.07f)

    Box(modifier = modifier.fillMaxSize().background(base)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush =
                    Brush.radialGradient(
                        colors = listOf(teal, Color.Transparent),
                        center = Offset(size.width * 0.12f, size.height * 0.02f),
                        radius = size.maxDimension * 0.75f,
                    ),
                radius = size.maxDimension * 0.75f,
                center = Offset(size.width * 0.12f, size.height * 0.02f),
            )
            drawCircle(
                brush =
                    Brush.radialGradient(
                        colors = listOf(violet, Color.Transparent),
                        center = Offset(size.width * 0.95f, size.height * 0.78f),
                        radius = size.maxDimension * 0.8f,
                    ),
                radius = size.maxDimension * 0.8f,
                center = Offset(size.width * 0.95f, size.height * 0.78f),
            )
            drawCircle(
                brush =
                    Brush.radialGradient(
                        colors = listOf(pink, Color.Transparent),
                        center = Offset(size.width * 0.7f, size.height * 0.05f),
                        radius = size.maxDimension * 0.45f,
                    ),
                radius = size.maxDimension * 0.45f,
                center = Offset(size.width * 0.7f, size.height * 0.05f),
            )
        }
        content()
    }
}
