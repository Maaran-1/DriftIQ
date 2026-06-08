package com.driftiq.app.service

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class DriftIQFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        // Handle push notification — show a notification to the user
        val title = message.notification?.title ?: "DriftIQ Alert"
        val body = message.notification?.body ?: "Your behavioral patterns have been updated."
        showNotification(title, body)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Token updated — save and sync to backend
        // In production: call settings repository to POST new FCM token
    }

    private fun showNotification(title: String, body: String) {
        // Implementation: create NotificationCompat.Builder and display
        // Channels are created in Application class
    }
}
