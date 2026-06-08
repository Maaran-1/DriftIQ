package com.driftiq.app.data.remote.dto

import com.google.gson.annotations.SerializedName

// Auth
data class RegisterRequest(val email: String, val password: String)
data class LoginRequest(val email: String, val password: String)
data class RefreshRequest(@SerializedName("refresh_token") val refreshToken: String)
data class LogoutRequest(@SerializedName("refresh_token") val refreshToken: String)

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("expires_in") val expiresIn: Int,
)

data class AccessTokenResponse(
    @SerializedName("access_token") val accessToken: String,
)

// Events
data class AppUsageEventDto(
    @SerializedName("package_name") val packageName: String,
    @SerializedName("session_start") val sessionStart: String,
    @SerializedName("session_end") val sessionEnd: String,
    @SerializedName("duration_seconds") val durationSeconds: Int,
)

data class BatchEventsRequest(
    @SerializedName("device_id") val deviceId: String,
    val events: List<AppUsageEventDto>,
)

data class BatchEventsResponse(
    val accepted: Int,
    @SerializedName("duplicate_skipped") val duplicateSkipped: Int,
    @SerializedName("invalid_skipped") val invalidSkipped: Int,
)

// Dashboard
data class DashboardSummaryResponse(
    @SerializedName("wellness_score") val wellnessScore: Int,
    @SerializedName("drift_score") val driftScore: Float,
    @SerializedName("risk_level") val riskLevel: Int,
    @SerializedName("risk_label") val riskLabel: String,
    @SerializedName("baseline_active") val baselineActive: Boolean,
    @SerializedName("calibration_progress") val calibrationProgress: Float,
    @SerializedName("baseline_days") val baselineDays: Int,
    val highlights: List<HighlightDto>,
    @SerializedName("last_updated") val lastUpdated: String?,
)

data class HighlightDto(val type: String, val message: String)

// Drift
data class DriftTodayResponse(
    val date: String,
    @SerializedName("composite_drift") val compositeDrift: Float,
    @SerializedName("drift_velocity") val driftVelocity: Float,
    @SerializedName("dimension_scores") val dimensionScores: Map<String, DimensionScoreDto>,
    @SerializedName("top_contributors") val topContributors: List<String>,
    val explanation: String,
)

data class DimensionScoreDto(
    @SerializedName("z_score") val zScore: Float,
    val direction: String,
    val value: Float,
    val baseline: Float,
)

data class DriftHistoryResponse(val history: List<DriftHistoryEntryDto>)
data class DriftHistoryEntryDto(
    val date: String,
    @SerializedName("composite_drift") val compositeDrift: Float,
    @SerializedName("risk_level") val riskLevel: Int,
)

// Risk
data class RiskCurrentResponse(
    val level: Int,
    val label: String,
    val explanation: String,
    val trend: String,
    @SerializedName("sustained_days") val sustainedDays: Int,
    val color: String,
)

data class RiskHistoryResponse(val history: List<RiskHistoryEntryDto>)
data class RiskHistoryEntryDto(val date: String, val level: Int, val label: String)

// Insights
data class DailyInsightResponse(
    @SerializedName("insight_id") val insightId: String,
    val type: String,
    val date: String,
    val content: String,
    @SerializedName("wellness_score") val wellnessScore: Int,
    @SerializedName("risk_level") val riskLevel: Int,
)

data class WeeklyInsightResponse(
    @SerializedName("insight_id") val insightId: String,
    @SerializedName("period_start") val periodStart: String,
    @SerializedName("period_end") val periodEnd: String,
    val content: String,
    @SerializedName("wellness_score") val wellnessScore: Int,
)

// Twin
data class TwinStatusResponse(
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("baseline_days") val baselineDays: Int,
    @SerializedName("confidence_score") val confidenceScore: Float,
    @SerializedName("calibration_progress") val calibrationProgress: Float,
)

// Settings
data class UpdateSettingsRequest(
    @SerializedName("drift_alert_threshold") val driftAlertThreshold: Float? = null,
    @SerializedName("notifications_enabled") val notificationsEnabled: Boolean? = null,
    @SerializedName("fcm_token") val fcmToken: String? = null,
)
