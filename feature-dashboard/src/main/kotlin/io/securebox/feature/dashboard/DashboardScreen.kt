package io.securebox.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.securebox.core.domain.model.CalculateSecurityScoreUseCase
import io.securebox.core.domain.model.DeviceMode
import io.securebox.core.domain.model.ScoreGrade
import io.securebox.core.ui.component.ActionCard
import io.securebox.core.ui.component.ScoreGauge
import io.securebox.core.ui.component.ScoreGradeLabels
import io.securebox.core.ui.component.SecureCard
import io.securebox.core.ui.icon.SecureBoxIcons

@Composable
fun DashboardRoute(
    onOpenNetwork: () -> Unit,
    onOpenThreats: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DashboardScreen(
        state = state,
        onOpenNetwork = onOpenNetwork,
        onOpenThreats = onOpenThreats,
        onOpenSettings = onOpenSettings,
    )
}

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onOpenNetwork: () -> Unit,
    onOpenThreats: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = stringResource(R.string.dashboard_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.dashboard_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(modifier = Modifier.padding(vertical = 8.dp)) {
            ScoreGauge(
                score = state.score.value,
                grade = state.score.grade,
                gradeLabel = stringResource(ScoreGradeLabels.of(state.score.grade)),
                contentDescription = stringResource(R.string.dashboard_score_description, state.score.value),
            )
        }

        Text(
            text = stringResource(R.string.dashboard_device_mode, deviceModeLabel(state.deviceMode)),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SecureCard {
            Text(text = stringResource(R.string.dashboard_improve_title), style = MaterialTheme.typography.titleMedium)
            val suggestions = state.score.factors.filterNot { it.satisfied }
            if (suggestions.isEmpty()) {
                Text(
                    text = stringResource(R.string.dashboard_all_good),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                suggestions.take(3).forEach { factor ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = SecureBoxIcons.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(factorLabel(factor.id)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        Text(text = stringResource(R.string.dashboard_quick_actions), style = MaterialTheme.typography.titleLarge)

        ActionCard(
            icon = SecureBoxIcons.Shield,
            title = stringResource(R.string.dashboard_action_scan_title),
            description = stringResource(R.string.dashboard_action_scan_body),
            onClick = onOpenThreats,
        )
        ActionCard(
            icon = SecureBoxIcons.Network,
            title = stringResource(R.string.dashboard_action_firewall_title),
            description = stringResource(R.string.dashboard_action_firewall_body),
            onClick = onOpenNetwork,
        )
        ActionCard(
            icon = SecureBoxIcons.Refresh,
            title = stringResource(R.string.dashboard_action_update_title),
            description = stringResource(R.string.dashboard_action_update_body),
            onClick = onOpenSettings,
        )

        Spacer(modifier = Modifier.height(8.dp))
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

/** Kept for readability in previews and tests. */
internal fun gradeOrDefault(score: Int): ScoreGrade =
    when {
        score >= 85 -> ScoreGrade.EXCELLENT
        score >= 65 -> ScoreGrade.GOOD
        score >= 40 -> ScoreGrade.FAIR
        else -> ScoreGrade.AT_RISK
    }
