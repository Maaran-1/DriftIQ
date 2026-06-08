package com.driftiq.app.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_usage_events")
data class AppUsageEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "session_start") val sessionStart: Long, // epoch millis
    @ColumnInfo(name = "session_end") val sessionEnd: Long,
    @ColumnInfo(name = "duration_seconds") val durationSeconds: Int,
    @ColumnInfo(name = "category") val category: String?,
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "is_synced") val isSynced: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "cached_dashboard")
data class CachedDashboardEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "wellness_score") val wellnessScore: Int,
    @ColumnInfo(name = "drift_score") val driftScore: Float,
    @ColumnInfo(name = "risk_level") val riskLevel: Int,
    @ColumnInfo(name = "risk_label") val riskLabel: String,
    @ColumnInfo(name = "baseline_active") val baselineActive: Boolean,
    @ColumnInfo(name = "calibration_progress") val calibrationProgress: Float,
    @ColumnInfo(name = "baseline_days") val baselineDays: Int,
    @ColumnInfo(name = "highlights_json") val highlightsJson: String,
    @ColumnInfo(name = "cached_at") val cachedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "cached_insights")
data class CachedInsightEntity(
    @PrimaryKey val insightType: String, // daily | weekly | monthly
    val content: String,
    @ColumnInfo(name = "wellness_score") val wellnessScore: Int,
    @ColumnInfo(name = "risk_level") val riskLevel: Int,
    @ColumnInfo(name = "period_start") val periodStart: String,
    @ColumnInfo(name = "period_end") val periodEnd: String,
    @ColumnInfo(name = "cached_at") val cachedAt: Long = System.currentTimeMillis(),
)
