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
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.talauncher.R
import com.talauncher.ui.components.SessionExpiryActionDialog
import com.talauncher.ui.components.SessionExpiryCountdownDialog
import com.talauncher.ui.theme.TALauncherTheme

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
        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_CHANNEL_ID = "session_expiry_overlay"
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
                stopServiceAfterHiding()
            }
        }
        return START_NOT_STICKY
    }

    private fun showCountdownOverlay(appName: String, remainingSeconds: Int, totalSeconds: Int) {
        if (!Settings.canDrawOverlays(this)) {
            hideOverlay()
            stopServiceAfterHiding()
            return
        }

        hideOverlay()
        updateForegroundNotification(appName, remainingSeconds)

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
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
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
            overlayView = null
            stopServiceAfterHiding()
        }
    }

    private fun showDecisionOverlay(appName: String, packageName: String, showMathOption: Boolean) {
        if (!Settings.canDrawOverlays(this)) {
            hideOverlay()
            stopServiceAfterHiding()
            return
        }

        hideOverlay()
        updateForegroundNotification(appName, null)

        val composeView = ComposeView(this)
        composeView.setContent {
            TALauncherTheme {
                SessionExpiryActionDialog(
                    appName = appName,
                    showMathChallengeOption = showMathOption,
                    onExtend = {
                        val extendIntent = Intent("com.talauncher.SESSION_EXPIRY_EXTEND").apply {
                            putExtra("package_name", packageName)
                        }
                        sendBroadcast(extendIntent)
                        hideOverlay()
                        stopServiceAfterHiding()
                    },
                    onClose = {
                        val closeIntent = Intent("com.talauncher.SESSION_EXPIRY_CLOSE").apply {
                            putExtra("package_name", packageName)
                        }
                        sendBroadcast(closeIntent)
                        hideOverlay()
                        stopServiceAfterHiding()
                    },
                    onMathChallenge = if (showMathOption) {
                        {
                            val mathIntent = Intent("com.talauncher.SESSION_EXPIRY_MATH_CHALLENGE").apply {
                                putExtra("package_name", packageName)
                            }
                            sendBroadcast(mathIntent)
                            hideOverlay()
                            stopServiceAfterHiding()
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
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
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
            overlayView = null
            stopServiceAfterHiding()
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

    private fun stopServiceAfterHiding() {
        stopForegroundIfNeeded()
        stopSelf()
    }

    private fun stopForegroundIfNeeded() {
        if (isForeground) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            isForeground = false
        }
    }

    private fun updateForegroundNotification(appName: String, remainingSeconds: Int?) {
        val notification = buildNotification(appName, remainingSeconds)
        if (!isForeground) {
            startForeground(NOTIFICATION_ID, notification)
            isForeground = true
        } else {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(appName: String, remainingSeconds: Int?): Notification {
        val contentText = if (remainingSeconds != null) {
            getString(
                R.string.overlay_notification_countdown,
                appName,
                remainingSeconds.coerceAtLeast(0)
            )
        } else {
            getString(R.string.overlay_notification_decision, appName)
        }

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
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
                enableLights(false)
                enableVibration(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        hideOverlay()
        stopForegroundIfNeeded()
        super.onDestroy()
    }
}
