package com.driftiq.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.driftiq.app.R
import com.driftiq.app.data.local.db.dao.AppUsageEventDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * Foreground service that runs continuously to:
 *   1. Collect Android usage events every 15 minutes via UsageStatsCollector
 *   2. Persist them to Room (local buffer)
 *   3. Trigger WorkManager SyncWorker to upload buffered events to the backend
 *
 * The service runs in the foreground to ensure the OS does not kill it.
 * WorkManager handles the actual network sync independently, so sync survives
 * even if this service is temporarily stopped.
 */
@AndroidEntryPoint
class CollectionService : Service() {

    @Inject lateinit var usageStatsCollector: UsageStatsCollector
    @Inject lateinit var eventDao: AppUsageEventDao

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var collectionJob: Job? = null

    companion object {
        private const val TAG = "CollectionService"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "driftiq_collection"
        const val POLL_INTERVAL_MS = 15 * 60 * 1000L // 15 minutes

        fun start(context: Context) {
            val intent = Intent(context, CollectionService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CollectionService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        Log.i(TAG, "CollectionService started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startCollection()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        Log.i(TAG, "CollectionService stopped")
        super.onDestroy()
    }

    private fun startCollection() {
        collectionJob?.cancel()
        collectionJob = serviceScope.launch {
            while (isActive) {
                collectAndPersist()
                // Trigger sync after collection — WorkManager handles network constraint
                SyncWorker.runNow(this@CollectionService)
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun collectAndPersist() {
        try {
            if (!usageStatsCollector.hasUsageStatsPermission()) {
                Log.w(TAG, "Usage stats permission not granted — skipping collection cycle")
                return
            }
            val events = usageStatsCollector.collectRecentEvents()
            if (events.isNotEmpty()) {
                eventDao.insertAll(events)
                Log.i(TAG, "Collected and persisted ${events.size} usage events")
            } else {
                Log.v(TAG, "No new usage events in this collection window")
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Usage stats permission revoked during collection: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error during usage stats collection", e)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_collection),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "DriftIQ background data collection"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val mainIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_collection_title))
            .setContentText(getString(R.string.notification_collection_body))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
