package io.securebox.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import io.securebox.core.ui.theme.SecureBoxColors

/** The glassy 24dp Aurora card used throughout both shells. */
@Composable
fun SecureCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (dark) 0.78f else 0.92f),
        border = border ?: BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)),
        shadowElevation = if (dark) 10.dp else 4.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                SecureBoxColors.AuroraTeal.copy(alpha = if (dark) 0.035f else 0.02f),
                                androidx.compose.ui.graphics.Color.Transparent,
                            ),
                        ),
                    )
                    .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

/** Accent bar + title + one-sentence "what this does" subtitle, on every screen. */
@Composable
fun ScreenHeading(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier =
                Modifier
                    .width(44.dp)
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(SecureBoxColors.AuroraTeal, SecureBoxColors.AuroraViolet),
                        ),
                        RoundedCornerShape(50),
                    ),
        )
        Text(text = title, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
