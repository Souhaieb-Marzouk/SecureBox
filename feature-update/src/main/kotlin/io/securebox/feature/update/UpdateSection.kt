package io.securebox.feature.update

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.securebox.core.domain.model.ReleaseInfo
import io.securebox.core.ui.component.SecureCard
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * Settings card that owns the whole update flow: check, download, verify and hand-off to the
 * system installer.
 */
@Composable
fun UpdateSection(
    modifier: Modifier = Modifier,
    viewModel: UpdateViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.refreshInstallPermission()
                    // Safe to call unconditionally: the ViewModel ignores it unless an install is
                    // actually pending, which avoids capturing a stale phase in this effect.
                    viewModel.installAfterPermissionResult()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    SecureCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = stringResource(R.string.update_section_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.update_installed_version, state.installedVersion),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = { viewModel.checkForUpdate(force = true) },
                enabled = state.phase !is UpdatePhase.Checking && state.phase !is UpdatePhase.Downloading,
            ) {
                if (state.phase is UpdatePhase.Checking) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(text = stringResource(R.string.update_check_button))
                }
            }
        }

        Text(
            text = statusLine(state),
            style = MaterialTheme.typography.bodyMedium,
            color =
                if (state.phase is UpdatePhase.Failed) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
    }

    when (val phase = state.phase) {
        is UpdatePhase.Available ->
            AvailableDialog(release = phase.release, onDismiss = viewModel::dismiss, onDownload = viewModel::downloadAndInstall)

        is UpdatePhase.Downloading -> DownloadingDialog(phase)
        is UpdatePhase.Failed -> FailureDialog(phase, onDismiss = viewModel::dismiss, onRetry = viewModel::retry)
        is UpdatePhase.NeedsInstallPermission ->
            InstallPermissionDialog(
                onOpenSettings = viewModel::openInstallPermissionSettings,
                onDismiss = viewModel::dismiss,
            )

        else -> Unit
    }
}

@Composable
private fun statusLine(state: UpdateUiState): String =
    when (state.phase) {
        UpdatePhase.UpToDate -> stringResource(R.string.update_up_to_date)
        is UpdatePhase.Failed -> failureMessage(state.phase.kind)
        UpdatePhase.Checking -> stringResource(R.string.update_checking)
        else -> lastCheckedLabel(state.lastCheckedAtMillis)
    }

@Composable
private fun lastCheckedLabel(lastCheckedAtMillis: Long?): String =
    if (lastCheckedAtMillis == null) {
        stringResource(R.string.update_never_checked)
    } else {
        val formatted =
            remember(lastCheckedAtMillis) {
                DateFormat
                    .getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
                    .format(Date(lastCheckedAtMillis))
            }
        stringResource(R.string.update_last_checked, formatted)
    }

@Composable
private fun failureMessage(kind: UpdateFailure): String =
    stringResource(
        when (kind) {
            UpdateFailure.RATE_LIMITED -> R.string.update_error_rate_limited
            UpdateFailure.NO_RELEASE -> R.string.update_error_no_release
            UpdateFailure.NETWORK -> R.string.update_error_network
            UpdateFailure.INVALID_RELEASE -> R.string.update_error_invalid
            UpdateFailure.DOWNLOAD -> R.string.update_error_download
            UpdateFailure.CHECKSUM_MISMATCH -> R.string.update_error_checksum
            UpdateFailure.MISSING_CHECKSUM -> R.string.update_error_missing_checksum
            UpdateFailure.INSTALL -> R.string.update_error_install
        },
    )

