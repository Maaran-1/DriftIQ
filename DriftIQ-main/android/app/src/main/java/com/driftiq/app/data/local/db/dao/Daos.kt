package com.driftiq.app.data.local.db.dao

import androidx.room.*
import com.driftiq.app.data.local.db.entity.AppUsageEventEntity
import com.driftiq.app.data.local.db.entity.CachedDashboardEntity
import com.driftiq.app.data.local.db.entity.CachedInsightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppUsageEventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(events: List<AppUsageEventEntity>)

    @Query("SELECT * FROM app_usage_events WHERE is_synced = 0 ORDER BY session_start ASC LIMIT 500")
    suspend fun getUnsynced(): List<AppUsageEventEntity>

    @Query("UPDATE app_usage_events SET is_synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM app_usage_events WHERE is_synced = 0")
    fun getPendingCount(): Flow<Int>

    @Query("DELETE FROM app_usage_events WHERE created_at < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long)
}

@Dao
interface CachedDashboardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CachedDashboardEntity)

    @Query("SELECT * FROM cached_dashboard WHERE id = 1")
    suspend fun getCached(): CachedDashboardEntity?
}

@Dao
interface CachedInsightDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CachedInsightEntity)

    @Query("SELECT * FROM cached_insights WHERE insightType = :type")
    suspend fun getCached(type: String): CachedInsightEntity?
}
