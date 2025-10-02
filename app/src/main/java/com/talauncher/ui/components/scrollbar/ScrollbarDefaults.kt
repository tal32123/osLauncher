package com.talauncher.ui.components.scrollbar

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Default values and configurations for scrollbar components.
 *
 * This object follows the Factory Pattern and provides Material Design-compliant
 * defaults for scrollbar appearance and behavior. It centralizes default values
 * following the Single Responsibility Principle.
 *
 * Design decisions:
 * - Material Design 3 color system integration
 * - Smooth animation specs following Material Motion guidelines
 * - Accessible sizing (minimum 48dp touch targets when interactive)
 * - Subtle visual presence that doesn't distract from content
 */
object ScrollbarDefaults {

    /**
     * Default thumb width for the scrollbar.
     */
    val ThumbWidth: Dp = 6.dp

    /**
     * Default track width for the scrollbar.
     */
    val TrackWidth: Dp = 8.dp

    /**
     * Minimum height for the thumb to ensure visibility and touch target.
     */
    val ThumbMinHeight: Dp = 48.dp

    /**
     * Default corner radius for the thumb.
     */
    val ThumbCornerRadius: Dp = 3.dp

    /**
     * Default corner radius for the track.
     */
    val TrackCornerRadius: Dp = 4.dp

    /**
     * Default padding around the scrollbar.
     */
    val Padding: Dp = 2.dp

    /**
     * Default auto-hide delay in milliseconds.
     */
    const val AutoHideDelayMillis: Long = 1500L

    /**
     * Default scroll to top threshold (10% from top).
     */
    const val ScrollToTopThreshold: Float = 0.1f

    /**
     * Creates a default scrollbar configuration using Material Theme colors.
     *
     * This composable function follows the Dependency Injection principle,
     * taking Material Theme colors and returning a configured ScrollbarConfig.
     *
     * @param thumbColor Color for the scrollbar thumb (default: onSurfaceVariant with 40% alpha)
     * @param thumbColorDragging Color when dragging (default: onSurfaceVariant with 60% alpha)
     * @param trackColor Color for the track (default: null, no track)
     * @param thumbWidth Width of the thumb
     * @param thumbMinHeight Minimum height of the thumb
     * @param thumbMaxHeight Maximum height of the thumb
     * @param thumbCornerRadius Corner radius of the thumb
     * @param trackWidth Width of the track
     * @param trackCornerRadius Corner radius of the track
     * @param padding Padding around the scrollbar
     * @param autoHideEnabled Whether to auto-hide when not in use
     * @param autoHideDelayMillis Delay before auto-hiding
     * @param enableHapticFeedback Whether to enable haptic feedback
     * @param scrollToTopThreshold Threshold for scroll to top indicator
     * @param enableScrollPreview Whether to show scroll preview
     * @return Configured ScrollbarConfig instance
     */
    @Composable
    fun config(
        thumbColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        thumbColorDragging: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        trackColor: Color? = null,
        thumbWidth: Dp = ThumbWidth,
        thumbMinHeight: Dp = ThumbMinHeight,
        thumbMaxHeight: Dp? = null,
        thumbCornerRadius: Dp = ThumbCornerRadius,
        trackWidth: Dp = TrackWidth,
        trackCornerRadius: Dp = TrackCornerRadius,
        padding: Dp = Padding,
        autoHideEnabled: Boolean = true,
        autoHideDelayMillis: Long = AutoHideDelayMillis,
        enableHapticFeedback: Boolean = true,
        scrollToTopThreshold: Float = ScrollToTopThreshold,
        enableScrollPreview: Boolean = false
    ): ScrollbarConfig {
        return ScrollbarConfig(
            thumbColor = thumbColor,
            thumbColorDragging = thumbColorDragging,
            trackColor = trackColor,
            thumbWidth = thumbWidth,
            thumbMinHeight = thumbMinHeight,
            thumbMaxHeight = thumbMaxHeight,
            thumbCornerRadius = thumbCornerRadius,
            trackWidth = trackWidth,
            trackCornerRadius = trackCornerRadius,
            padding = padding,
            autoHideEnabled = autoHideEnabled,
            autoHideDelayMillis = autoHideDelayMillis,
            enableHapticFeedback = enableHapticFeedback,
            scrollToTopThreshold = scrollToTopThreshold,
            enableScrollPreview = enableScrollPreview
        )
    }

    /**
     * Creates a default preview provider for common use cases.
     *
     * This follows the Factory Pattern, providing a simple way to create
     * preview providers without exposing implementation details.
     *
     * @param items List of preview items
     * @return ScrollPreviewProvider instance
     */
    fun previewProvider(items: List<ScrollPreviewItem>): ScrollPreviewProvider {
        return DefaultScrollPreviewProvider(items)
    }

