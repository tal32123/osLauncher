package com.talauncher.ui.components.scrollbar

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Configuration for scrollbar appearance and behavior.
 *
 * This class encapsulates all customizable aspects of the scrollbar,
 * following the Single Responsibility Principle by managing only configuration data.
 *
 * @param thumbColor Color of the scrollbar thumb
 * @param thumbColorDragging Color of the thumb when being dragged
 * @param trackColor Color of the scrollbar track (optional, null for no track)
 * @param thumbWidth Width of the scrollbar thumb
 * @param thumbMinHeight Minimum height of the thumb
 * @param thumbMaxHeight Maximum height of the thumb (null for no limit)
 * @param thumbCornerRadius Corner radius for rounded thumb
 * @param trackWidth Width of the scrollbar track
 * @param trackCornerRadius Corner radius for rounded track
 * @param padding Padding around the scrollbar
 * @param autoHideEnabled Whether the scrollbar should auto-hide when not in use
 * @param autoHideDelayMillis Delay before auto-hiding (milliseconds)
 * @param enableHapticFeedback Whether to provide haptic feedback on interaction
 * @param scrollToTopThreshold Scroll position threshold to show "scroll to top" indicator
 * @param enableScrollPreview Whether to show content preview when scrolling
 */
@Immutable
data class ScrollbarConfig(
    val thumbColor: Color,
    val thumbColorDragging: Color,
    val trackColor: Color? = null,
    val thumbWidth: Dp = 6.dp,
    val thumbMinHeight: Dp = 48.dp,
    val thumbMaxHeight: Dp? = null,
    val thumbCornerRadius: Dp = 3.dp,
    val trackWidth: Dp = 8.dp,
    val trackCornerRadius: Dp = 4.dp,
    val padding: Dp = 2.dp,
    val autoHideEnabled: Boolean = true,
    val autoHideDelayMillis: Long = 1500L,
    val enableHapticFeedback: Boolean = true,
    val scrollToTopThreshold: Float = 0.1f,
    val enableScrollPreview: Boolean = false
) {
    init {
        require(thumbWidth > 0.dp) { "Thumb width must be positive" }
        require(thumbMinHeight > 0.dp) { "Thumb minimum height must be positive" }
        require(thumbMaxHeight == null || thumbMaxHeight >= thumbMinHeight) {
            "Thumb maximum height must be greater than or equal to minimum height"
        }
        require(trackWidth >= thumbWidth) { "Track width must be greater than or equal to thumb width" }
        require(autoHideDelayMillis >= 0) { "Auto-hide delay must be non-negative" }
        require(scrollToTopThreshold in 0f..1f) { "Scroll to top threshold must be between 0 and 1" }
    }
}

/**
 * Represents the current interaction state of the scrollbar.
 *
 * This sealed class follows the State Pattern, encapsulating different states
 * and their behaviors. Each state knows how to transition to other states.
 */
sealed class ScrollbarInteractionState {
    /**
     * Scrollbar is idle, not being interacted with.
     */
    data object Idle : ScrollbarInteractionState()

    /**
     * User is actively dragging the scrollbar thumb.
     *
     * @param startY The Y position where dragging started
     * @param currentY The current Y position of the drag
     */
    data class Dragging(
        val startY: Float,
        val currentY: Float
    ) : ScrollbarInteractionState()

    /**
     * Scrollbar is animating (e.g., smooth scroll, fade in/out).
     *
     * @param targetAlpha The target alpha value for fade animation
     */
    data class Animating(
        val targetAlpha: Float
    ) : ScrollbarInteractionState()

    /**
     * Scrollbar is in the process of auto-hiding.
     *
     * @param hideAfterMillis Time remaining before hiding (milliseconds)
     */
    data class AutoHiding(
        val hideAfterMillis: Long
    ) : ScrollbarInteractionState()
}

/**
 * Visual state properties for rendering the scrollbar.
 *
 * This class follows the Single Responsibility Principle by managing only
 * visual rendering state, separate from interaction logic.
 *
 * @param thumbOffsetY Vertical offset of the thumb from the top
 * @param thumbHeight Current height of the thumb
 * @param alpha Current opacity of the scrollbar (0f to 1f)
 * @param isVisible Whether the scrollbar should be visible
 * @param currentColor Current color of the thumb (may vary based on state)
 */
