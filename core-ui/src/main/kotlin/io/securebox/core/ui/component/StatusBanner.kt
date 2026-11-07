package io.securebox.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.securebox.core.ui.icon.SecureBoxIcons

enum class BannerSeverity {
    INFO,
    WARNING,
    DANGER,
}

/**
 * Inline alert with the "what happened + why + what to do" structure. Colour is reinforced by an
 * icon and explicit wording, never colour alone.
 */
@Composable
fun StatusBanner(
    severity: BannerSeverity,
    message: String,
    modifier: Modifier = Modifier,
) {
    val accent =
        when (severity) {
            BannerSeverity.INFO -> MaterialTheme.colorScheme.primary
            BannerSeverity.WARNING -> MaterialTheme.colorScheme.tertiary
            BannerSeverity.DANGER -> MaterialTheme.colorScheme.error
        }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = accent.copy(alpha = 0.10f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(34.dp)
                        .padding(0.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(shape = CircleShape, color = accent.copy(alpha = 0.16f)) {
                    Box(modifier = Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector =
                                when (severity) {
                                    BannerSeverity.INFO -> SecureBoxIcons.Shield
                                    BannerSeverity.WARNING -> SecureBoxIcons.Alert
                                    BannerSeverity.DANGER -> SecureBoxIcons.Alert
                                },
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
