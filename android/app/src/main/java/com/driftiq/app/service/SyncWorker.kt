package com.driftiq.app.service

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.driftiq.app.data.local.datastore.UserPreferencesDataStore
import com.driftiq.app.domain.usecase.SyncEventsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that syncs locally-buffered usage events to the backend.
 *
 * Runs every 15 minutes (minimum WorkManager interval) when:
 *   - Network is available (CONNECTED constraint)
 *   - User is logged in (has an access token)
 *
 * Survives app restarts and device reboots via WorkManager's persistent job queue.
 * Uses KEEP policy so duplicate enqueues don't create parallel syncs.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncEventsUseCase: SyncEventsUseCase,
    private val dataStore: UserPreferencesDataStore,
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "SyncWorker"
        private const val WORK_NAME = "driftiq_sync_events"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(
                15, TimeUnit.MINUTES,
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS,
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
            Log.i(TAG, "SyncWorker scheduled")
        }

        /**
         * Trigger an immediate one-shot sync (e.g. when app comes to foreground).
         */
        fun runNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }

    override suspend fun doWork(): Result {
        val token = dataStore.accessToken.firstOrNull()
        if (token.isNullOrBlank()) {
            Log.d(TAG, "No auth token — skipping sync")
            return Result.success()
        }

        val deviceId = dataStore.deviceId.firstOrNull()
            ?: android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID,
            )
            ?: "unknown"

        return syncEventsUseCase(deviceId)
            .onSuccess { accepted ->
                Log.i(TAG, "Sync complete — $accepted events accepted by backend")
            }
            .onFailure { e ->
                Log.e(TAG, "Sync failed: ${e.message}", e)
            }
            .fold(
                onSuccess = { Result.success() },
                onFailure = { Result.retry() },
            )
    }
}
