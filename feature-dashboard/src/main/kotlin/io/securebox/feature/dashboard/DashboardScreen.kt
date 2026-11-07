package io.securebox.feature.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.securebox.core.domain.model.CalculateSecurityScoreUseCase
import io.securebox.core.domain.model.DeviceMode
import io.securebox.core.domain.model.ScoreGrade
import io.securebox.core.ui.component.ActionCard
import io.securebox.core.ui.component.BrandMark
import io.securebox.core.ui.component.GradientDot
import io.securebox.core.ui.component.ScoreGauge
import io.securebox.core.ui.component.ScoreGradeLabels
import io.securebox.core.ui.component.ScreenHeading
import io.securebox.core.ui.component.SecureCard
import io.securebox.core.ui.icon.SecureBoxIcons
import io.securebox.core.ui.theme.SecureBoxColors

@Composable
fun DashboardRoute(
    onOpenNetwork: () -> Unit,
    onOpenThreats: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenApps: () -> Unit,
    onOpenHelp: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DashboardScreen(
        state = state,
        onOpenNetwork = onOpenNetwork,
        onOpenThreats = onOpenThreats,
        onOpenSettings = onOpenSettings,
        onOpenApps = onOpenApps,
        onOpenHelp = onOpenHelp,
    )
}

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onOpenNetwork: () -> Unit,
    onOpenThreats: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenApps: () -> Unit,
    onOpenHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val suggestions = state.score.factors.filterNot { it.satisfied }
    val healthy = suggestions.isEmpty()
    val wide = LocalConfiguration.current.screenWidthDp >= 600

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ScreenHeading(
            title = stringResource(R.string.dashboard_title),
            subtitle = stringResource(R.string.dashboard_subtitle),
        )

        SecureCard(contentPadding = PaddingValues(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandMark(size = 26.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.dashboard_score_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GradientDot()
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = deviceModeLabel(state.deviceMode),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                ScoreGauge(
                    score = state.score.value,
                    grade = state.score.grade,
                    gradeLabel = stringResource(ScoreGradeLabels.of(state.score.grade)),
                    contentDescription = stringResource(R.string.dashboard_score_description, state.score.value),
                    size = if (wide) 180.dp else 132.dp,
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text =
                            stringResource(
                                if (healthy) R.string.dashboard_hero_protected else R.string.dashboard_hero_attention,
                            ),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text =
                            stringResource(
                                if (healthy) R.string.dashboard_hero_body_protected else R.string.dashboard_hero_body_attention,
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Text(text = stringResource(R.string.dashboard_quick_actions), style = MaterialTheme.typography.titleLarge)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickTile(
                icon = SecureBoxIcons.Shield,
                title = stringResource(R.string.dashboard_quick_scan),
                body = stringResource(R.string.dashboard_quick_scan_body),
                colors = listOf(SecureBoxColors.AuroraTeal, SecureBoxColors.AuroraCyan),
                onClick = onOpenThreats,
                modifier = Modifier.weight(1f).heightIn(min = if (wide) 190.dp else 148.dp),
            )
            QuickTile(
                icon = SecureBoxIcons.Network,
                title = stringResource(R.string.dashboard_quick_block),
                body = stringResource(R.string.dashboard_quick_block_body),
                colors = listOf(SecureBoxColors.AuroraViolet, SecureBoxColors.AuroraPink),
                onClick = onOpenNetwork,
                modifier = Modifier.weight(1f).heightIn(min = if (wide) 190.dp else 148.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickTile(
                icon = SecureBoxIcons.Apps,
                title = stringResource(R.string.dashboard_quick_apps),
                body = stringResource(R.string.dashboard_quick_apps_body),
                colors = listOf(SecureBoxColors.AuroraAmber, SecureBoxColors.AuroraCoral),
                onClick = onOpenApps,
                modifier = Modifier.weight(1f).heightIn(min = if (wide) 190.dp else 148.dp),
            )
            QuickTile(
                icon = SecureBoxIcons.Globe,
                title = stringResource(R.string.dashboard_quick_help),
                body = stringResource(R.string.dashboard_quick_help_body),
                colors = listOf(SecureBoxColors.AuroraCyan, SecureBoxColors.AuroraViolet),
                onClick = onOpenHelp,
                modifier = Modifier.weight(1f).heightIn(min = if (wide) 190.dp else 148.dp),
            )
        }

        Text(
            text = stringResource(if (healthy) R.string.dashboard_all_clear else R.string.dashboard_needs_attention),
            style = MaterialTheme.typography.titleLarge,
        )
        SecureCard {
            if (healthy) {
                Text(
                    text = stringResource(R.string.dashboard_all_good),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                suggestions.take(3).forEach { factor ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { openFactor(factor.id, onOpenNetwork, onOpenSettings) }
                                .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        GradientDot(color = SecureBoxColors.AuroraAmber)
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(text = stringResource(factorLabel(factor.id)), style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = stringResource(factorBody(factor.id)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            imageVector = SecureBoxIcons.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }

        ActionCard(
            icon = SecureBoxIcons.Lock,
            title = stringResource(R.string.dashboard_help_title),
            description = stringResource(R.string.dashboard_help_body),
            onClick = onOpenHelp,
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun QuickTile(
    icon: ImageVector,
    title: String,
    body: String,
    colors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        shadowElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .background(Brush.linearGradient(colors), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SecureBoxColors.Ink950,
                    modifier = Modifier.size(21.dp),
                )
            }
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = body,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun openFactor(
    id: String,
    onOpenNetwork: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    when (id) {
        CalculateSecurityScoreUseCase.FACTOR_FIREWALL,
        CalculateSecurityScoreUseCase.FACTOR_USAGE,
        -> onOpenNetwork()

        else -> onOpenSettings()
    }
}

@Composable
private fun deviceModeLabel(mode: DeviceMode): String =
    stringResource(
        if (mode == DeviceMode.TV) R.string.dashboard_device_tv else R.string.dashboard_device_phone,
    )

private fun factorLabel(id: String): Int =
    when (id) {
        CalculateSecurityScoreUseCase.FACTOR_FIREWALL -> R.string.dashboard_factor_firewall
        CalculateSecurityScoreUseCase.FACTOR_UPDATES -> R.string.dashboard_factor_updates
        CalculateSecurityScoreUseCase.FACTOR_USAGE -> R.string.dashboard_factor_usage
        CalculateSecurityScoreUseCase.FACTOR_NOTIFICATIONS -> R.string.dashboard_factor_notifications
        else -> R.string.dashboard_all_good
    }

private fun factorBody(id: String): Int =
    when (id) {
        CalculateSecurityScoreUseCase.FACTOR_FIREWALL -> R.string.dashboard_factor_firewall_body
        CalculateSecurityScoreUseCase.FACTOR_UPDATES -> R.string.dashboard_factor_updates_body
        CalculateSecurityScoreUseCase.FACTOR_USAGE -> R.string.dashboard_factor_usage_body
        CalculateSecurityScoreUseCase.FACTOR_NOTIFICATIONS -> R.string.dashboard_factor_notifications_body
        else -> R.string.dashboard_all_good
    }

/** Kept for readability in previews and tests. */
internal fun gradeOrDefault(score: Int): ScoreGrade =
    when {
        score >= 85 -> ScoreGrade.EXCELLENT
        score >= 65 -> ScoreGrade.GOOD
        score >= 40 -> ScoreGrade.FAIR
        else -> ScoreGrade.AT_RISK
    }
