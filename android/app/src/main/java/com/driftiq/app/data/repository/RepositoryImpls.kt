package com.driftiq.app.data.repository

import com.driftiq.app.data.remote.DriftIQApiService
import com.driftiq.app.domain.model.*
import com.driftiq.app.domain.repository.*
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepositoryImpl @Inject constructor(private val api: DriftIQApiService) : DashboardRepository {
    override suspend fun getDashboardSummary(): Result<DashboardSummary> = runCatching {
        val r = api.getDashboardSummary()
        if (!r.isSuccessful) throw Exception("Dashboard fetch failed: ${r.code()}")
        val b = r.body()!!
        DashboardSummary(
            wellnessScore = b.wellnessScore,
            driftScore = b.driftScore,
            riskLevel = b.riskLevel,
            riskLabel = b.riskLabel,
            baselineActive = b.baselineActive,
            calibrationProgress = b.calibrationProgress,
            baselineDays = b.baselineDays,
            highlights = b.highlights.map { Highlight(it.type, it.message) },
        )
    }
}

@Singleton
class DriftRepositoryImpl @Inject constructor(private val api: DriftIQApiService) : DriftRepository {
    override suspend fun getDriftToday(): Result<DriftScore> = runCatching {
        val r = api.getDriftToday()
        if (!r.isSuccessful) throw Exception("Drift fetch failed")
        val b = r.body()!!
        DriftScore(
            scoreDate = LocalDate.parse(b.date),
            compositeDrift = b.compositeDrift,
            driftVelocity = b.driftVelocity,
            dimensionZScores = b.dimensionScores.mapValues { it.value.zScore },
            topContributors = b.topContributors,
            explanation = b.explanation,
        )
    }

    override suspend fun getDriftHistory(days: Int): Result<List<DriftHistoryEntry>> = runCatching {
        val r = api.getDriftHistory(days)
        if (!r.isSuccessful) throw Exception("History fetch failed")
        r.body()!!.history.map {
            DriftHistoryEntry(
                date = LocalDate.parse(it.date),
                compositeDrift = it.compositeDrift,
                riskLevel = it.riskLevel,
            )
        }
    }
}

@Singleton
class RiskRepositoryImpl @Inject constructor(private val api: DriftIQApiService) : RiskRepository {
    override suspend fun getCurrentRisk(): Result<RiskRecord> = runCatching {
        val r = api.getCurrentRisk()
        if (!r.isSuccessful) throw Exception("Risk fetch failed")
        val b = r.body()!!
        RiskRecord(
            recordDate = LocalDate.now(),
            riskLevel = b.level,
            riskLabel = b.label,
            explanation = b.explanation,
            trend = b.trend,
            color = b.color,
            sustainedDays = b.sustainedDays,
        )
    }

    override suspend fun getRiskHistory(days: Int): Result<List<RiskRecord>> = runCatching {
        val r = api.getRiskHistory(days)
        if (!r.isSuccessful) throw Exception("Risk history fetch failed")
        r.body()!!.history.map {
            RiskRecord(
                recordDate = LocalDate.parse(it.date),
                riskLevel = it.level,
                riskLabel = it.label,
                explanation = "",
                trend = "stable",
                color = "#2ECC71",
                sustainedDays = 0,
            )
        }
    }
}

@Singleton
class InsightRepositoryImpl @Inject constructor(private val api: DriftIQApiService) : InsightRepository {
    override suspend fun getDailyInsight(): Result<Insight> = runCatching {
        val r = api.getDailyInsight()
        if (!r.isSuccessful) throw Exception("Insight fetch failed")
        val b = r.body()!!
        Insight(
            id = b.insightId, type = "daily",
            periodStart = LocalDate.parse(b.date), periodEnd = LocalDate.parse(b.date),
            content = b.content, wellnessScore = b.wellnessScore, riskLevel = b.riskLevel,
        )
    }

    override suspend fun getWeeklyInsight(): Result<Insight> = runCatching {
        val r = api.getWeeklyInsight()
        if (!r.isSuccessful) throw Exception("Weekly insight fetch failed")
        val b = r.body()!!
        Insight(
            id = b.insightId, type = "weekly",
            periodStart = LocalDate.parse(b.periodStart), periodEnd = LocalDate.parse(b.periodEnd),
            content = b.content, wellnessScore = b.wellnessScore, riskLevel = 0,
        )
    }

    override suspend fun getMonthlyInsight(): Result<Insight> = runCatching {
        val r = api.getMonthlyInsight()
        if (!r.isSuccessful) throw Exception("Monthly insight fetch failed")
        val b = r.body()!!
        Insight(
            id = b.insightId, type = "monthly",
            periodStart = LocalDate.parse(b.periodStart), periodEnd = LocalDate.parse(b.periodEnd),
            content = b.content, wellnessScore = b.wellnessScore, riskLevel = 0,
        )
    }
}

@Singleton
class TwinRepositoryImpl @Inject constructor(private val api: DriftIQApiService) : TwinRepository {
    override suspend fun getTwinStatus(): Result<DigitalTwin> = runCatching {
        val r = api.getTwinStatus()
        if (!r.isSuccessful) throw Exception("Twin fetch failed")
        val b = r.body()!!
        DigitalTwin(
            isActive = b.isActive,
            baselineDays = b.baselineDays,
            confidenceScore = b.confidenceScore,
            calibrationProgress = b.calibrationProgress,
            dimensions = emptyMap(),
        )
    }

    override suspend fun resetTwin(): Result<Unit> = runCatching {
        val r = api.resetTwin()
        if (!r.isSuccessful) throw Exception("Twin reset failed")
    }
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(private val api: DriftIQApiService) : SettingsRepository {
    override suspend fun updateFcmToken(token: String): Result<Unit> = runCatching {
        api.updateSettings(com.driftiq.app.data.remote.dto.UpdateSettingsRequest(fcmToken = token))
    }
    override suspend fun exportData(): Result<String> = runCatching {
        val r = api.exportData()
        r.body()?.get("download_url") ?: "pending"
    }
    override suspend fun deleteAccount(password: String): Result<Unit> = runCatching {
        api.deleteAccount(mapOf("password" to password, "confirm" to true))
    }
    override suspend fun updateDriftAlertThreshold(threshold: Float): Result<Unit> = runCatching {
        api.updateSettings(com.driftiq.app.data.remote.dto.UpdateSettingsRequest(driftAlertThreshold = threshold))
    }
}
