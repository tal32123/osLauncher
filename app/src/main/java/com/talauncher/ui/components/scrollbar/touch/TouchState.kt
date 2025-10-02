package com.talauncher.ui.components.scrollbar.touch

import androidx.compose.ui.geometry.Offset

/**
 * Sealed class hierarchy representing the various touch states for the scrollbar.
 *
 * This hierarchy follows the Single Responsibility Principle by encapsulating touch state
 * data and the Open/Closed Principle by allowing extension through sealed class variants
 * without modifying existing code.
 *
 * Touch states follow the Android touch event lifecycle:
 * Idle -> Down -> (Moving | Pressing) -> Released -> Idle
 *
 * @see android.view.MotionEvent
 */
sealed class TouchState {

    /**
     * Initial state when no touch interaction is occurring.
     * This is the default state when the scrollbar is not being interacted with.
     */
    data object Idle : TouchState()

    /**
     * State when the user first touches down on the scrollbar.
     *
     * @property position The coordinate where the touch down occurred
     * @property timestamp The system time in milliseconds when the touch occurred
     */
    data class Down(
        val position: Offset,
        val timestamp: Long
    ) : TouchState()

    /**
     * State when the user is actively moving their finger while touching the scrollbar.
     * This state is used during drag operations.
     *
     * @property startPosition The coordinate where the drag started
     * @property currentPosition The current coordinate of the touch
     * @property velocity The current velocity of the movement in pixels per second
     * @property timestamp The system time in milliseconds when this state was captured
     */
    data class Moving(
        val startPosition: Offset,
        val currentPosition: Offset,
        val velocity: Offset,
        val timestamp: Long
    ) : TouchState()

    /**
     * State when the user is pressing and holding on the scrollbar without significant movement.
     * This state is used to detect long press gestures.
     *
     * @property position The coordinate where the press is occurring
     * @property startTimestamp The system time in milliseconds when the press started
     * @property currentTimestamp The current system time in milliseconds
     */
    data class Pressing(
        val position: Offset,
        val startTimestamp: Long,
        val currentTimestamp: Long
    ) : TouchState() {
        /**
         * Calculates the duration of the press in milliseconds.
         */
        val pressDuration: Long
            get() = currentTimestamp - startTimestamp
    }

    /**
     * State when the user releases their touch from the scrollbar.
     * This is a transitional state before returning to Idle.
     *
     * @property position The coordinate where the touch was released
     * @property velocity The velocity at the time of release (used for fling detection)
     * @property timestamp The system time in milliseconds when the release occurred
     */
    data class Released(
        val position: Offset,
        val velocity: Offset,
        val timestamp: Long
    ) : TouchState()

    /**
     * State when the touch interaction is cancelled by the system.
     * This can occur when another component intercepts the touch event or
     * when the user's finger moves outside the scrollbar bounds.
     *
     * @property lastPosition The last known coordinate before cancellation
     * @property timestamp The system time in milliseconds when cancellation occurred
     */
    data class Cancelled(
        val lastPosition: Offset,
        val timestamp: Long
    ) : TouchState()
}

/**
 * Data class containing touch velocity information.
 * Used for gesture detection, particularly fling gestures.
 *
 * @property xVelocity Horizontal velocity in pixels per second
 * @property yVelocity Vertical velocity in pixels per second
 */
data class TouchVelocity(
    val xVelocity: Float,
    val yVelocity: Float
) {
    /**
     * Calculates the magnitude of the velocity vector.
     * Useful for determining if a fling gesture has occurred.
     */
    val magnitude: Float
        get() = kotlin.math.sqrt(xVelocity * xVelocity + yVelocity * yVelocity)

    /**
     * Converts to Compose Offset for compatibility with other APIs.
     */
    fun toOffset(): Offset = Offset(xVelocity, yVelocity)

    companion object {
        /**
         * Zero velocity (no movement).
         */
        val Zero = TouchVelocity(0f, 0f)
    }
}

/**
 * Data class representing a touch event with its associated metadata.
 * This class encapsulates all information needed to process a touch event.
 *
 * @property position The coordinate of the touch
 * @property timestamp The system time in milliseconds when the event occurred
 * @property action The type of touch action (DOWN, MOVE, UP, CANCEL)
 * @property pressure The pressure applied to the touch (0.0 to 1.0, where 1.0 is normal)
 */
data class TouchEvent(
    val position: Offset,
    val timestamp: Long,
    val action: TouchAction,
    val pressure: Float = 1.0f
) {
    init {
        require(pressure in 0f..1f) { "Pressure must be between 0.0 and 1.0, got $pressure" }
    }
}

/**
 * Enum representing the different types of touch actions.
 * Maps to Android MotionEvent action types.
 */
enum class TouchAction {
    /**
     * Finger touched down on the screen.
     * Corresponds to MotionEvent.ACTION_DOWN.
     */
    DOWN,

    /**
     * Finger moved while touching the screen.
     * Corresponds to MotionEvent.ACTION_MOVE.
     */
    MOVE,

    /**
     * Finger lifted from the screen.
     * Corresponds to MotionEvent.ACTION_UP.
     */
    UP,

    /**
     * Touch event was cancelled by the system.
     * Corresponds to MotionEvent.ACTION_CANCEL.
     */
    CANCEL
}
