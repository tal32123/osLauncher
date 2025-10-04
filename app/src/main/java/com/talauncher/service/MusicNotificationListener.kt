package com.talauncher.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/**
 * NotificationListenerService required for accessing MediaSession APIs.
 * This service allows the app to monitor media playback from other apps
 * through MediaSessionManager.
 */
class MusicNotificationListener : NotificationListenerService() {
    private val TAG = "MusicNotificationListener"

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "Notification listener disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // We don't need to actively monitor notifications
        // This service is primarily used to grant access to MediaSessionManager
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // We don't need to actively monitor notifications
        // This service is primarily used to grant access to MediaSessionManager
    }
}