@Immutable
data class ScrollbarVisualState(
    val thumbOffsetY: Float = 0f,
    val thumbHeight: Float = 0f,
    val alpha: Float = 1f,
    val isVisible: Boolean = false,
    val currentColor: Color = Color.Transparent
) {
    init {
        require(alpha in 0f..1f) { "Alpha must be between 0 and 1" }
        require(thumbHeight >= 0f) { "Thumb height must be non-negative" }
        require(thumbOffsetY >= 0f) { "Thumb offset must be non-negative" }
    }
}

/**
 * Touch interaction state for tracking user input.
 *
 * This sealed class encapsulates touch event handling state,
 * following the Single Responsibility Principle.
 */
sealed class TouchState {
    /**
     * No touch interaction.
     */
    data object None : TouchState()

    /**
     * Touch started, tracking the initial position.
     *
     * @param startY Y position where touch started
     * @param startScrollOffset The scroll offset when touch started
     */
    data class Started(
        val startY: Float,
        val startScrollOffset: Float
    ) : TouchState()

    /**
     * Touch is moving, actively dragging.
     *
     * @param currentY Current Y position
     * @param deltaY Change in Y position since last event
     * @param initialScrollOffset The scroll offset when dragging started
     */
    data class Moving(
        val currentY: Float,
        val deltaY: Float,
        val initialScrollOffset: Float
    ) : TouchState()

    /**
     * Touch ended, may trigger momentum or snap animation.
     *
     * @param velocityY Velocity at the end of the touch (pixels per second)
     */
    data class Ended(
        val velocityY: Float
    ) : TouchState()
}

/**
 * Scroll position information for calculating scrollbar position and size.
 *
 * This class follows the Interface Segregation Principle by providing
 * only the scroll information needed for scrollbar calculations.
 *
 * @param offset Current scroll offset (pixels)
 * @param maxOffset Maximum scroll offset (pixels)
 * @param viewportHeight Height of the visible viewport (pixels)
 * @param contentHeight Total height of the scrollable content (pixels)
 */
@Immutable
data class ScrollPosition(
    val offset: Float,
    val maxOffset: Float,
    val viewportHeight: Float,
    val contentHeight: Float
) {
    /**
     * Normalized scroll position (0f to 1f).
     */
    val normalizedPosition: Float
        get() = if (maxOffset > 0f) (offset / maxOffset).coerceIn(0f, 1f) else 0f

    /**
     * Ratio of viewport height to content height.
     */
    val viewportRatio: Float
        get() = if (contentHeight > 0f) (viewportHeight / contentHeight).coerceIn(0f, 1f) else 1f

    /**
     * Whether content is scrollable (content exceeds viewport).
     */
    val isScrollable: Boolean
        get() = contentHeight > viewportHeight && maxOffset > 0f

    init {
        require(offset >= 0f) { "Offset must be non-negative" }
        require(maxOffset >= 0f) { "Max offset must be non-negative" }
        require(viewportHeight > 0f) { "Viewport height must be positive" }
        require(contentHeight >= 0f) { "Content height must be non-negative" }
    }
}

/**
 * Preview item data for scroll preview feature.
 *
 * This class encapsulates preview content information,
 * following the Single Responsibility Principle.
 *
 * @param text Text to display in the preview
 * @param position Normalized position in the list (0f to 1f)
 */
@Immutable
data class ScrollPreviewItem(
    val text: String,
    val position: Float
) {
    init {
        require(position in 0f..1f) { "Position must be between 0 and 1" }
    }
}

/**
 * Interface for providing preview items during scrolling.
 *
 * This interface follows the Dependency Inversion Principle,
 * allowing different implementations of preview providers
 * without changing the scrollbar component.
 */
interface ScrollPreviewProvider {
    /**
     * Get the preview item at the given normalized scroll position.
     *
     * @param normalizedPosition The current scroll position (0f to 1f)
     * @return Preview item to display, or null if no preview available
     */
    fun getPreviewAt(normalizedPosition: Float): ScrollPreviewItem?
}

/**
 * Default empty preview provider.
 *
 * This follows the Null Object Pattern, providing a safe default
 * that does nothing when no preview is needed.
 */
object EmptyScrollPreviewProvider : ScrollPreviewProvider {
    override fun getPreviewAt(normalizedPosition: Float): ScrollPreviewItem? = null
}
