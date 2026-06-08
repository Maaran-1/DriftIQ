package com.driftiq.app.presentation.risk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driftiq.app.domain.model.RiskRecord
import com.driftiq.app.domain.usecase.GetCurrentRiskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RiskViewModel @Inject constructor(
    private val getCurrentRisk: GetCurrentRiskUseCase,
) : ViewModel() {
    private val _risk = MutableStateFlow<RiskRecord?>(null)
    val risk: StateFlow<RiskRecord?> = _risk
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init { load() }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            getCurrentRisk().onSuccess { _risk.value = it }
            _isLoading.value = false
        }
    }
}
