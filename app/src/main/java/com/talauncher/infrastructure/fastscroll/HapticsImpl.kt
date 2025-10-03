package com.talauncher.infrastructure.fastscroll

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.talauncher.domain.fastscroll.Haptics

/**
 * Android implementation of haptic feedback for fast scrolling.
 *
 * Architecture:
 * - Implements Haptics interface (Dependency Inversion)
 * - Handles Android-specific vibration APIs
 * - Rate-limits app change haptics to prevent spam
 *
 * SOLID Principles:
 * - Single Responsibility: Only handles haptic feedback
 * - Dependency Inversion: Implements abstract Haptics interface
 * - Open/Closed: Can be extended for different haptic patterns
 *
 * Performance:
 * - Rate-limited to max 1 app change haptic per 50ms
 * - Letter change haptics are not rate-limited (less frequent)
 * - Uses modern VibrationEffect API on supported devices
 *
 * @param context Android context for accessing vibrator service
 */
class HapticsImpl(context: Context) : Haptics {

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator ?: context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    // Rate limiting for app change haptics
    private var lastAppChangeHapticTime = 0L
    private val appChangeHapticThrottleMs = 50L

    /**
     * Performs a subtle haptic pulse for letter boundary crossing.
     * Duration: 15ms, Medium intensity
     */
    override fun performLetterChange() {
        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(
                15, // 15ms duration
                VibrationEffect.DEFAULT_AMPLITUDE // Medium intensity
            )
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(15)
        }
    }

    /**
     * Performs a very light haptic pulse for app-to-app movement.
     * Duration: 8ms, Light intensity
     * Rate-limited to max 1 per 50ms to avoid spam.
     */
    override fun performAppChange() {
        if (!vibrator.hasVibrator()) return

        val now = System.currentTimeMillis()
        if (now - lastAppChangeHapticTime < appChangeHapticThrottleMs) {
            return // Throttle
        }
        lastAppChangeHapticTime = now

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(
                8, // 8ms duration (lighter than letter change)
                80 // Light intensity (0-255 scale)
            )
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(8)
        }
    }
}
