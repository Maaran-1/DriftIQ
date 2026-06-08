package com.driftiq.app.presentation.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driftiq.app.domain.model.DriftHistoryEntry
import com.driftiq.app.domain.model.RiskRecord
import com.driftiq.app.domain.usecase.GetDriftHistoryUseCase
import com.driftiq.app.domain.usecase.GetCurrentRiskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TimelineUiState(
    val isLoading: Boolean = true,
    val entries: List<DriftHistoryEntry> = emptyList(),
    val currentRisk: RiskRecord? = null,
    val selectedDays: Int = 30,
)

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val getDriftHistory: GetDriftHistoryUseCase,
    private val getCurrentRisk: GetCurrentRiskUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TimelineUiState())
    val uiState: StateFlow<TimelineUiState> = _uiState

    init { load() }

    fun load(days: Int = 30) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, selectedDays = days)
            val history = getDriftHistory(days).getOrNull() ?: emptyList()
            val risk = getCurrentRisk().getOrNull()
            _uiState.value = _uiState.value.copy(isLoading = false, entries = history, currentRisk = risk)
        }
    }
}
