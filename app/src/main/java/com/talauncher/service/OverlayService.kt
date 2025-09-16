package com.talauncher.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import com.talauncher.ui.components.SessionExpiryActionDialog
import com.talauncher.ui.components.SessionExpiryCountdownDialog
import com.talauncher.ui.theme.TALauncherTheme
import com.talauncher.R

class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isForeground = false

    companion object {
        const val ACTION_SHOW_COUNTDOWN = "show_countdown"
        const val ACTION_SHOW_DECISION = "show_decision"
        const val ACTION_HIDE_OVERLAY = "hide_overlay"
        const val EXTRA_APP_NAME = "app_name"
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_REMAINING_SECONDS = "remaining_seconds"
        const val EXTRA_TOTAL_SECONDS = "total_seconds"
        const val EXTRA_SHOW_MATH_OPTION = "show_math_option"
        /**
         * Notification channel that keeps the overlay service in the foreground while the session
         * expiry dialogs are displayed. The channel uses a low importance level so that the
         * persistent notification stays unobtrusive while still explaining why TALauncher is
         * running in the foreground.
         */
        private const val NOTIFICATION_CHANNEL_ID = "session_expiry_overlay"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_COUNTDOWN -> {
                ensureForegroundNotification()
                val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "this app"
                val remainingSeconds = intent.getIntExtra(EXTRA_REMAINING_SECONDS, 0)
                val totalSeconds = intent.getIntExtra(EXTRA_TOTAL_SECONDS, 0)
                showCountdownOverlay(appName, remainingSeconds, totalSeconds)
            }
            ACTION_SHOW_DECISION -> {
                ensureForegroundNotification()
                val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "this app"
                val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
                val showMathOption = intent.getBooleanExtra(EXTRA_SHOW_MATH_OPTION, false)
                showDecisionOverlay(appName, packageName, showMathOption)
            }
            ACTION_HIDE_OVERLAY -> {
                dismissOverlay()
            }
        }
        return START_NOT_STICKY
    }

    private fun showCountdownOverlay(appName: String, remainingSeconds: Int, totalSeconds: Int) {
        if (!Settings.canDrawOverlays(this)) {
            stopForegroundService()
            return
        }

        removeOverlayView()

        val composeView = ComposeView(this)
        composeView.setContent {
            TALauncherTheme {
                SessionExpiryCountdownDialog(
                    appName = appName,
                    remainingSeconds = remainingSeconds,
                    totalSeconds = totalSeconds
                )
            }
        }

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        )

        layoutParams.gravity = Gravity.CENTER

        try {
            overlayView = composeView
            windowManager?.addView(overlayView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showDecisionOverlay(appName: String, packageName: String, showMathOption: Boolean) {
        if (!Settings.canDrawOverlays(this)) {
            stopForegroundService()
            return
        }

        removeOverlayView()

        val composeView = ComposeView(this)
        composeView.setContent {
            TALauncherTheme {
                SessionExpiryActionDialog(
                    appName = appName,
                    showMathChallengeOption = showMathOption,
                    onExtend = {
                        // Send broadcast to notify HomeViewModel
                        val intent = Intent("com.talauncher.SESSION_EXPIRY_EXTEND")
                        intent.putExtra("package_name", packageName)
                        sendBroadcast(intent)
                        dismissOverlay()
                    },
                    onClose = {
                        // Send broadcast to notify HomeViewModel
                        val intent = Intent("com.talauncher.SESSION_EXPIRY_CLOSE")
                        intent.putExtra("package_name", packageName)
                        sendBroadcast(intent)
                        dismissOverlay()
                    },
                    onMathChallenge = if (showMathOption) {
                        {
                            // Send broadcast to notify HomeViewModel
                            val intent = Intent("com.talauncher.SESSION_EXPIRY_MATH_CHALLENGE")
                            intent.putExtra("package_name", packageName)
                            sendBroadcast(intent)
                            dismissOverlay()
                        }
                    } else {
                        null
                    }
                )
            }
        }

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        )

        layoutParams.gravity = Gravity.CENTER

        try {
            overlayView = composeView
            windowManager?.addView(overlayView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeOverlayView() {
        overlayView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        overlayView = null
    }

    private fun dismissOverlay() {
        removeOverlayView()
        stopForegroundService()
    }

    private fun stopForegroundService() {
        if (isForeground) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            isForeground = false
        }
        stopSelf()
    }

    private fun ensureForegroundNotification() {
        if (isForeground) return
        val notification = buildForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)
        isForeground = true
    }

    private fun buildForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_text))
            .setCategory(Notification.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.overlay_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.overlay_notification_channel_description)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        removeOverlayView()
        if (isForeground) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            isForeground = false
        }
        super.onDestroy()
    }
}
