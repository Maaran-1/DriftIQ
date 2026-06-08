package com.driftiq.app.domain.usecase

import com.driftiq.app.domain.model.DashboardSummary
import com.driftiq.app.domain.model.DriftScore
import com.driftiq.app.domain.model.Insight
import com.driftiq.app.domain.model.RiskRecord
import com.driftiq.app.domain.repository.*
import javax.inject.Inject

class GetDashboardSummaryUseCase @Inject constructor(
    private val dashboardRepository: DashboardRepository
) {
    suspend operator fun invoke(): Result<DashboardSummary> =
        dashboardRepository.getDashboardSummary()
}

class GetDriftTodayUseCase @Inject constructor(
    private val driftRepository: DriftRepository
) {
    suspend operator fun invoke(): Result<DriftScore> =
        driftRepository.getDriftToday()
}

class GetDriftHistoryUseCase @Inject constructor(
    private val driftRepository: DriftRepository
) {
    suspend operator fun invoke(days: Int = 30): Result<List<com.driftiq.app.domain.model.DriftHistoryEntry>> =
        driftRepository.getDriftHistory(days)
}

class GetCurrentRiskUseCase @Inject constructor(
    private val riskRepository: RiskRepository
) {
    suspend operator fun invoke(): Result<RiskRecord> =
        riskRepository.getCurrentRisk()
}

class GetDailyInsightUseCase @Inject constructor(
    private val insightRepository: InsightRepository
) {
    suspend operator fun invoke(): Result<Insight> =
        insightRepository.getDailyInsight()
}

class GetWeeklyInsightUseCase @Inject constructor(
    private val insightRepository: InsightRepository
) {
    suspend operator fun invoke(): Result<Insight> =
        insightRepository.getWeeklyInsight()
}

class GetMonthlyInsightUseCase @Inject constructor(
    private val insightRepository: InsightRepository
) {
    suspend operator fun invoke(): Result<Insight> =
        insightRepository.getMonthlyInsight()
}

class SyncEventsUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(deviceId: String): Result<Int> =
        eventRepository.syncPendingEvents(deviceId)
}

class GetTwinStatusUseCase @Inject constructor(
    private val twinRepository: TwinRepository
) {
    suspend operator fun invoke() = twinRepository.getTwinStatus()
}

class ResetTwinUseCase @Inject constructor(
    private val twinRepository: TwinRepository
) {
    suspend operator fun invoke() = twinRepository.resetTwin()
}
