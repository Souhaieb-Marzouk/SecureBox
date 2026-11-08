package io.securebox.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.securebox.core.ui.component.FocusAnchor
import io.securebox.core.ui.component.ScreenHeading
import io.securebox.core.ui.component.SecureCard

/** Plain-language privacy policy shown inside the app. It mirrors PRIVACY.md in the repository. */
@Composable
fun PrivacyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sections =
        listOf(
            R.string.privacy_no_telemetry_title to R.string.privacy_no_telemetry_body,
            R.string.privacy_on_device_title to R.string.privacy_on_device_body,
            R.string.privacy_network_title to R.string.privacy_network_body,
            R.string.privacy_permissions_title to R.string.privacy_permissions_body,
            R.string.privacy_controls_title to R.string.privacy_controls_body,
            R.string.privacy_export_title to R.string.privacy_export_body,
        )

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FocusAnchor()
        ScreenHeading(
            title = stringResource(R.string.privacy_title),
            subtitle = stringResource(R.string.privacy_subtitle),
        )

        sections.forEach { (titleRes, bodyRes) ->
            SecureCard {
                Text(text = stringResource(titleRes), style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(bodyRes),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row {
            OutlinedButton(onClick = onBack) {
                Text(text = stringResource(R.string.privacy_back))
            }
        }
        FocusAnchor()
    }
}
