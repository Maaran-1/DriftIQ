package com.driftiq.app.domain.repository

import com.driftiq.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface AuthRepository {
    suspend fun register(email: String, password: String): Result<Unit>
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun logout(): Result<Unit>
    suspend fun refreshToken(): Result<Unit>
    fun isLoggedIn(): Boolean
}

interface EventRepository {
    suspend fun syncPendingEvents(deviceId: String): Result<Int>
    fun getPendingEventCount(): Flow<Int>
}

interface DashboardRepository {
    suspend fun getDashboardSummary(): Result<DashboardSummary>
}

interface DriftRepository {
    suspend fun getDriftToday(): Result<DriftScore>
    suspend fun getDriftHistory(days: Int = 30): Result<List<DriftHistoryEntry>>
}

interface RiskRepository {
    suspend fun getCurrentRisk(): Result<RiskRecord>
    suspend fun getRiskHistory(days: Int = 30): Result<List<RiskRecord>>
}

interface InsightRepository {
    suspend fun getDailyInsight(): Result<Insight>
    suspend fun getWeeklyInsight(): Result<Insight>
    suspend fun getMonthlyInsight(): Result<Insight>
}

interface TwinRepository {
    suspend fun getTwinStatus(): Result<DigitalTwin>
    suspend fun resetTwin(): Result<Unit>
}

interface SettingsRepository {
    suspend fun updateFcmToken(token: String): Result<Unit>
    suspend fun exportData(): Result<String>
    suspend fun deleteAccount(password: String): Result<Unit>
    suspend fun updateDriftAlertThreshold(threshold: Float): Result<Unit>
}
