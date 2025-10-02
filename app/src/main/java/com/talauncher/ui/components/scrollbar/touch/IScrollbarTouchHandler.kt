package com.talauncher.ui.components.scrollbar.touch

import androidx.compose.ui.geometry.Offset

/**
 * Interface defining the contract for handling touch events on the scrollbar.
 *
 * This interface follows the Interface Segregation Principle by defining only the essential
 * methods needed for touch handling, and the Dependency Inversion Principle by allowing
 * clients to depend on this abstraction rather than concrete implementations.
 *
 * Implementations of this interface are responsible for:
 * - Processing raw touch events
 * - Validating touch input
 * - Detecting gestures (tap, drag, fling)
 * - Managing touch state transitions
 * - Calculating scroll positions from touch coordinates
 *
 * @see ScrollbarTouchHandler for the concrete implementation
 */
interface IScrollbarTouchHandler {

    /**
     * Handles the touch down event when the user first touches the scrollbar.
     *
     * This method should:
     * - Validate that the touch is within scrollbar bounds
     * - Initialize touch tracking state
     * - Prepare for potential drag or tap gestures
     * - Trigger haptic feedback if configured
     *
     * @param position The coordinate where the touch down occurred
     * @param timestamp The system time in milliseconds when the event occurred
     * @return TouchResult indicating how to handle the event and any state transitions
     */
    fun onTouchDown(position: Offset, timestamp: Long): TouchResult

    /**
     * Handles the touch move event when the user drags their finger on the scrollbar.
     *
     * This method should:
     * - Track the movement delta
     * - Update velocity calculations
     * - Determine if movement threshold for drag has been exceeded
     * - Calculate the new scroll position
     * - Update the moving state
     *
     * @param position The current coordinate of the touch
     * @param timestamp The system time in milliseconds when the event occurred
     * @return TouchResult indicating scroll position changes and state updates
     */
    fun onTouchMove(position: Offset, timestamp: Long): TouchResult

    /**
     * Handles the touch up event when the user releases their finger from the scrollbar.
     *
     * This method should:
     * - Determine if the gesture was a tap or drag
     * - Check for fling gestures based on release velocity
     * - Calculate final scroll position
     * - Reset touch tracking state
     * - Trigger completion haptic feedback if configured
     *
     * @param position The coordinate where the touch was released
     * @param timestamp The system time in milliseconds when the event occurred
     * @return TouchResult with final gesture type and scroll position
     */
    fun onTouchUp(position: Offset, timestamp: Long): TouchResult

    /**
     * Handles the touch cancel event when the system cancels the touch interaction.
     *
     * This can occur when:
     * - Another component intercepts the touch event
     * - The user's finger moves outside valid bounds
     * - The app goes to background during touch
     * - System gestures take priority
     *
     * This method should:
     * - Clean up any ongoing gesture tracking
     * - Reset state to idle
     * - Cancel any pending actions
     *
     * @param timestamp The system time in milliseconds when cancellation occurred
     * @return TouchResult indicating the cancellation and state reset
     */
    fun onTouchCancel(timestamp: Long): TouchResult

    /**
     * Gets the current touch state.
     *
     * @return The current TouchState
     */
    fun getCurrentState(): TouchState

    /**
     * Resets the touch handler to its initial state.
     * Useful for cleaning up state when the scrollbar is hidden or reset.
     */
    fun reset()
}

/**
 * Data class representing the result of processing a touch event.
 *
 * This class encapsulates all information needed by the scrollbar component
 * to respond to touch events, following the Single Responsibility Principle
 * by clearly separating touch processing results from touch handling logic.
 *
 * @property shouldConsumeEvent Whether the scrollbar should consume this touch event
 *                               (preventing other components from handling it)
 * @property targetItemIndex The index of the list item that should be scrolled to,
 *                           null if no scroll action is needed
 * @property targetScrollOffset The exact scroll offset to scroll to (in pixels),
 *                             null if index-based scrolling is used
 * @property stateTransition The state transition that occurred, if any
 * @property gestureType The type of gesture detected, if any
 * @property shouldTriggerHaptic Whether haptic feedback should be triggered
 * @property hapticType The type of haptic feedback to trigger
 */
