package io.securebox.feature.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.securebox.core.domain.security.AppRiskReport
import io.securebox.core.domain.security.SecurityRepository
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppsFilter {
    ALL,
    RISKY,
    SYSTEM,
    PLAY_STORE,
    OTHER_STORE,
    MANUAL,
}

data class AppsUiState(
    val loading: Boolean = true,
    val reports: List<AppRiskReport> = emptyList(),
    val filter: AppsFilter = AppsFilter.ALL,
    val analyzed: Int = 0,
    val total: Int = 0,
    val selected: AppRiskReport? = null,
)

@HiltViewModel
class AppsViewModel
    @Inject
    constructor(
        private val securityRepository: SecurityRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(AppsUiState())
        val state: StateFlow<AppsUiState> = _state.asStateFlow()
        private var loadJob: Job? = null

        init {
            load()
        }

        fun load() {
            if (loadJob?.isActive == true) return
            loadJob =
                viewModelScope.launch {
                    _state.update { it.copy(loading = true) }
                    val selectedPackage = _state.value.selected?.app?.packageName
                    val reports =
                        runCatching {
                            securityRepository.auditAll { done, total ->
                                _state.update { it.copy(analyzed = done, total = total) }
                            }
                        }.getOrDefault(emptyList())
                    _state.update {
                        it.copy(
                            loading = false,
                            reports = reports,
                            analyzed = reports.size,
                            total = reports.size,
                            selected =
                                if (selectedPackage == null) {
                                    it.selected
                                } else {
                                    reports.firstOrNull { report -> report.app.packageName == selectedPackage }
                                },
                        )
                    }
                }
        }

        fun setFilter(filter: AppsFilter) {
            _state.update { it.copy(filter = filter) }
        }

        fun select(report: AppRiskReport?) {
            _state.update { it.copy(selected = report) }
        }
    }