@Composable
private fun AvailableDialog(
    release: ReleaseInfo,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
) {
    val context = LocalContext.current
    val apk = release.asset(ReleaseInfo.UNIVERSAL_ASSET_NAME) ?: release.assets.firstOrNull { it.name.endsWith(".apk") }
    val sizeLabel = apk?.let { Formatter.formatFileSize(context, it.sizeBytes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.update_available_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.update_available_body, release.tagName),
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (sizeLabel != null) {
                    Text(
                        text = stringResource(R.string.update_download_size, sizeLabel),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(text = stringResource(R.string.update_notes_title), style = MaterialTheme.typography.titleMedium)
                Column(
                    modifier =
                        Modifier
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState()),
                ) {
                    if (release.notes.isBlank()) {
                        Text(text = stringResource(R.string.update_notes_empty))
                    } else {
                        MarkdownText(markdown = release.notes)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDownload) {
                Text(text = stringResource(R.string.update_download_install))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.update_later))
            }
        },
    )
}

@Composable
private fun DownloadingDialog(phase: UpdatePhase.Downloading) {
    val context = LocalContext.current
    val downloaded = Formatter.formatFileSize(context, phase.downloadedBytes)
    val total = Formatter.formatFileSize(context, phase.totalBytes)
    AlertDialog(
        onDismissRequest = {},
        title = { Text(text = stringResource(R.string.update_downloading_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LinearProgressIndicator(
                    progress = phase.fraction,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.update_downloading_detail, downloaded, total),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {},
    )
}

@Composable
private fun FailureDialog(
    phase: UpdatePhase.Failed,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    val why =
        when (phase.kind) {
            UpdateFailure.CHECKSUM_MISMATCH -> stringResource(R.string.update_error_checksum_why)
            UpdateFailure.MISSING_CHECKSUM -> stringResource(R.string.update_error_missing_checksum_why)
            UpdateFailure.RATE_LIMITED -> stringResource(R.string.update_error_rate_limited_why)
            else -> null
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.update_failed_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = failureMessage(phase.kind), style = MaterialTheme.typography.bodyLarge)
                if (why != null) {
                    Text(
                        text = why,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (phase.detail != null) {
                    Text(
                        text = phase.detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(text = stringResource(R.string.update_error_action), style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            TextButton(onClick = onRetry) {
                Text(text = stringResource(R.string.update_retry))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.update_later))
            }
        },
    )
}

@Composable
private fun InstallPermissionDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.update_permission_title)) },
        text = { Text(text = stringResource(R.string.update_permission_body)) },
        confirmButton = {
            Button(onClick = onOpenSettings) {
                Text(text = stringResource(R.string.update_open_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.update_later))
            }
        },
    )
}

/**
 * Tiny Markdown renderer for release notes: headings, bullets, bold and inline code.
 * A full Markdown engine would add weight the app does not need.
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    val lines = remember(markdown) { markdown.lines() }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        lines.forEach { rawLine ->
            val line = rawLine.trimEnd()
            when {
                line.isBlank() -> Spacer(modifier = Modifier.height(2.dp))
                line.startsWith("### ") ->
                    Text(text = inlineMarkdown(line.removePrefix("### ")), style = MaterialTheme.typography.titleMedium)

                line.startsWith("## ") ->
                    Text(text = inlineMarkdown(line.removePrefix("## ")), style = MaterialTheme.typography.titleLarge)

                line.startsWith("# ") ->
                    Text(text = inlineMarkdown(line.removePrefix("# ")), style = MaterialTheme.typography.headlineMedium)

                line.startsWith("- ") || line.startsWith("* ") ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier =
                                Modifier
                                    .padding(top = 8.dp)
                                    .size(6.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                        )
                        Text(
                            text = inlineMarkdown(line.drop(2)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                else ->
                    Text(
                        text = inlineMarkdown(line),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
            }
        }
    }
}

/** Very small inline formatter: **bold** and `code`. */
private fun inlineMarkdown(text: String): AnnotatedString =
    buildAnnotatedString {
        val segments = text.split("`")
        segments.forEachIndexed { segmentIndex, segment ->
            val isCode = segmentIndex % 2 == 1
            val boldParts = segment.split("**")
            boldParts.forEachIndexed { boldIndex, part ->
                val isBold = boldIndex % 2 == 1
                when {
                    isCode -> withStyle(SpanStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)) { append(part) }
                    isBold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(part) }
                    else -> append(part)
                }
            }
        }
    }
