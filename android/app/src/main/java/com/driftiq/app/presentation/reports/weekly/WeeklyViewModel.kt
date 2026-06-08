package com.driftiq.app.presentation.reports.weekly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driftiq.app.domain.model.DriftHistoryEntry
import com.driftiq.app.domain.model.Insight
import com.driftiq.app.domain.usecase.GetDriftHistoryUseCase
import com.driftiq.app.domain.usecase.GetWeeklyInsightUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WeeklyUiState(
    val isLoading: Boolean = true,
    val insight: Insight? = null,
    val history: List<DriftHistoryEntry> = emptyList(),
)

@HiltViewModel
class WeeklyViewModel @Inject constructor(
    private val getWeeklyInsight: GetWeeklyInsightUseCase,
    private val getDriftHistory: GetDriftHistoryUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(WeeklyUiState())
    val uiState: StateFlow<WeeklyUiState> = _uiState

    init {
        viewModelScope.launch {
            val insight = getWeeklyInsight().getOrNull()
            val history = getDriftHistory(7).getOrNull() ?: emptyList()
            _uiState.value = WeeklyUiState(isLoading = false, insight = insight, history = history)
        }
    }
}
