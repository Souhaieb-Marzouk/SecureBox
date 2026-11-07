package io.securebox.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.securebox.core.ui.component.BrandMark
import io.securebox.core.ui.component.ScreenHeading
import io.securebox.core.ui.component.SecureCard
import io.securebox.core.ui.theme.SecureBoxColors

/**
 * Plain-language guide. Every technical concept used elsewhere in the app is explained here with a
 * single sentence and a concrete "what to do" step, so a first-time user never has to guess.
 */
@Composable
fun HelpScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            TextButton(onClick = onBack) {
                Text(text = stringResource(R.string.help_back))
            }
        }
        item {
            ScreenHeading(
                title = stringResource(R.string.help_title),
                subtitle = stringResource(R.string.help_subtitle),
            )
        }
        item {
            SecureCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    BrandMark(size = 64.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.help_intro_title),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = stringResource(R.string.help_intro_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.help_steps_title),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        item {
            HelpStep(
                number = "1",
                title = stringResource(R.string.help_step_scan_title),
                body = stringResource(R.string.help_step_scan_body),
            )
        }
        item {
            HelpStep(
                number = "2",
                title = stringResource(R.string.help_step_apps_title),
                body = stringResource(R.string.help_step_apps_body),
            )
        }
        item {
            HelpStep(
                number = "3",
                title = stringResource(R.string.help_step_block_title),
                body = stringResource(R.string.help_step_block_body),
            )
        }
        item {
            HelpStep(
                number = "4",
                title = stringResource(R.string.help_step_update_title),
                body = stringResource(R.string.help_step_update_body),
            )
        }

        item {
            Text(
                text = stringResource(R.string.help_words_title),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        item {
            SecureCard {
                HelpTerm(
                    term = stringResource(R.string.help_word_score),
                    definition = stringResource(R.string.help_word_score_body),
                )
                HelpTerm(
                    term = stringResource(R.string.help_word_firewall),
                    definition = stringResource(R.string.help_word_firewall_body),
                )
                HelpTerm(
                    term = stringResource(R.string.help_word_permission),
                    definition = stringResource(R.string.help_word_permission_body),
                )
                HelpTerm(
                    term = stringResource(R.string.help_word_sideloaded),
                    definition = stringResource(R.string.help_word_sideloaded_body),
                )
                HelpTerm(
                    term = stringResource(R.string.help_word_tracker),
                    definition = stringResource(R.string.help_word_tracker_body),
                )
            }
        }

        item {
            SecureCard {
                Text(
                    text = stringResource(R.string.help_risky_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.help_risky_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            SecureCard {
                Text(
                    text = stringResource(R.string.help_safe_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.help_safe_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HelpStep(
    number: String,
    title: String,
    body: String,
) {
    SecureCard(modifier = Modifier.focusable()) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(36.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(SecureBoxColors.AuroraTeal, SecureBoxColors.AuroraViolet),
                            ),
                            CircleShape,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.labelLarge,
                    color = SecureBoxColors.Ink950,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HelpTerm(
    term: String,
    definition: String,
) {
    Column(
        modifier = Modifier.padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = term, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(
            text = definition,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
