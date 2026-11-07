package io.securebox.feature.threats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.securebox.core.domain.security.DangerousSetting
import io.securebox.core.domain.security.DeviceSettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HardeningUiState(
    val loading: Boolean = true,
    val settings: List<DangerousSetting> = emptyList(),
)

@HiltViewModel
class HardeningViewModel
    @Inject
    constructor(
        private val repository: DeviceSettingsRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(HardeningUiState())
        val state: StateFlow<HardeningUiState> = _state.asStateFlow()

        init {
            refresh()
        }

        fun refresh() {
            viewModelScope.launch {
                val settings = runCatching { repository.audit() }.getOrDefault(emptyList())
                _state.update { it.copy(loading = false, settings = settings) }
            }
        }
    }
