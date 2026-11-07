package io.securebox.core.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.securebox.core.domain.model.ScoreGrade
import io.securebox.core.ui.theme.SecureBoxColors

/**
 * Circular gauge for the 0-100 Security Score. The number is the primary information; the ring is a
 * secondary, colour-supported cue and never the only signal.
 */
@Composable
fun ScoreGauge(
    score: Int,
    grade: ScoreGrade,
    gradeLabel: String,
    modifier: Modifier = Modifier,
    size: Dp = 190.dp,
    contentDescription: String,
) {
    val colors =
        when (grade) {
            ScoreGrade.EXCELLENT, ScoreGrade.GOOD ->
                listOf(SecureBoxColors.AuroraTeal, SecureBoxColors.AuroraCyan, SecureBoxColors.AuroraTeal)

            ScoreGrade.FAIR ->
                listOf(SecureBoxColors.AuroraAmber, SecureBoxColors.AuroraTeal, SecureBoxColors.AuroraAmber)

            ScoreGrade.AT_RISK ->
                listOf(SecureBoxColors.AuroraCoral, SecureBoxColors.AuroraViolet, SecureBoxColors.AuroraCoral)
        }
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val fraction = (score.coerceIn(0, 100) / 100f)

    Box(
        modifier =
            modifier
                .size(size)
                .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidth = 16.dp.toPx()
            val inset = strokeWidth / 2f
            val arcSize = Size(this.size.width - strokeWidth, this.size.height - strokeWidth)

            // Soft glow behind the ring.
            drawArc(
                brush = Brush.sweepGradient(colors),
                startAngle = -90f,
                sweepAngle = 360f * fraction,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                alpha = 0.22f,
                style = Stroke(width = strokeWidth * 2.2f, cap = StrokeCap.Round),
            )
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
                brush = Brush.sweepGradient(colors),
                startAngle = -90f,
                sweepAngle = 360f * fraction,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Text(
                text = score.toString(),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = gradeLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** A tiny gradient dot used in legends and status rows. */
@Composable
fun GradientDot(color: Color = SecureBoxColors.AuroraTeal) {
    Canvas(modifier = Modifier.size(8.dp)) {
        drawCircle(
            brush = Brush.radialGradient(listOf(color, color.copy(alpha = 0.35f))),
            radius = this.size.minDimension / 2f,
        )
    }
}
