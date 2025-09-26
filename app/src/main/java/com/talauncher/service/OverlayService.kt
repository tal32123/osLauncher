package com.talauncher.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Build
import android.os.SystemClock
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.talauncher.MainActivity
import com.talauncher.R
import com.talauncher.data.model.MathDifficulty
import com.talauncher.ui.components.SessionExpiryActionDialog
import com.talauncher.ui.components.SessionExpiryCountdownDialog
import com.talauncher.ui.components.MathChallengeDialog
import com.talauncher.ui.theme.TALauncherTheme
import android.util.Log

class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var notificationManager: NotificationManager? = null
    private var isForegroundService = false
    private var currentNotificationMessage: String? = null
    private var pendingForegroundStart: Runnable? = null
    private var pendingForegroundView: View? = null
    private var pendingForegroundMessage: String? = null
    private var pendingForegroundStartTime: Long = 0L

    companion object {
        const val ACTION_SHOW_COUNTDOWN = "show_countdown"
        const val ACTION_SHOW_DECISION = "show_decision"
        const val ACTION_SHOW_MATH_CHALLENGE = "show_math_challenge"
        const val ACTION_HIDE_OVERLAY = "hide_overlay"
        const val EXTRA_APP_NAME = "app_name"
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_REMAINING_SECONDS = "remaining_seconds"
        const val EXTRA_TOTAL_SECONDS = "total_seconds"
        const val EXTRA_SHOW_MATH_OPTION = "show_math_option"
        const val EXTRA_DIFFICULTY = "difficulty"
        private const val CHANNEL_ID = "session_overlay_channel"
        private const val NOTIFICATION_ID = 1001
        private const val TAG = "OverlayService"
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
            ACTION_SHOW_MATH_CHALLENGE -> {
                val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "this app"
                val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
                val difficulty = MathDifficulty.fromStorageValue(intent.getStringExtra(EXTRA_DIFFICULTY))
                showMathChallengeOverlay(appName, packageName, difficulty)
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
            windowManager?.addView(composeView, layoutParams)
            overlayView = composeView
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach countdown overlay", e)
            e.printStackTrace()
            return
        }

        val notificationMessage = getString(
            R.string.overlay_notification_countdown,
            appName,
            remainingSeconds.coerceAtLeast(0)
        )
        if (!startForegroundWithMessage(notificationMessage)) {
            Log.w(TAG, "Unable to show countdown overlay without notification permission")
            hideOverlay()
            stopSelf()
        }
    }

    private fun showDecisionOverlay(appName: String, packageName: String, showMathOption: Boolean) {
        if (!Settings.canDrawOverlays(this)) {
            return
        }

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
                            extendIntent.setPackage(this@OverlayService.packageName)
                            sendBroadcast(extendIntent)
                            hideOverlay(shouldStopService = true)
                        },
                        onClose = {
                            val closeIntent = Intent("com.talauncher.SESSION_EXPIRY_CLOSE")
                            closeIntent.putExtra("package_name", packageName)
                            closeIntent.setPackage(this@OverlayService.packageName)
                            sendBroadcast(closeIntent)
                            hideOverlay(shouldStopService = true)
                        },
                        onMathChallenge = if (showMathOption) {
                            {
                                val challengeIntent =
                                    Intent("com.talauncher.SESSION_EXPIRY_MATH_CHALLENGE")
                                challengeIntent.putExtra("package_name", packageName)
                                challengeIntent.setPackage(this@OverlayService.packageName)
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
            windowManager?.addView(composeView, layoutParams)
            overlayView = composeView
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach decision overlay", e)
            e.printStackTrace()
            return
        }

        val notificationMessage = getString(R.string.overlay_notification_decision, appName)
        if (!startForegroundWithMessage(notificationMessage)) {
            Log.w(TAG, "Unable to show decision overlay without notification permission")
            hideOverlay()
            stopSelf()
        }
    }

    private fun showMathChallengeOverlay(
        appName: String,
        packageName: String,
        difficulty: MathDifficulty
    ) {
        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Cannot show math challenge overlay without SYSTEM_ALERT_WINDOW permission")
            return
        }

        hideOverlay()

        val composeView = ComposeView(this).apply {
            setContent {
                TALauncherTheme {
                    MathChallengeDialog(
                        difficulty = difficulty,
                        isTimeExpired = true,
                        onCorrect = {
                            val correctIntent = Intent("com.talauncher.MATH_CHALLENGE_CORRECT")
                            correctIntent.putExtra("package_name", packageName)
                            correctIntent.setPackage(this@OverlayService.packageName)
                            sendBroadcast(correctIntent)
                            hideOverlay(shouldStopService = true)
                        },
                        onDismiss = {
                            val dismissIntent = Intent("com.talauncher.MATH_CHALLENGE_DISMISS")
                            dismissIntent.putExtra("package_name", packageName)
                            dismissIntent.setPackage(this@OverlayService.packageName)
                            sendBroadcast(dismissIntent)
                            hideOverlay(shouldStopService = true)
                        }
                    )
                }
            }
        }

        val layoutParams = createLayoutParams()

        try {
            windowManager?.addView(composeView, layoutParams)
            overlayView = composeView
            Log.d(TAG, "Math challenge overlay displayed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show math challenge overlay", e)
            e.printStackTrace()
            return
        }

        val notificationMessage = getString(R.string.overlay_notification_default)
        if (!startForegroundWithMessage(notificationMessage)) {
            Log.w(TAG, "Unable to show math challenge overlay without notification permission")
            hideOverlay()
            stopSelf()
        }
    }

    private fun hideOverlay(shouldStopService: Boolean = false) {
        cancelPendingForegroundStart()
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

        // Clear references to prevent memory leaks
        windowManager = null
        notificationManager = null

        super.onDestroy()
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // Essential flags for blocking overlay that captures all interactions
        val flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR or
                WindowManager.LayoutParams.FLAG_DIM_BEHIND or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            dimAmount = 0.6f // Stronger dim for better visibility
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

    private fun startForegroundWithMessage(message: String): Boolean {
        if (!hasNotificationPermission()) {
            Log.w(TAG, "Cannot start foreground service without notification permission")
            return false
        }

        val sanitizedMessage = message.ifBlank { getString(R.string.overlay_notification_default) }

        if (!isForegroundService && Build.VERSION.SDK_INT >= 35) {
            val view = overlayView
            if (view == null) {
                Log.w(TAG, "Cannot start foreground service on Android 15+ without overlay view")
                return false
            }

            if (!view.isAttachedToWindow) {
                Log.d(TAG, "Overlay view not yet attached; waiting before starting foreground service")
                scheduleForegroundStart(view, sanitizedMessage)
                return true
            }

            if (view.windowVisibility != View.VISIBLE || !view.isShown) {
                Log.d(TAG, "Overlay not visible yet; waiting before starting foreground service")
                scheduleForegroundStart(view, sanitizedMessage)
                return true
            }
        }

        cancelPendingForegroundStart()
        return startForegroundInternal(sanitizedMessage)
    }

    private fun scheduleForegroundStart(view: View, message: String) {
        if (pendingForegroundView !== view) {
            cancelPendingForegroundStart()
        }

        pendingForegroundMessage = message
        if (pendingForegroundStart != null) {
            return
        }

        pendingForegroundView = view
        pendingForegroundStartTime = SystemClock.uptimeMillis()
        val runnable = object : Runnable {
            override fun run() {
                val currentView = pendingForegroundView
                if (currentView == null || overlayView !== currentView) {
                    cancelPendingForegroundStart()
                    return
                }

                if (currentView.windowVisibility == View.VISIBLE && currentView.isShown) {
                    val messageToUse = pendingForegroundMessage ?: message
                    cancelPendingForegroundStart()
                    startForegroundInternal(messageToUse)
                    return
                }

                val elapsed = SystemClock.uptimeMillis() - pendingForegroundStartTime
                if (elapsed > 4000L) {
                    Log.w(TAG, "Overlay not visible after waiting; stopping overlay service")
                    cancelPendingForegroundStart()
                    hideOverlay(shouldStopService = true)
                    return
                }

                currentView.postDelayed(this, 50L)
            }
        }

        pendingForegroundStart = runnable
        view.post(runnable)
    }

    private fun cancelPendingForegroundStart() {
        val view = pendingForegroundView
        val runnable = pendingForegroundStart
        if (view != null && runnable != null) {
            view.removeCallbacks(runnable)
        }
        pendingForegroundStart = null
        pendingForegroundView = null
        pendingForegroundMessage = null
        pendingForegroundStartTime = 0L
    }

    private fun startForegroundInternal(message: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        val sanitizedMessage = message.ifBlank { getString(R.string.overlay_notification_default) }
        if (!isForegroundService) {
            val notification = buildNotification(sanitizedMessage)
            try {
                startForeground(NOTIFICATION_ID, notification)
                isForegroundService = true
                Log.d(TAG, "Foreground service started successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start foreground service", e)
                return false
            }
        } else if (currentNotificationMessage != sanitizedMessage) {
            val notification = buildNotification(sanitizedMessage)
            if (hasNotificationPermission()) {
                notificationManager?.notify(NOTIFICATION_ID, notification)
            }
        }
        currentNotificationMessage = sanitizedMessage
        return true
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
        if (hasNotificationPermission()) {
            notificationManager?.cancel(NOTIFICATION_ID)
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            true
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}
