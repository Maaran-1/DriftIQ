package com.driftiq.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driftiq.app.domain.model.DashboardSummary
import com.driftiq.app.domain.usecase.GetDashboardSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// MVI State
sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Ready(val summary: DashboardSummary) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getDashboardSummary: GetDashboardSummaryUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            getDashboardSummary()
                .onSuccess { _uiState.value = HomeUiState.Ready(it) }
                .onFailure { _uiState.value = HomeUiState.Error(it.message ?: "Unknown error") }
        }
    }
}
