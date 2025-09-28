package com.talauncher.utils

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import com.talauncher.ui.components.SessionExpiryActionDialog
import com.talauncher.ui.components.SessionExpiryCountdownDialog
import com.talauncher.data.model.MathDifficulty
import com.talauncher.ui.components.MathChallengeDialog
import com.talauncher.ui.theme.TALauncherTheme
import com.talauncher.BuildConfig

/**
 * Manages system overlay windows that can be shown from background without foreground service.
 * This approach bypasses Android 15's foreground service restrictions by using ApplicationContext.
 */
class BackgroundOverlayManager private constructor(private val applicationContext: Context) {

    private var windowManager: WindowManager? = null
    private var currentOverlayView: View? = null
    private val tag = "BackgroundOverlayManager"

    companion object {
        @Volatile
        private var INSTANCE: BackgroundOverlayManager? = null

        fun getInstance(context: Context): BackgroundOverlayManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BackgroundOverlayManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    init {
        windowManager = applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    fun showCountdownOverlay(
        appName: String,
        remainingSeconds: Int,
        totalSeconds: Int,
        onFinished: () -> Unit = {}
    ): Boolean {
        if (!canDrawOverlays()) {
            Log.w(tag, "Cannot show countdown overlay - no SYSTEM_ALERT_WINDOW permission")
            return false
        }

        hideCurrentOverlay()

        val composeView = ComposeView(applicationContext).apply {
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

        return showOverlayView(composeView)
    }

    fun showDecisionOverlay(
        appName: String,
        packageName: String,
        showMathOption: Boolean,
        onExtend: () -> Unit = {},
        onClose: () -> Unit = {},
        onMathChallenge: (() -> Unit)? = null
    ): Boolean {
        if (!canDrawOverlays()) {
            Log.w(tag, "Cannot show decision overlay - no SYSTEM_ALERT_WINDOW permission")
            return false
        }

        hideCurrentOverlay()

        val composeView = ComposeView(applicationContext).apply {
            setContent {
                TALauncherTheme {
                    SessionExpiryActionDialog(
                        appName = appName,
                        showMathChallengeOption = showMathOption,
                        onExtend = {
                            onExtend()
                            hideCurrentOverlay()
                        },
                        onClose = {
                            onClose()
                            hideCurrentOverlay()
                        },
                        onMathChallenge = if (showMathOption && onMathChallenge != null) {
                            {
                                onMathChallenge()
                                hideCurrentOverlay()
                            }
                        } else {
                            null
                        }
                    )
                }
            }
        }

        return showOverlayView(composeView)
    }

    fun showMathChallengeOverlay(
        appName: String,
        packageName: String,
        difficulty: MathDifficulty,
        onCorrect: () -> Unit = {},
        onDismiss: () -> Unit = {}
    ): Boolean {
        if (!canDrawOverlays()) {
            Log.w(tag, "Cannot show math challenge overlay - no SYSTEM_ALERT_WINDOW permission")
            return false
        }

        hideCurrentOverlay()

        val composeView = ComposeView(applicationContext).apply {
            setContent {
                TALauncherTheme {
                    MathChallengeDialog(
                        difficulty = difficulty,
                        isTimeExpired = true,
                        onCorrect = {
                            onCorrect()
                            hideCurrentOverlay()
                        },
                        onDismiss = {
                            onDismiss()
                            hideCurrentOverlay()
                        }
                    )
                }
            }
        }

        return showOverlayView(composeView)
    }

    fun hideCurrentOverlay() {
        currentOverlayView?.let { view ->
            try {
                windowManager?.removeViewImmediate(view)
                if (BuildConfig.DEBUG) Log.d(tag, "Overlay hidden successfully")
            } catch (e: Exception) {
                Log.e(tag, "Error hiding overlay", e)
            }
        }
        currentOverlayView = null
    }

    private fun showOverlayView(view: View): Boolean {
        return try {
            val layoutParams = createLayoutParams()
            windowManager?.addView(view, layoutParams)
            currentOverlayView = view
            if (BuildConfig.DEBUG) Log.d(tag, "Overlay shown successfully")
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to show overlay", e)
            false
        }
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
                WindowManager.LayoutParams.FLAG_DIM_BEHIND or
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
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
        }
    }

    private fun canDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(applicationContext)
    }

    fun isOverlayVisible(): Boolean {
        return currentOverlayView != null
    }
}