data class TouchResult(
    val shouldConsumeEvent: Boolean,
    val targetItemIndex: Int? = null,
    val targetScrollOffset: Float? = null,
    val stateTransition: StateTransition? = null,
    val gestureType: GestureType? = null,
    val shouldTriggerHaptic: Boolean = false,
    val hapticType: HapticType = HapticType.LIGHT
) {
    companion object {
        /**
         * Creates a TouchResult that does not consume the event.
         * Used when the touch is outside scrollbar bounds or invalid.
         */
        fun notConsumed(): TouchResult = TouchResult(
            shouldConsumeEvent = false
        )

        /**
         * Creates a TouchResult that consumes the event but performs no action.
         * Used during intermediate touch states.
         */
        fun consumed(): TouchResult = TouchResult(
            shouldConsumeEvent = true
        )

        /**
         * Creates a TouchResult for scrolling to a specific item index.
         *
         * @param itemIndex The index to scroll to
         * @param gestureType The gesture that triggered the scroll
         * @param triggerHaptic Whether to trigger haptic feedback
         */
        fun scrollToItem(
            itemIndex: Int,
            gestureType: GestureType,
            triggerHaptic: Boolean = false
        ): TouchResult = TouchResult(
            shouldConsumeEvent = true,
            targetItemIndex = itemIndex,
            gestureType = gestureType,
            shouldTriggerHaptic = triggerHaptic
        )

        /**
         * Creates a TouchResult for scrolling to a specific offset.
         *
         * @param offset The scroll offset in pixels
         * @param gestureType The gesture that triggered the scroll
         * @param triggerHaptic Whether to trigger haptic feedback
         */
        fun scrollToOffset(
            offset: Float,
            gestureType: GestureType,
            triggerHaptic: Boolean = false
        ): TouchResult = TouchResult(
            shouldConsumeEvent = true,
            targetScrollOffset = offset,
            gestureType = gestureType,
            shouldTriggerHaptic = triggerHaptic
        )
    }
}

/**
 * Sealed class representing state transitions in the touch handler.
 *
 * State transitions follow a defined state machine:
 * - Idle -> Active (on touch down)
 * - Active -> Dragging (on movement threshold exceeded)
 * - Active -> Tapping (on quick release without movement)
 * - Dragging -> Released (on touch up)
 * - Any -> Cancelled (on touch cancel)
 * - Any -> Idle (on gesture completion)
 */
sealed class StateTransition {
    /**
     * Transition from Idle to Active state when touch down occurs.
     */
    data object IdleToActive : StateTransition()

    /**
     * Transition from Active to Dragging state when drag threshold is exceeded.
     */
    data object ActiveToDragging : StateTransition()

    /**
     * Transition from Active to Tapping state when quick tap is detected.
     */
    data object ActiveToTapping : StateTransition()

    /**
     * Transition from Dragging to Released state when touch is released during drag.
     */
    data class DraggingToReleased(val hasFling: Boolean) : StateTransition()

    /**
     * Transition to Cancelled state when touch is cancelled by system.
     */
    data object ToCancelled : StateTransition()

    /**
     * Transition back to Idle state when gesture completes.
     */
    data object ToIdle : StateTransition()
}

/**
 * Enum representing different types of gestures that can be detected on the scrollbar.
 */
enum class GestureType {
    /**
     * A quick tap gesture on the scrollbar.
     * Typically used to jump to a specific section.
     */
    TAP,

    /**
     * A drag gesture on the scrollbar.
     * Used for smooth scrolling through content.
     */
    DRAG,

    /**
     * A fling gesture (fast drag with momentum).
     * Can be used for animated scrolling with deceleration.
     */
    FLING,

    /**
     * A long press gesture on the scrollbar.
     * Can be used to show additional UI or enter a special mode.
     */
    LONG_PRESS
}

/**
 * Enum representing different types of haptic feedback.
 * Maps to Android HapticFeedbackConstants.
 */
enum class HapticType {
    /**
     * Light haptic feedback for subtle interactions.
     */
    LIGHT,

    /**
     * Medium haptic feedback for standard interactions.
     */
    MEDIUM,

    /**
     * Strong haptic feedback for significant interactions.
     */
    STRONG,

    /**
     * Haptic feedback for confirming a selection or action.
     */
    CONFIRM,

    /**
     * Haptic feedback for rejecting or cancelling an action.
     */
    REJECT
}
