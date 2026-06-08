package com.driftiq.app.data.repository

import com.driftiq.app.data.local.datastore.UserPreferencesDataStore
import com.driftiq.app.data.local.db.dao.AppUsageEventDao
import com.driftiq.app.data.remote.DriftIQApiService
import com.driftiq.app.data.remote.dto.AppUsageEventDto
import com.driftiq.app.data.remote.dto.BatchEventsRequest
import com.driftiq.app.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepositoryImpl @Inject constructor(
    private val api: DriftIQApiService,
    private val eventDao: AppUsageEventDao,
    private val dataStore: UserPreferencesDataStore,
) : EventRepository {

    private val isoFormatter = DateTimeFormatter.ISO_INSTANT

    override suspend fun syncPendingEvents(deviceId: String): Result<Int> = runCatching {
        val unsyncedEntities = eventDao.getUnsynced()
        if (unsyncedEntities.isEmpty()) return@runCatching 0

        val dtos = unsyncedEntities.map { entity ->
            AppUsageEventDto(
                packageName = entity.packageName,
                sessionStart = isoFormatter.format(Instant.ofEpochMilli(entity.sessionStart)),
                sessionEnd = isoFormatter.format(Instant.ofEpochMilli(entity.sessionEnd)),
                durationSeconds = entity.durationSeconds,
            )
        }

        val response = api.postEventsBatch(BatchEventsRequest(deviceId, dtos))
        if (response.isSuccessful) {
            val ids = unsyncedEntities.map { it.id }
            eventDao.markSynced(ids)
            response.body()?.accepted ?: 0
        } else {
            throw Exception("Sync failed: ${response.code()}")
        }
    }

    override fun getPendingEventCount(): Flow<Int> = eventDao.getPendingCount()
}
