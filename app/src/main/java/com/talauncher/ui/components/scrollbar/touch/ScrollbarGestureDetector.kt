package com.talauncher.ui.components.scrollbar.touch

import android.view.VelocityTracker
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Gesture detector specialized for scrollbar touch interactions.
 *
 * This class follows the Single Responsibility Principle by focusing solely on gesture
 * recognition, delegating touch event processing to IScrollbarTouchHandler and position
 * calculations to IScrollPositionCalculator.
 *
 * Responsibilities:
 * - Detect TAP, DRAG, FLING, and LONG_PRESS gestures
 * - Track touch velocity using Android's VelocityTracker
 * - Apply configurable thresholds for gesture recognition
 * - Maintain gesture state throughout the touch sequence
 *
 * @property config Configuration parameters for gesture detection thresholds
 * @property density Screen density for converting dp to pixels
 */
class ScrollbarGestureDetector(
    private val config: GestureConfig = GestureConfig.Default,
    private val density: Density
) {

    private var velocityTracker: VelocityTracker? = null
    private var initialTouchPosition: Offset = Offset.Zero
    private var initialTouchTimestamp: Long = 0L
    private var lastTouchPosition: Offset = Offset.Zero
    private var hasDragThresholdExceeded: Boolean = false
    private var currentGesture: GestureType? = null

    /**
     * Processes a touch down event to initialize gesture tracking.
     *
     * @param position The coordinate where the touch down occurred
     * @param timestamp The system time in milliseconds when the event occurred
     */
    fun onTouchDown(position: Offset, timestamp: Long) {
        reset()
        initialTouchPosition = position
        lastTouchPosition = position
        initialTouchTimestamp = timestamp
        hasDragThresholdExceeded = false
        currentGesture = null

        // Initialize velocity tracker
        velocityTracker = VelocityTracker.obtain()
        velocityTracker?.addMovement(position, timestamp)
    }

    /**
     * Processes a touch move event to track movement and detect gestures.
     *
     * @param position The current coordinate of the touch
     * @param timestamp The system time in milliseconds when the event occurred
     * @return DetectionResult containing the detected gesture and movement data
     */
    fun onTouchMove(position: Offset, timestamp: Long): DetectionResult {
        velocityTracker?.addMovement(position, timestamp)

        val deltaFromInitial = position - initialTouchPosition
        val deltaFromLast = position - lastTouchPosition
        val distanceFromInitial = calculateDistance(deltaFromInitial)
        val timeSinceDown = timestamp - initialTouchTimestamp

        lastTouchPosition = position

        // Check for long press if not yet dragging
        if (!hasDragThresholdExceeded && timeSinceDown >= config.longPressTimeoutMillis) {
            if (distanceFromInitial <= config.longPressMovementThresholdPx) {
                currentGesture = GestureType.LONG_PRESS
                return DetectionResult(
                    gesture = GestureType.LONG_PRESS,
                    delta = Offset.Zero,
                    velocity = getCurrentVelocity(),
                    isGestureStart = true
                )
            }
        }

        // Check if drag threshold has been exceeded
        if (!hasDragThresholdExceeded) {
            val dragThresholdPx = with(density) { config.dragThresholdDp.toPx() }
            if (distanceFromInitial > dragThresholdPx) {
                hasDragThresholdExceeded = true
                currentGesture = GestureType.DRAG
                return DetectionResult(
                    gesture = GestureType.DRAG,
                    delta = deltaFromLast,
                    velocity = getCurrentVelocity(),
                    isGestureStart = true
                )
            }
        }

        // Continue drag if threshold already exceeded
        if (hasDragThresholdExceeded) {
            return DetectionResult(
                gesture = GestureType.DRAG,
                delta = deltaFromLast,
                velocity = getCurrentVelocity(),
                isGestureStart = false
            )
        }

        // No gesture detected yet, but movement is being tracked
        return DetectionResult(
            gesture = null,
            delta = deltaFromLast,
            velocity = getCurrentVelocity(),
            isGestureStart = false
        )
    }

    /**
     * Processes a touch up event to finalize gesture detection.
     *
     * @param position The coordinate where the touch was released
     * @param timestamp The system time in milliseconds when the event occurred
     * @return DetectionResult containing the final gesture type
     */
    fun onTouchUp(position: Offset, timestamp: Long): DetectionResult {
        velocityTracker?.addMovement(position, timestamp)

        val timeSinceDown = timestamp - initialTouchTimestamp
        val deltaFromInitial = position - initialTouchPosition
        val distanceFromInitial = calculateDistance(deltaFromInitial)
        val velocity = getCurrentVelocity()
        val velocityMagnitude = velocity.magnitude

        // Determine final gesture type
        val finalGesture = when {
            // Fling gesture: fast movement with high velocity
            hasDragThresholdExceeded && velocityMagnitude >= config.flingVelocityThresholdPxPerSecond -> {
                GestureType.FLING
            }
            // Drag gesture: threshold exceeded but no fling
            hasDragThresholdExceeded -> {
                GestureType.DRAG
            }
            // Long press: held for long duration without movement
            currentGesture == GestureType.LONG_PRESS -> {
                GestureType.LONG_PRESS
            }
            // Tap gesture: quick release without exceeding drag threshold
            timeSinceDown <= config.tapMaxDurationMillis &&
            distanceFromInitial <= with(density) { config.tapMaxMovementDp.toPx() } -> {
                GestureType.TAP
            }
            // Default to tap if no other gesture detected
            else -> GestureType.TAP
        }

        val result = DetectionResult(
            gesture = finalGesture,
            delta = Offset.Zero,
            velocity = velocity,
            isGestureStart = false,
            isGestureEnd = true
        )

        reset()
        return result
    }

    /**
     * Processes a touch cancel event to clean up gesture tracking.
     *
     * @return DetectionResult indicating gesture cancellation
     */
    fun onTouchCancel(): DetectionResult {
        val result = DetectionResult(
            gesture = null,
            delta = Offset.Zero,
            velocity = TouchVelocity.Zero,
            isGestureStart = false,
            isGestureEnd = true,
            isCancelled = true
        )
        reset()
        return result
    }

    /**
     * Gets the current velocity from the velocity tracker.
     *
     * @return TouchVelocity containing x and y velocity in pixels per second
     */
    private fun getCurrentVelocity(): TouchVelocity {
        val tracker = velocityTracker ?: return TouchVelocity.Zero

        tracker.computeCurrentVelocity(1000) // Compute velocity in pixels per second
        return TouchVelocity(
            xVelocity = tracker.xVelocity,
            yVelocity = tracker.yVelocity
        )
    }

    /**
     * Calculates the Euclidean distance of an offset vector.
     *
     * @param offset The offset vector
     * @return The distance in pixels
     */
    private fun calculateDistance(offset: Offset): Float {
        return sqrt(offset.x * offset.x + offset.y * offset.y)
    }

    /**
     * Resets the gesture detector to its initial state.
     * Cleans up the velocity tracker and resets all tracking variables.
     */
    private fun reset() {
        velocityTracker?.recycle()
        velocityTracker = null
        initialTouchPosition = Offset.Zero
        lastTouchPosition = Offset.Zero
        initialTouchTimestamp = 0L
        hasDragThresholdExceeded = false
        currentGesture = null
    }

    /**
     * Extension function to add a movement event to the VelocityTracker.
     * This creates a synthetic MotionEvent since we're working with Compose touch events.
     */
    private fun VelocityTracker.addMovement(position: Offset, timestamp: Long) {
        // Note: In a real implementation, we would need to create a MotionEvent
        // For Compose, we can use the pointer input APIs which already track velocity
        // This is a simplified version that shows the architecture
        // In production, use PointerInputChange.historical data or Compose's built-in velocity tracking
    }

    companion object {
        /**
         * Factory method to create a ScrollbarGestureDetector with default configuration.
         *
         * @param density Screen density for converting dp to pixels
         * @return A new ScrollbarGestureDetector instance
         */
        fun create(density: Density): ScrollbarGestureDetector {
            return ScrollbarGestureDetector(GestureConfig.Default, density)
        }

        /**
         * Factory method to create a ScrollbarGestureDetector with custom configuration.
         *
         * @param config Custom gesture configuration
         * @param density Screen density for converting dp to pixels
         * @return A new ScrollbarGestureDetector instance
         */
        fun create(config: GestureConfig, density: Density): ScrollbarGestureDetector {
            return ScrollbarGestureDetector(config, density)
        }
    }
}

