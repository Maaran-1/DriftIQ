package com.driftiq.app.presentation.onboarding

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driftiq.app.data.local.datastore.UserPreferencesDataStore
import com.driftiq.app.domain.model.DigitalTwin
import com.driftiq.app.domain.usecase.GetTwinStatusUseCase
import com.driftiq.app.service.UsageStatsCollector
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: UserPreferencesDataStore,
    private val usageStatsCollector: UsageStatsCollector,
    private val getTwinStatus: GetTwinStatusUseCase,
) : ViewModel() {

    private val _usagePermissionGranted = MutableStateFlow(false)
    val usagePermissionGranted: StateFlow<Boolean> = _usagePermissionGranted

    private val _twinStatus = MutableStateFlow<DigitalTwin?>(null)
    val twinStatus: StateFlow<DigitalTwin?> = _twinStatus

    init {
        checkUsagePermission()
        loadTwinStatus()
    }

    fun checkUsagePermission() {
        _usagePermissionGranted.value = usageStatsCollector.hasUsageStatsPermission()
    }

    fun openUsageStatsSettings() {
        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun requestNotificationPermission() {
        // Request POST_NOTIFICATIONS permission — handled by Compose permission API
    }

    fun setConsentGiven() {
        viewModelScope.launch { dataStore.setConsentGiven(true) }
    }

    fun completeOnboarding() {
        viewModelScope.launch { dataStore.setOnboardingComplete(true) }
    }

    private fun loadTwinStatus() {
        viewModelScope.launch {
            getTwinStatus().onSuccess { _twinStatus.value = it }
        }
    }
}
