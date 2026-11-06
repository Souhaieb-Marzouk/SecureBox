package io.securebox.feature.threatintel

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/**
 * Threat Intelligence is an isolated, feature-flagged add-on. In this release it is enabled for
 * the free trial; a remote flag can turn it off without shipping new code, and a paid tier can be
 * introduced by changing [TrialState] only.
 */
object ThreatIntelFlags {
    /** Local default for the trial build. The feed client will honour a remote flag in v0.5.0. */
    const val ENABLED_BY_DEFAULT: Boolean = true

    val trialState: TrialState = TrialState.FREE_TRIAL
}

enum class TrialState {
    FREE_TRIAL,
    PAID,
    DISABLED,
}

/** Small "Free Trial" badge shown next to the module title. */
@Composable
fun TrialBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f),
    ) {
        Text(
            text = stringResource(R.string.threat_intel_badge_free_trial),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