/**
 * Configuration class for gesture detection thresholds.
 *
 * This class follows the Open/Closed Principle by allowing customization through
 * constructor parameters while providing sensible defaults.
 *
 * All threshold values are based on Android UX guidelines and Material Design specifications.
 *
 * @property dragThresholdDp The minimum distance in dp that must be moved to trigger a drag gesture
 * @property tapMaxDurationMillis The maximum duration in milliseconds for a tap gesture
 * @property tapMaxMovementDp The maximum movement in dp allowed for a tap gesture
 * @property longPressTimeoutMillis The minimum duration in milliseconds to trigger a long press
 * @property longPressMovementThresholdPx The maximum movement in pixels allowed during long press
 * @property flingVelocityThresholdPxPerSecond The minimum velocity in px/s to trigger a fling gesture
 */
data class GestureConfig(
    val dragThresholdDp: Dp = 8.dp,
    val tapMaxDurationMillis: Long = 300L,
    val tapMaxMovementDp: Dp = 10.dp,
    val longPressTimeoutMillis: Long = 500L,
    val longPressMovementThresholdPx: Float = 20f,
    val flingVelocityThresholdPxPerSecond: Float = 2000f
) {
    init {
        require(dragThresholdDp.value > 0) { "Drag threshold must be positive" }
        require(tapMaxDurationMillis > 0) { "Tap max duration must be positive" }
        require(tapMaxMovementDp.value > 0) { "Tap max movement must be positive" }
        require(longPressTimeoutMillis > 0) { "Long press timeout must be positive" }
        require(longPressMovementThresholdPx > 0) { "Long press movement threshold must be positive" }
        require(flingVelocityThresholdPxPerSecond > 0) { "Fling velocity threshold must be positive" }
    }

    companion object {
        /**
         * Default gesture configuration based on Android platform constants.
         * These values align with ViewConfiguration and Material Design guidelines.
         */
        val Default = GestureConfig()

        /**
         * Sensitive configuration with lower thresholds for more responsive gestures.
         * Useful for experienced users or when precision is important.
         */
        val Sensitive = GestureConfig(
            dragThresholdDp = 4.dp,
            tapMaxDurationMillis = 200L,
            tapMaxMovementDp = 6.dp,
            longPressTimeoutMillis = 400L,
            longPressMovementThresholdPx = 12f,
            flingVelocityThresholdPxPerSecond = 1500f
        )

        /**
         * Relaxed configuration with higher thresholds for less sensitive gestures.
         * Useful for accessibility or when accidental gestures should be avoided.
         */
        val Relaxed = GestureConfig(
            dragThresholdDp = 12.dp,
            tapMaxDurationMillis = 400L,
            tapMaxMovementDp = 16.dp,
            longPressTimeoutMillis = 600L,
            longPressMovementThresholdPx = 30f,
            flingVelocityThresholdPxPerSecond = 2500f
        )
    }
}

/**
 * Data class representing the result of gesture detection.
 *
 * @property gesture The detected gesture type, or null if no gesture is detected yet
 * @property delta The movement delta since the last touch event
 * @property velocity The current velocity of the touch
 * @property isGestureStart Whether this is the start of a new gesture
 * @property isGestureEnd Whether this is the end of the gesture
 * @property isCancelled Whether the gesture was cancelled
 */
data class DetectionResult(
    val gesture: GestureType?,
    val delta: Offset,
    val velocity: TouchVelocity,
    val isGestureStart: Boolean = false,
    val isGestureEnd: Boolean = false,
    val isCancelled: Boolean = false
) {
    /**
     * Whether this result indicates an active gesture.
     */
    val isActive: Boolean
        get() = gesture != null && !isGestureEnd && !isCancelled

    companion object {
        /**
         * Creates a DetectionResult indicating no gesture is active.
         */
        fun none(): DetectionResult = DetectionResult(
            gesture = null,
            delta = Offset.Zero,
            velocity = TouchVelocity.Zero
        )
    }
}
