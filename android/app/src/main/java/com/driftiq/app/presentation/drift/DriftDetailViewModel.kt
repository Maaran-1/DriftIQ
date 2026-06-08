package com.driftiq.app.presentation.drift

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driftiq.app.domain.model.DriftHistoryEntry
import com.driftiq.app.domain.model.DriftScore
import com.driftiq.app.domain.usecase.GetDriftHistoryUseCase
import com.driftiq.app.domain.usecase.GetDriftTodayUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DriftDetailUiState(
    val isLoading: Boolean = true,
    val today: DriftScore? = null,
    val history: List<DriftHistoryEntry> = emptyList(),
    val selectedPeriodDays: Int = 30,
    val error: String? = null,
)

@HiltViewModel
class DriftDetailViewModel @Inject constructor(
    private val getDriftToday: GetDriftTodayUseCase,
    private val getDriftHistory: GetDriftHistoryUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DriftDetailUiState())
    val uiState: StateFlow<DriftDetailUiState> = _uiState

    init { load() }

    fun load(days: Int = 30) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, selectedPeriodDays = days)
            val todayResult = getDriftToday()
            val historyResult = getDriftHistory(days)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                today = todayResult.getOrNull(),
                history = historyResult.getOrNull() ?: emptyList(),
                error = todayResult.exceptionOrNull()?.message,
            )
        }
    }

    fun changePeriod(days: Int) = load(days)
}
