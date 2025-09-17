package com.talauncher.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
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
import com.talauncher.MainActivity
import com.talauncher.R
import com.talauncher.ui.components.SessionExpiryActionDialog
import com.talauncher.ui.components.SessionExpiryCountdownDialog
import com.talauncher.ui.theme.TALauncherTheme

class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var notificationManager: NotificationManager? = null
    private var isForegroundService = false
    private var currentNotificationMessage: String? = null

    companion object {
        const val ACTION_SHOW_COUNTDOWN = "show_countdown"
        const val ACTION_SHOW_DECISION = "show_decision"
        const val ACTION_HIDE_OVERLAY = "hide_overlay"
        const val EXTRA_APP_NAME = "app_name"
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_REMAINING_SECONDS = "remaining_seconds"
        const val EXTRA_TOTAL_SECONDS = "total_seconds"
        const val EXTRA_SHOW_MATH_OPTION = "show_math_option"
        private const val CHANNEL_ID = "session_overlay_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
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
                hideOverlay(shouldStopService = true)
            }
        }
        return START_NOT_STICKY
    }

    private fun showCountdownOverlay(appName: String, remainingSeconds: Int, totalSeconds: Int) {
        if (!Settings.canDrawOverlays(this)) {
            return
        }

        val notificationMessage = getString(
            R.string.overlay_notification_countdown,
            appName,
            remainingSeconds.coerceAtLeast(0)
        )
        startForegroundWithMessage(notificationMessage)

        hideOverlay()

        val composeView = ComposeView(this).apply {
            setContent {
                TALauncherTheme {
                    SessionExpiryCountdownDialog(
                        appName = appName,
                        remainingSeconds = remainingSeconds,
                        totalSeconds = totalSeconds
                    )
                }
            }
        }

        val layoutParams = createLayoutParams()

        try {
            overlayView = composeView
            windowManager?.addView(overlayView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showDecisionOverlay(appName: String, packageName: String, showMathOption: Boolean) {
        if (!Settings.canDrawOverlays(this)) {
            return
        }

        val notificationMessage = getString(R.string.overlay_notification_decision, appName)
        startForegroundWithMessage(notificationMessage)

        hideOverlay()

        val composeView = ComposeView(this).apply {
            setContent {
                TALauncherTheme {
                    SessionExpiryActionDialog(
                        appName = appName,
                        showMathChallengeOption = showMathOption,
                        onExtend = {
                            val extendIntent = Intent("com.talauncher.SESSION_EXPIRY_EXTEND")
                            extendIntent.putExtra("package_name", packageName)
                            sendBroadcast(extendIntent)
                            hideOverlay(shouldStopService = true)
                        },
                        onClose = {
                            val closeIntent = Intent("com.talauncher.SESSION_EXPIRY_CLOSE")
                            closeIntent.putExtra("package_name", packageName)
                            sendBroadcast(closeIntent)
                            hideOverlay(shouldStopService = true)
                        },
                        onMathChallenge = if (showMathOption) {
                            {
                                val challengeIntent =
                                    Intent("com.talauncher.SESSION_EXPIRY_MATH_CHALLENGE")
                                challengeIntent.putExtra("package_name", packageName)
                                sendBroadcast(challengeIntent)
                                hideOverlay(shouldStopService = true)
                            }
                        } else {
                            null
                        }
                    )
                }
            }
        }

        val layoutParams = createLayoutParams()

        try {
            overlayView = composeView
            windowManager?.addView(overlayView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hideOverlay(shouldStopService: Boolean = false) {
        overlayView?.let { view ->
            try {
                windowManager?.removeViewImmediate(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        overlayView = null
        if (shouldStopService) {
            stopForegroundIfNeeded()
            stopSelf()
        }
    }

    override fun onDestroy() {
        hideOverlay()
        stopForegroundIfNeeded()
        super.onDestroy()
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR or
                    WindowManager.LayoutParams.FLAG_DIM_BEHIND or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            dimAmount = 0.35f
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.overlay_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.overlay_notification_channel_description)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(message: String): Notification {
        val sanitizedMessage = message.ifBlank { getString(R.string.overlay_notification_default) }
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(sanitizedMessage)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun startForegroundWithMessage(message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        val sanitizedMessage = message.ifBlank { getString(R.string.overlay_notification_default) }
        if (!isForegroundService) {
            val notification = buildNotification(sanitizedMessage)
            startForeground(NOTIFICATION_ID, notification)
            isForegroundService = true
        } else if (currentNotificationMessage != sanitizedMessage) {
            val notification = buildNotification(sanitizedMessage)
            notificationManager?.notify(NOTIFICATION_ID, notification)
        }
        currentNotificationMessage = sanitizedMessage
    }

    private fun stopForegroundIfNeeded() {
        if (isForegroundService) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            isForegroundService = false
        }
        currentNotificationMessage = null
        notificationManager?.cancel(NOTIFICATION_ID)
    }
}