    /**
     * Creates a preview provider from a list of strings.
     *
     * Positions are automatically calculated based on list indices.
     *
     * @param textItems List of strings for preview
     * @return ScrollPreviewProvider instance
     */
    fun previewProviderFromStrings(textItems: List<String>): ScrollPreviewProvider {
        val previewItems = textItems.mapIndexed { index, text ->
            ScrollPreviewItem(
                text = text,
                position = if (textItems.size > 1) index.toFloat() / (textItems.size - 1) else 0f
            )
        }
        return DefaultScrollPreviewProvider(previewItems)
    }

    /**
     * Animation specs for different scrollbar interactions.
     *
     * These follow Material Motion guidelines for natural, responsive animations.
     */
    @Immutable
    object AnimationSpecs {
        /**
         * Animation spec for thumb position changes during scrolling.
         * Fast and responsive to feel connected to scroll gestures.
         */
        val ThumbPosition: AnimationSpec<Float> = tween(
            durationMillis = 100,
            easing = LinearEasing
        )

        /**
         * Animation spec for thumb height changes.
         * Slightly slower for smooth visual transitions.
         */
        val ThumbHeight: AnimationSpec<Float> = tween(
            durationMillis = 150,
            easing = FastOutSlowInEasing
        )

        /**
         * Animation spec for fade in/out effects.
         * Medium duration for noticeable but not distracting transitions.
         */
        val FadeInOut: AnimationSpec<Float> = tween(
            durationMillis = 200,
            easing = LinearEasing
        )

        /**
         * Animation spec for scroll position changes when dragging thumb.
         * Immediate response for direct manipulation.
         */
        val ScrollPosition: AnimationSpec<Float> = tween(
            durationMillis = 0
        )

        /**
         * Animation spec for smooth scroll to position.
         * Natural spring animation for programmatic scrolling.
         */
        val SmoothScroll: AnimationSpec<Float> = spring(
            dampingRatio = 0.8f,
            stiffness = 300f
        )
    }

    /**
     * Default durations for various scrollbar operations.
     */
    @Immutable
    object Durations {
        /**
         * Duration for fade-in animation when scrollbar appears.
         */
        const val FadeInMillis: Int = 200

        /**
         * Duration for fade-out animation when scrollbar hides.
         */
        const val FadeOutMillis: Int = 200

        /**
         * Duration for thumb position animation.
         */
        const val ThumbPositionMillis: Int = 100

        /**
         * Duration for thumb height animation.
         */
        const val ThumbHeightMillis: Int = 150

        /**
         * Duration for color transition when starting/stopping drag.
         */
        const val ColorTransitionMillis: Int = 150
    }

    /**
     * Alpha values for different visibility states.
     */
    @Immutable
    object AlphaValues {
        /**
         * Fully visible alpha.
         */
        const val Visible: Float = 1f

        /**
         * Partially visible alpha for idle state.
         */
        const val Idle: Float = 0.7f

        /**
         * Hidden alpha (fully transparent).
         */
        const val Hidden: Float = 0f
    }

    /**
     * Minimum scroll velocity to trigger momentum scrolling (pixels/second).
     */
    const val MinimumMomentumVelocity: Float = 100f

    /**
     * Friction factor for momentum scrolling decay.
     */
    const val MomentumFriction: Float = 0.95f
}

/**
 * Default implementation of ScrollPreviewProvider.
 *
 * This class follows the Strategy Pattern, providing a concrete implementation
 * of the preview provider interface.
 *
 * @param items List of preview items, must be sorted by position
 */
private class DefaultScrollPreviewProvider(
    private val items: List<ScrollPreviewItem>
) : ScrollPreviewProvider {

    init {
        require(items.isEmpty() || items.zipWithNext().all { (a, b) -> a.position <= b.position }) {
            "Preview items must be sorted by position"
        }
    }

    /**
     * Finds the closest preview item to the given position using binary search.
     *
     * Time complexity: O(log n)
     *
     * @param normalizedPosition Current scroll position (0f to 1f)
     * @return The closest preview item, or null if no items available
     */
    override fun getPreviewAt(normalizedPosition: Float): ScrollPreviewItem? {
        if (items.isEmpty()) return null

        // Handle edge cases
        if (normalizedPosition <= 0f) return items.first()
        if (normalizedPosition >= 1f) return items.last()

        // Binary search for the closest item
        var left = 0
        var right = items.size - 1
        var closestIndex = 0
        var closestDiff = Float.MAX_VALUE

        while (left <= right) {
            val mid = (left + right) / 2
            val item = items[mid]
            val diff = kotlin.math.abs(item.position - normalizedPosition)

            if (diff < closestDiff) {
                closestDiff = diff
                closestIndex = mid
            }

            when {
                item.position < normalizedPosition -> left = mid + 1
                item.position > normalizedPosition -> right = mid - 1
                else -> return item // Exact match
            }
        }

        return items[closestIndex]
    }
}
