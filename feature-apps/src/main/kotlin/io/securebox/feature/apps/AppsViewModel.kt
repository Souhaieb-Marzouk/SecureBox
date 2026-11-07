package io.securebox.feature.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.securebox.core.domain.security.AppRiskReport
import io.securebox.core.domain.security.SecurityRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppsFilter {
    ALL,
    RISKY,
    SIDELOADED,
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

        init {
            load()
        }

        fun load() {
            viewModelScope.launch {
                _state.update { it.copy(loading = true) }
                val cached = runCatching { securityRepository.storedReports() }.getOrDefault(emptyList())
                if (cached.isNotEmpty()) {
                    _state.update { it.copy(reports = cached.sortedBy { report -> report.score }) }
                }
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
