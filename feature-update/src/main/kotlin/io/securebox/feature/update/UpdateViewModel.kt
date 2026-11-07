package io.securebox.feature.update

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.securebox.core.domain.install.ApkInstaller
import io.securebox.core.domain.install.InstallOutcome
import io.securebox.core.domain.model.AppVersion
import io.securebox.core.domain.model.CheckResult
import io.securebox.core.domain.model.DownloadResult
import io.securebox.core.domain.model.ReleaseInfo
import io.securebox.core.domain.model.UpdateDecision
import io.securebox.core.domain.repository.UpdateRepository
import io.securebox.core.domain.update.UpdateEvaluator
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class UpdateFailure {
    RATE_LIMITED,
    NO_RELEASE,
    NETWORK,
    INVALID_RELEASE,
    DOWNLOAD,
    CHECKSUM_MISMATCH,
    MISSING_CHECKSUM,
    INSTALL,
}

sealed interface UpdatePhase {
    data object Idle : UpdatePhase

    data object Checking : UpdatePhase

    data object UpToDate : UpdatePhase

    data class Available(val release: ReleaseInfo) : UpdatePhase

    data class Downloading(
        val fraction: Float,
        val downloadedBytes: Long,
        val totalBytes: Long,
    ) : UpdatePhase

    data object Verifying : UpdatePhase

    data class ReadyToInstall(
        val file: File,
        val sha256: String,
    ) : UpdatePhase

    data class NeedsInstallPermission(val file: File) : UpdatePhase

    data class Failed(
        val kind: UpdateFailure,
        val detail: String? = null,
    ) : UpdatePhase
}

data class UpdateUiState(
    val installedVersion: String,
    val phase: UpdatePhase = UpdatePhase.Idle,
    val lastCheckedAtMillis: Long? = null,
    val installPermissionGranted: Boolean = true,
)

@HiltViewModel
class UpdateViewModel
    @Inject
    constructor(
        private val updateRepository: UpdateRepository,
        private val apkInstaller: ApkInstaller,
        private val appVersion: AppVersion,
    ) : ViewModel() {
        private val _state = MutableStateFlow(UpdateUiState(installedVersion = appVersion.versionName))
        val state: StateFlow<UpdateUiState> = _state.asStateFlow()

        init {
            viewModelScope.launch {
                val last = updateRepository.lastCheckedAtMillis()
                _state.update { it.copy(lastCheckedAtMillis = last) }
            }
            // Silent check using the 12h cache; it only shows UI when an update actually exists.
            checkForUpdate(force = false)
        }

        fun refreshInstallPermission() {
            _state.update { it.copy(installPermissionGranted = apkInstaller.canInstallPackages()) }
        }

        fun checkForUpdate(force: Boolean) {
            if (_state.value.phase is UpdatePhase.Checking || _state.value.phase is UpdatePhase.Downloading) return
            val silent = !force
            _state.update { it.copy(phase = UpdatePhase.Checking) }
            viewModelScope.launch {
                when (val result = updateRepository.fetchLatestRelease(force = force, current = appVersion)) {
                    is CheckResult.Fetched -> handleRelease(result.release)
                    CheckResult.RateLimited -> fail(UpdateFailure.RATE_LIMITED, silent = silent)
                    CheckResult.NoRelease -> fail(UpdateFailure.NO_RELEASE, silent = silent)
                    is CheckResult.Failed -> fail(UpdateFailure.NETWORK, result.reason, silent = silent)
                }
                _state.update { it.copy(lastCheckedAtMillis = updateRepository.lastCheckedAtMillis()) }
            }
        }

        private fun handleRelease(release: ReleaseInfo) {
            when (val decision = UpdateEvaluator.evaluate(appVersion, release)) {
                is UpdateDecision.UpdateAvailable -> _state.update { it.copy(phase = UpdatePhase.Available(release)) }
                UpdateDecision.UpToDate -> _state.update { it.copy(phase = UpdatePhase.UpToDate) }
                is UpdateDecision.Incomparable -> fail(UpdateFailure.INVALID_RELEASE, decision.reason)
            }
        }

        fun downloadAndInstall() {
            val release = (_state.value.phase as? UpdatePhase.Available)?.release ?: return
            viewModelScope.launch {
                _state.update { it.copy(phase = UpdatePhase.Downloading(0f, 0L, 0L)) }
                val primaryAbi = Build.SUPPORTED_ABIS.firstOrNull()
                when (
                    val result =
                        updateRepository.downloadApk(
                            release = release,
                            primaryAbi = primaryAbi,
                            onProgress = { progress ->
                                _state.update {
                                    it.copy(
                                        phase =
                                            UpdatePhase.Downloading(
                                                fraction = progress.fraction,
                                                downloadedBytes = progress.bytesDownloaded,
                                                totalBytes = progress.totalBytes,
                                            ),
                                    )
                                }
                            },
                            onVerifying = {
                                _state.update { it.copy(phase = UpdatePhase.Verifying) }
                            },
                        )
                ) {
                    is DownloadResult.Verified -> install(result.file, result.sha256)
                    is DownloadResult.ChecksumMismatch -> fail(UpdateFailure.CHECKSUM_MISMATCH)
                    DownloadResult.MissingChecksum -> fail(UpdateFailure.MISSING_CHECKSUM)
                    is DownloadResult.Failed -> fail(UpdateFailure.DOWNLOAD, result.reason)
                }
            }
        }

        private fun install(
            file: File,
            sha256: String,
        ) {
            _state.update { it.copy(phase = UpdatePhase.ReadyToInstall(file, sha256)) }
            when (val outcome = apkInstaller.install(file)) {
                InstallOutcome.Started -> _state.update { it.copy(phase = UpdatePhase.Idle) }
                InstallOutcome.PermissionRequired -> {
                    _state.update { it.copy(phase = UpdatePhase.NeedsInstallPermission(file), installPermissionGranted = false) }
                }
                is InstallOutcome.Failed -> fail(UpdateFailure.INSTALL, outcome.reason)
            }
        }

        fun openInstallPermissionSettings() {
            apkInstaller.openInstallPermissionSettings()
        }

        fun installAfterPermissionResult() {
            val pending = _state.value.phase as? UpdatePhase.NeedsInstallPermission ?: return
            if (!apkInstaller.canInstallPackages()) {
                _state.update { it.copy(installPermissionGranted = false) }
                return
            }
            when (val outcome = apkInstaller.install(pending.file)) {
                InstallOutcome.Started -> _state.update { it.copy(phase = UpdatePhase.Idle) }
                InstallOutcome.PermissionRequired -> _state.update { it.copy(installPermissionGranted = false) }
                is InstallOutcome.Failed -> fail(UpdateFailure.INSTALL, outcome.reason)
            }
        }

        fun dismiss() {
            _state.update { it.copy(phase = UpdatePhase.Idle) }
        }

        fun retry() {
            checkForUpdate(force = true)
        }

        private fun fail(
            kind: UpdateFailure,
            detail: String? = null,
            silent: Boolean = false,
        ) {
            // A background check that fails (offline TV box, GitHub rate limit) must not ambush the
            // user with a dialog later; only an explicit "Check for updates" surfaces the error.
            if (silent) {
                _state.update { it.copy(phase = UpdatePhase.Idle) }
                return
            }
            _state.update { it.copy(phase = UpdatePhase.Failed(kind, detail)) }
        }
    }
