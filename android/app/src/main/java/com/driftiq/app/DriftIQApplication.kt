package com.driftiq.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * Application class.
 *
 * Implements [Configuration.Provider] to inject HiltWorkerFactory into WorkManager.
 * This is required for Hilt-injected WorkManager workers (e.g. SyncWorker).
 * WorkManager must NOT be auto-initialised (see AndroidManifest.xml) when using
 * a custom factory — the provider pattern handles lazy initialisation.
 */
@HiltAndroidApp
class DriftIQApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(NotificationManager::class.java)

        listOf(
            NotificationChannel(
                "driftiq_collection",
                "Data Collection",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "Background behavioral data collection" },

            NotificationChannel(
                "driftiq_alerts",
                "Behavioral Alerts",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "Drift alerts and risk level changes" },

            NotificationChannel(
                "driftiq_insights",
                "Insights",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Weekly and daily behavioral insights" },
        ).forEach { nm.createNotificationChannel(it) }
    }
}
