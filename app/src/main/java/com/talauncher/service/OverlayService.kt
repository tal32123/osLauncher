package com.talauncher.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import com.talauncher.ui.components.SessionExpiryActionDialog
import com.talauncher.ui.components.SessionExpiryCountdownDialog
import com.talauncher.ui.theme.TALauncherTheme
import com.talauncher.MainActivity
import com.talauncher.R

class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isForegroundServiceRunning = false
    private val notificationManager: NotificationManager? by lazy {
        getSystemService(NotificationManager::class.java)
    }

    companion object {
        const val ACTION_SHOW_COUNTDOWN = "show_countdown"
        const val ACTION_SHOW_DECISION = "show_decision"
        const val ACTION_HIDE_OVERLAY = "hide_overlay"
        const val EXTRA_APP_NAME = "app_name"
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_REMAINING_SECONDS = "remaining_seconds"
        const val EXTRA_TOTAL_SECONDS = "total_seconds"
        const val EXTRA_SHOW_MATH_OPTION = "show_math_option"
        private const val NOTIFICATION_CHANNEL_ID = "session_overlay_channel"
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
                val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "this app"
                val remainingSeconds = intent.getIntExtra(EXTRA_REMAINING_SECONDS, 0)
                val totalSeconds = intent.getIntExtra(EXTRA_TOTAL_SECONDS, 0)
                showCountdownOverlay(appName, remainingSeconds, totalSeconds)
            }
            ACTION_SHOW_DECISION -> {
                val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "this app"
                val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
                val showMathOption = intent.getBooleanExtra(EXTRA_SHOW_MATH_OPTION, false)
                showDecisionOverlay(appName, packageName, showMathOption)
            }
            ACTION_HIDE_OVERLAY -> {
                hideOverlay()
                stopForegroundService()
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = getString(R.string.session_overlay_notification_channel_name)
            val channelDescription = getString(
                R.string.session_overlay_notification_channel_description,
                getString(R.string.app_name)
            )
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = channelDescription
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundWithNotification(appName: String) {
        val notification = buildOverlayNotification(appName)
        startForeground(NOTIFICATION_ID, notification)
        isForegroundServiceRunning = true
    }

    private fun buildOverlayNotification(appName: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = getString(R.string.session_overlay_notification_title)
        val launcherName = getString(R.string.app_name)
        val text = getString(R.string.session_overlay_notification_text, launcherName, appName)
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_overlay_notification)
            .setContentIntent(pendingIntent)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setShowWhen(false)
            .build()
    }

    private fun stopForegroundInternal() {
        if (!isForegroundServiceRunning) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        isForegroundServiceRunning = false
    }

    private fun stopForegroundService() {
        stopForegroundInternal()
        stopSelf()
    }

    private fun showCountdownOverlay(appName: String, remainingSeconds: Int, totalSeconds: Int) {
        if (!Settings.canDrawOverlays(this)) {
            hideOverlay()
            stopForegroundService()
            return
        }

        hideOverlay()
        startForegroundWithNotification(appName)

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
            hideOverlay()
            stopForegroundService()
            return
        }

        hideOverlay()
        startForegroundWithNotification(appName)

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
                        hideOverlay()
                        stopForegroundService()
                    },
                    onClose = {
                        // Send broadcast to notify HomeViewModel
                        val intent = Intent("com.talauncher.SESSION_EXPIRY_CLOSE")
                        intent.putExtra("package_name", packageName)
                        sendBroadcast(intent)
                        hideOverlay()
                        stopForegroundService()
                    },
                    onMathChallenge = if (showMathOption) {
                        {
                            // Send broadcast to notify HomeViewModel
                            val intent = Intent("com.talauncher.SESSION_EXPIRY_MATH_CHALLENGE")
                            intent.putExtra("package_name", packageName)
                            sendBroadcast(intent)
                            hideOverlay()
                            stopForegroundService()
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

    private fun hideOverlay() {
        overlayView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        overlayView = null
    }

    override fun onDestroy() {
        hideOverlay()
        stopForegroundInternal()
        super.onDestroy()
    }
}