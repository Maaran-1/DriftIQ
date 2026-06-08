package com.driftiq.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class DriftIQApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Create notification channels
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
