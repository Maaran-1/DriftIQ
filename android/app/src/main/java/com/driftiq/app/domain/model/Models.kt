package com.driftiq.app.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

data class AppUsageEvent(
    val packageName: String,
    val sessionStart: LocalDateTime,
    val sessionEnd: LocalDateTime,
    val durationSeconds: Int,
    val category: String? = null,
    val deviceId: String? = null,
)

data class DailyFeatureVector(
    val featureDate: LocalDate,
    val isWeekend: Boolean,
    val totalScreenTimeMinutes: Float,
    val unlockCount: Int,
    val uniqueAppsUsed: Int,
    val socialMinutes: Float,
    val productivityMinutes: Float,
    val entertainmentMinutes: Float,
    val learningMinutes: Float,
    val sleepEstimateHours: Float,
    val peakUsageHour: Int,
    val usageEntropy: Float,
    val lateNightMinutes: Float,
    val morningMinutes: Float,
    val sessionCount: Int,
    val avgSessionDuration: Float,
    val notificationCount: Int,
)

data class DriftScore(
    val scoreDate: LocalDate,
    val compositeDrift: Float,
    val driftVelocity: Float,
    val dimensionZScores: Map<String, Float>,
    val topContributors: List<String>,
    val explanation: String,
)

data class DimensionScore(
    val zScore: Float,
    val direction: String, // increase | decrease | stable
    val value: Float,
    val baseline: Float,
)

data class RiskRecord(
    val recordDate: LocalDate,
    val riskLevel: Int,
    val riskLabel: String,
    val explanation: String,
    val trend: String, // improving | stable | worsening
    val color: String,
    val sustainedDays: Int,
)

data class Insight(
    val id: String,
    val type: String, // daily | weekly | monthly
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val content: String,
    val wellnessScore: Int,
    val riskLevel: Int,
)

data class DigitalTwin(
    val isActive: Boolean,
    val baselineDays: Int,
    val confidenceScore: Float,
    val calibrationProgress: Float,
    val dimensions: Map<String, BaselineDimension>,
)

data class BaselineDimension(
    val weekdayMean: Float,
    val weekdayStd: Float,
    val weekendMean: Float,
    val weekendStd: Float,
    val confidence: Float,
    val nSamples: Int,
)

data class DashboardSummary(
    val wellnessScore: Int,
    val driftScore: Float,
    val riskLevel: Int,
    val riskLabel: String,
    val baselineActive: Boolean,
    val calibrationProgress: Float,
    val baselineDays: Int,
    val highlights: List<Highlight>,
)

data class Highlight(
    val type: String,
    val message: String,
)

data class DriftHistoryEntry(
    val date: LocalDate,
    val compositeDrift: Float,
    val riskLevel: Int,
)

// Risk colors matching spec Appendix C
object RiskColors {
    fun forLevel(level: Int): Long = when (level) {
        0 -> 0xFF2ECC71L
        1 -> 0xFFF1C40FL
        2 -> 0xFFE67E22L
        3 -> 0xFFE74C3CL
        4 -> 0xFFC0392BL
        5 -> 0xFF7B241CL
        else -> 0xFF2ECC71L
    }
}
