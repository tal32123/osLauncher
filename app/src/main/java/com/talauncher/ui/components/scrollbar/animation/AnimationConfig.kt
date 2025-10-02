package com.talauncher.ui.components.scrollbar.animation

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.math.ln

/**
 * Configuration data class for scrollbar animations.
 *
 * This immutable configuration class encapsulates all animation-related parameters,
 * following the Single Responsibility Principle by managing only animation settings.
 *
 * **Design Pattern**: Builder Pattern (via copy() for modifications)
 * **SOLID Principle**: Single Responsibility - Manages animation configuration only
 *
 * @property baseDuration Base duration for scroll animations (short distances)
 * @property maxDuration Maximum duration cap for scroll animations (long distances)
 * @property visibilityDuration Duration for fade in/out visibility animations
 * @property autoHideDelay Delay before auto-hiding the scrollbar after interaction
 * @property scrollAnimationSpec Animation specification for scroll position changes
 * @property visibilityAnimationSpec Animation specification for visibility changes
 * @property minScrollDistance Minimum scroll distance (in items) to trigger animation
 * @property distanceMultiplier Multiplier for dynamic duration calculation based on distance
 */
data class AnimationConfig(
    val baseDuration: Duration = 150.milliseconds,
    val maxDuration: Duration = 500.milliseconds,
    val visibilityDuration: Duration = 200.milliseconds,
    val autoHideDelay: Duration = 1500.milliseconds,
    val scrollAnimationSpec: AnimationSpec<Float> = tween(
        durationMillis = 300,
        easing = FastOutSlowInEasing
    ),
    val visibilityAnimationSpec: AnimationSpec<Float> = tween(
        durationMillis = 200,
        easing = LinearOutSlowInEasing
    ),
    val minScrollDistance: Int = 1,
    val distanceMultiplier: Float = 15f
) {
    /**
     * Calculates dynamic animation duration based on scroll distance.
     *
     * Uses a logarithmic scale to ensure smooth animations for both
     * short and long scroll distances while respecting max duration cap.
     *
     * @param distance Absolute scroll distance in items
     * @return Calculated duration in milliseconds, capped at maxDuration
     */
    fun calculateDuration(distance: Int): Int {
        if (distance <= minScrollDistance) {
            return baseDuration.inWholeMilliseconds.toInt()
        }

        // Logarithmic scaling for natural feel
        val scaledDuration = baseDuration.inWholeMilliseconds +
            (kotlin.math.ln(distance.toFloat()) * distanceMultiplier).toLong()

        return scaledDuration.coerceAtMost(maxDuration.inWholeMilliseconds).toInt()
    }

    /**
     * Creates an animation spec with dynamic duration based on distance.
     *
     * @param distance Scroll distance in items
     * @return AnimationSpec configured with appropriate duration and easing
     */
    fun createScrollSpec(distance: Int): AnimationSpec<Float> {
        return tween(
            durationMillis = calculateDuration(distance),
            easing = FastOutSlowInEasing
        )
    }

    companion object {
        /**
         * Default animation configuration with balanced settings.
         */
        val Default = AnimationConfig()

        /**
         * Fast animation configuration for quick interactions.
         */
        val Fast = AnimationConfig(
            baseDuration = 100.milliseconds,
            maxDuration = 300.milliseconds,
            visibilityDuration = 150.milliseconds,
            autoHideDelay = 1000.milliseconds,
            distanceMultiplier = 10f
        )

        /**
         * Slow animation configuration for smooth, deliberate movements.
         */
        val Slow = AnimationConfig(
            baseDuration = 250.milliseconds,
            maxDuration = 800.milliseconds,
            visibilityDuration = 300.milliseconds,
            autoHideDelay = 2000.milliseconds,
            distanceMultiplier = 20f
        )

        /**
         * Accessibility-friendly configuration with longer delays.
         */
        val Accessible = AnimationConfig(
            baseDuration = 200.milliseconds,
            maxDuration = 600.milliseconds,
            visibilityDuration = 250.milliseconds,
            autoHideDelay = 3000.milliseconds,
            scrollAnimationSpec = tween(
                durationMillis = 400,
                easing = LinearEasing // More predictable for accessibility
            ),
            distanceMultiplier = 18f
        )

        /**
         * Reduced motion configuration for users with motion sensitivity.
         * Minimizes animation durations while maintaining functionality.
         */
        val ReducedMotion = AnimationConfig(
            baseDuration = 50.milliseconds,
            maxDuration = 100.milliseconds,
            visibilityDuration = 100.milliseconds,
            autoHideDelay = 2000.milliseconds,
            scrollAnimationSpec = tween(
                durationMillis = 100,
                easing = LinearEasing
            ),
            visibilityAnimationSpec = tween(
                durationMillis = 100,
                easing = LinearEasing
            ),
            distanceMultiplier = 5f
        )
    }
}

/**
 * Scrollbar visual configuration for dimensions and styling.
 *
 * Separated from AnimationConfig to follow Single Responsibility Principle.
 *
 * @property thumbWidth Width of the scrollbar thumb
 * @property thumbMinHeight Minimum height of the scrollbar thumb
 * @property trackWidth Width of the scrollbar track (background)
 * @property padding Padding around the scrollbar
 */
data class ScrollbarConfig(
    val thumbWidth: Dp = 4.dp,
    val thumbMinHeight: Dp = 48.dp,
    val trackWidth: Dp = 6.dp,
    val padding: Dp = 4.dp
) {
    companion object {
        val Default = ScrollbarConfig()

        val Compact = ScrollbarConfig(
            thumbWidth = 3.dp,
            thumbMinHeight = 40.dp,
            trackWidth = 4.dp,
            padding = 2.dp
        )

        val Wide = ScrollbarConfig(
            thumbWidth = 6.dp,
            thumbMinHeight = 56.dp,
            trackWidth = 8.dp,
            padding = 6.dp
        )
    }
}
