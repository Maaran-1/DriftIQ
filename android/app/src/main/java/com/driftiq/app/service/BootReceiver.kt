package com.driftiq.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            // Restart foreground collection service
            CollectionService.start(context)
            // Re-schedule periodic sync worker (WorkManager jobs survive reboots,
            // but we reschedule defensively in case the job was cancelled)
            SyncWorker.schedule(context)
        }
    }
}
