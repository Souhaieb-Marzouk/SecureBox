package io.securebox

import android.content.Context
import android.net.VpnService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.securebox.core.data.security.PackageSignatureVerifier
import io.securebox.core.data.security.SignatureCheck
import io.securebox.core.domain.model.AppSettings
import io.securebox.core.domain.permission.PlatformPermissions
import io.securebox.core.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MainUiState(
    val isLoading: Boolean = true,
    val settings: AppSettings = AppSettings(),
    val signatureOk: Boolean = true,
)

@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
        private val signatureVerifier: PackageSignatureVerifier,
        private val platformPermissions: PlatformPermissions,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        private val _state = MutableStateFlow(MainUiState())
        val state: StateFlow<MainUiState> = _state.asStateFlow()

        init {
            viewModelScope.launch {
                settingsRepository.settings.collect { settings ->
                    _state.update { it.copy(settings = settings, isLoading = false) }
                }
            }
            viewModelScope.launch {
                val check = withContext(Dispatchers.IO) { signatureVerifier.verify() }
                // NOT_CONFIGURED (debug builds) and UNREADABLE must not scare the user: only a real
                // certificate mismatch means the APK was re-signed by someone else.
                val ok = check.reason != SignatureCheck.Reason.MISMATCH
                _state.update { it.copy(signatureOk = ok) }
            }
        }

        /** Re-reads special permissions when the app returns to the foreground. */
        fun refreshRuntimeState() {
            viewModelScope.launch {
                val usage = platformPermissions.hasUsageAccess()
                val notifications = platformPermissions.hasNotificationPermission()
                val vpnPrepared = runCatching { VpnService.prepare(context) == null }.getOrDefault(false)
                settingsRepository.recordPermissionSnapshot(
                    usageAccessGranted = usage,
                    notificationsGranted = notifications,
                    vpnPrepared = vpnPrepared,
                )
            }
        }
    }
