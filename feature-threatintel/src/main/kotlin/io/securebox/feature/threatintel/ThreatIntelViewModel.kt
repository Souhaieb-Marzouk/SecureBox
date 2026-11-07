package io.securebox.feature.threatintel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.securebox.core.domain.repository.SettingsRepository
import io.securebox.core.domain.security.FeedUpdateResult
import io.securebox.core.domain.security.ThreatFeedRepository
import io.securebox.core.domain.security.ThreatFeedStatus
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FeedResultKind {
    UPDATED,
    UP_TO_DATE,
    INVALID_SIGNATURE,
    NOT_CONFIGURED,
    FAILED,
}

data class ThreatIntelUiState(
    val loading: Boolean = true,
    val updating: Boolean = false,
    val status: ThreatFeedStatus? = null,
    val autoUpdate: Boolean = true,
    val result: FeedResultKind? = null,
    val detail: String? = null,
)

@HiltViewModel
class ThreatIntelViewModel
    @Inject
    constructor(
        private val feedRepository: ThreatFeedRepository,
        private val settingsRepository: SettingsRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(ThreatIntelUiState())
        val state: StateFlow<ThreatIntelUiState> = _state.asStateFlow()

        init {
            refresh()
            viewModelScope.launch {
                settingsRepository.settings.collect { settings ->
                    _state.update { it.copy(autoUpdate = settings.threatIntelAutoUpdate) }
                }
            }
        }

        fun refresh() {
            viewModelScope.launch {
                val status = runCatching { feedRepository.status() }.getOrNull()
                _state.update { it.copy(loading = false, status = status) }
            }
        }

        fun updateNow() {
            if (_state.value.updating) return
            viewModelScope.launch {
                _state.update { it.copy(updating = true, result = null, detail = null) }
                val result = runCatching { feedRepository.update(force = true) }.getOrNull()
                val kind =
                    when (result) {
                        is FeedUpdateResult.Updated -> FeedResultKind.UPDATED
                        FeedUpdateResult.UpToDate -> FeedResultKind.UP_TO_DATE
                        FeedUpdateResult.InvalidSignature -> FeedResultKind.INVALID_SIGNATURE
                        FeedUpdateResult.NotConfigured -> FeedResultKind.NOT_CONFIGURED
                        is FeedUpdateResult.Failed -> FeedResultKind.FAILED
                        null -> FeedResultKind.FAILED
                    }
                val status = runCatching { feedRepository.status() }.getOrNull()
                _state.update {
                    it.copy(
                        updating = false,
                        status = status,
                        result = kind,
                        detail = (result as? FeedUpdateResult.Failed)?.reason,
                    )
                }
            }
        }

        fun setAutoUpdate(enabled: Boolean) {
            viewModelScope.launch { settingsRepository.setThreatIntelAutoUpdate(enabled) }
        }
    }
