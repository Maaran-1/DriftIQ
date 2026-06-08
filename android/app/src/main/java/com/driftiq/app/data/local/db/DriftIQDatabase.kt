package com.driftiq.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.driftiq.app.data.local.db.dao.AppUsageEventDao
import com.driftiq.app.data.local.db.dao.CachedDashboardDao
import com.driftiq.app.data.local.db.dao.CachedInsightDao
import com.driftiq.app.data.local.db.entity.*

@Database(
    entities = [
        AppUsageEventEntity::class,
        CachedDashboardEntity::class,
        CachedInsightEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class DriftIQDatabase : RoomDatabase() {
    abstract fun appUsageEventDao(): AppUsageEventDao
    abstract fun cachedDashboardDao(): CachedDashboardDao
    abstract fun cachedInsightDao(): CachedInsightDao
}
