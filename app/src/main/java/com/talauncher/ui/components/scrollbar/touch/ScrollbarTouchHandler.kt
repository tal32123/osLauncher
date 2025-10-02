package com.talauncher.ui.components.scrollbar.touch

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Density

/**
 * Concrete implementation of the scrollbar touch handler.
 *
 * This class orchestrates touch event processing by:
 * - Delegating gesture recognition to ScrollbarGestureDetector
 * - Delegating position calculations to IScrollPositionCalculator
 * - Managing touch state transitions
 * - Validating touch input
 * - Determining when to consume events
 *
 * This implementation follows SOLID principles:
 * - Single Responsibility: Focuses on coordinating touch handling
 * - Open/Closed: Extensible through dependency injection
 * - Liskov Substitution: Fully implements IScrollbarTouchHandler contract
 * - Interface Segregation: Depends only on specific interfaces needed
 * - Dependency Inversion: Depends on abstractions (interfaces) not concrete implementations
 *
 * @property gestureDetector The gesture detector for recognizing touch gestures
 * @property positionCalculator The calculator for converting touch positions to scroll positions
 * @property scrollbarBounds The current bounds of the scrollbar
 * @property totalItems The total number of items in the scrollable list
 * @property config Configuration for touch handling behavior
 */
class ScrollbarTouchHandler(
    private val gestureDetector: ScrollbarGestureDetector,
    private val positionCalculator: IScrollPositionCalculator,
    private var scrollbarBounds: Rect,
    private var totalItems: Int,
    private val config: TouchHandlerConfig = TouchHandlerConfig.Default
) : IScrollbarTouchHandler {

    private var currentState: TouchState = TouchState.Idle
    private var lastGestureType: GestureType? = null

    /**
     * Constructor that creates a ScrollbarTouchHandler with default gesture detector.
     *
     * @param positionCalculator The calculator for position mapping
     * @param scrollbarBounds The bounds of the scrollbar
     * @param totalItems The total number of items in the list
     * @param density Screen density for gesture detection
     * @param config Configuration for touch handling behavior
     */
    constructor(
        positionCalculator: IScrollPositionCalculator,
        scrollbarBounds: Rect,
        totalItems: Int,
        density: Density,
        config: TouchHandlerConfig = TouchHandlerConfig.Default
    ) : this(
        gestureDetector = ScrollbarGestureDetector.create(density),
        positionCalculator = positionCalculator,
        scrollbarBounds = scrollbarBounds,
        totalItems = totalItems,
        config = config
    )

    override fun onTouchDown(position: Offset, timestamp: Long): TouchResult {
        // Validate touch is within scrollbar bounds
        if (!positionCalculator.isWithinBounds(position, scrollbarBounds)) {
            return TouchResult.notConsumed()
        }

        // Initialize gesture detection
        gestureDetector.onTouchDown(position, timestamp)

        // Update state
        currentState = TouchState.Down(position, timestamp)

        // Determine if we should perform immediate action or wait for gesture completion
        return if (config.enableTapToScroll) {
            // For tap-to-scroll, we calculate position immediately but don't scroll yet
            val itemIndex = positionCalculator.calculateItemIndex(position, scrollbarBounds, totalItems)
            TouchResult(
                shouldConsumeEvent = true,
                targetItemIndex = itemIndex,
                stateTransition = StateTransition.IdleToActive,
                shouldTriggerHaptic = config.enableHapticFeedback,
                hapticType = HapticType.LIGHT
            )
        } else {
            // For drag-only mode, just consume the event without immediate action
            TouchResult(
                shouldConsumeEvent = true,
                stateTransition = StateTransition.IdleToActive,
                shouldTriggerHaptic = config.enableHapticFeedback,
                hapticType = HapticType.LIGHT
            )
        }
    }

    override fun onTouchMove(position: Offset, timestamp: Long): TouchResult {
        // Validate we're in an active touch state
        if (currentState is TouchState.Idle || currentState is TouchState.Cancelled) {
            return TouchResult.notConsumed()
        }

        // Process gesture detection
        val detectionResult = gestureDetector.onTouchMove(position, timestamp)

        // Validate touch is still within bounds (with some tolerance for drag)
        val isInBounds = positionCalculator.isWithinBounds(position, scrollbarBounds)
        if (!isInBounds && !config.allowDragOutsideBounds) {
            // Touch moved outside bounds and we don't allow that
            return handleTouchOutOfBounds(position, timestamp)
        }

        // Update state based on gesture
        when (detectionResult.gesture) {
            GestureType.DRAG -> {
                if (detectionResult.isGestureStart) {
                    // Transition to dragging state
                    val startPos = (currentState as? TouchState.Down)?.position
                        ?: position
                    currentState = TouchState.Moving(
                        startPosition = startPos,
                        currentPosition = position,
                        velocity = detectionResult.velocity.toOffset(),
                        timestamp = timestamp
                    )

                    // Calculate scroll position
                    val itemIndex = if (isInBounds) {
                        positionCalculator.calculateItemIndex(position, scrollbarBounds, totalItems)
                    } else null

                    return TouchResult(
                        shouldConsumeEvent = true,
                        targetItemIndex = itemIndex,
                        stateTransition = StateTransition.ActiveToDragging,
                        gestureType = GestureType.DRAG,
                        shouldTriggerHaptic = config.enableHapticFeedback && config.enableDragHaptic,
                        hapticType = HapticType.LIGHT
                    )
                } else {
                    // Continue dragging
                    currentState = (currentState as? TouchState.Moving)?.copy(
                        currentPosition = position,
                        velocity = detectionResult.velocity.toOffset(),
                        timestamp = timestamp
                    ) ?: TouchState.Moving(
                        startPosition = position,
                        currentPosition = position,
                        velocity = detectionResult.velocity.toOffset(),
                        timestamp = timestamp
                    )

                    // Calculate scroll position during drag
                    val itemIndex = if (isInBounds) {
                        positionCalculator.calculateItemIndex(position, scrollbarBounds, totalItems)
                    } else null

                    return TouchResult(
                        shouldConsumeEvent = true,
                        targetItemIndex = itemIndex,
                        gestureType = GestureType.DRAG,
                        shouldTriggerHaptic = false // No haptic during continuous drag
                    )
                }
            }

            GestureType.LONG_PRESS -> {
                // Update to pressing state
                val startTimestamp = (currentState as? TouchState.Down)?.timestamp
                    ?: timestamp
                currentState = TouchState.Pressing(
                    position = position,
                    startTimestamp = startTimestamp,
                    currentTimestamp = timestamp
                )

                return TouchResult(
                    shouldConsumeEvent = true,
                    gestureType = GestureType.LONG_PRESS,
                    shouldTriggerHaptic = config.enableHapticFeedback,
                    hapticType = HapticType.MEDIUM
                )
            }

            else -> {
                // No specific gesture detected yet, but we're tracking movement
                return TouchResult.consumed()
            }
        }
    }

    override fun onTouchUp(position: Offset, timestamp: Long): TouchResult {
        // Validate we're in an active touch state
        if (currentState is TouchState.Idle || currentState is TouchState.Cancelled) {
            return TouchResult.notConsumed()
        }

        // Process final gesture detection
        val detectionResult = gestureDetector.onTouchUp(position, timestamp)
        lastGestureType = detectionResult.gesture

        // Update to released state
        currentState = TouchState.Released(
            position = position,
            velocity = detectionResult.velocity.toOffset(),
            timestamp = timestamp
        )

        // Calculate final scroll position if within bounds
        val isInBounds = positionCalculator.isWithinBounds(position, scrollbarBounds)
        val itemIndex = if (isInBounds) {
            positionCalculator.calculateItemIndex(position, scrollbarBounds, totalItems)
        } else null

        // Determine result based on gesture type
        val result = when (detectionResult.gesture) {
            GestureType.TAP -> {
                // Handle tap gesture
                if (config.enableTapToScroll && itemIndex != null) {
                    TouchResult.scrollToItem(
                        itemIndex = itemIndex,
                        gestureType = GestureType.TAP,
                        triggerHaptic = config.enableHapticFeedback
                    ).copy(
                        stateTransition = StateTransition.ActiveToTapping,
                        hapticType = HapticType.CONFIRM
                    )
                } else {
                    TouchResult(
                        shouldConsumeEvent = true,
                        gestureType = GestureType.TAP,
                        stateTransition = StateTransition.ToIdle
                    )
                }
            }

            GestureType.DRAG -> {
                // Handle drag gesture completion
                TouchResult(
                    shouldConsumeEvent = true,
                    targetItemIndex = itemIndex,
                    gestureType = GestureType.DRAG,
                    stateTransition = StateTransition.DraggingToReleased(hasFling = false),
                    shouldTriggerHaptic = config.enableHapticFeedback,
                    hapticType = HapticType.LIGHT
                )
            }

            GestureType.FLING -> {
                // Handle fling gesture
                if (config.enableFling && itemIndex != null) {
                    TouchResult(
                        shouldConsumeEvent = true,
                        targetItemIndex = itemIndex,
                        gestureType = GestureType.FLING,
                        stateTransition = StateTransition.DraggingToReleased(hasFling = true),
                        shouldTriggerHaptic = config.enableHapticFeedback,
                        hapticType = HapticType.MEDIUM
                    )
                } else {
                    // Fling disabled, treat as regular drag
                    TouchResult(
                        shouldConsumeEvent = true,
                        targetItemIndex = itemIndex,
                        gestureType = GestureType.DRAG,
                        stateTransition = StateTransition.DraggingToReleased(hasFling = false)
                    )
                }
            }

            GestureType.LONG_PRESS -> {
                // Handle long press release
                TouchResult(
                    shouldConsumeEvent = true,
                    gestureType = GestureType.LONG_PRESS,
                    stateTransition = StateTransition.ToIdle
                )
            }

            null -> {
                // No gesture detected, just release
                TouchResult(
                    shouldConsumeEvent = true,
                    stateTransition = StateTransition.ToIdle
                )
            }
        }

        // Reset to idle state
        currentState = TouchState.Idle

        return result
    }

    override fun onTouchCancel(timestamp: Long): TouchResult {
        // Clean up gesture detection
        gestureDetector.onTouchCancel()

        // Update state
        val lastPosition = when (val state = currentState) {
            is TouchState.Down -> state.position
            is TouchState.Moving -> state.currentPosition
            is TouchState.Pressing -> state.position
            is TouchState.Released -> state.position
            is TouchState.Cancelled -> state.lastPosition
            is TouchState.Idle -> Offset.Zero
        }

        currentState = TouchState.Cancelled(
            lastPosition = lastPosition,
            timestamp = timestamp
        )

        // Return cancellation result
        val result = TouchResult(
            shouldConsumeEvent = true,
            stateTransition = StateTransition.ToCancelled
        )

        // Reset to idle
        currentState = TouchState.Idle

        return result
    }

    override fun getCurrentState(): TouchState {
        return currentState
    }

    override fun reset() {
        currentState = TouchState.Idle
        lastGestureType = null
        // Note: gestureDetector maintains its own internal state management
    }

    /**
     * Updates the scrollbar bounds. Should be called when the scrollbar size or position changes.
     *
     * @param bounds The new bounds of the scrollbar
     */
    fun updateScrollbarBounds(bounds: Rect) {
        this.scrollbarBounds = bounds
    }

    /**
     * Updates the total number of items in the list. Should be called when the list size changes.
     *
     * @param totalItems The new total number of items
     */
    fun updateTotalItems(totalItems: Int) {
        require(totalItems >= 0) { "Total items must be non-negative, got $totalItems" }
        this.totalItems = totalItems
    }

    /**
     * Handles the case when touch moves outside scrollbar bounds.
     *
     * @param position The position where the touch moved to
     * @param timestamp The timestamp of the event
     * @return TouchResult indicating how to handle the out-of-bounds event
     */
    private fun handleTouchOutOfBounds(position: Offset, timestamp: Long): TouchResult {
        return if (config.cancelOnMoveOutsideBounds) {
            // Cancel the gesture
            onTouchCancel(timestamp)
        } else {
            // Continue consuming but don't update scroll position
            TouchResult.consumed()
        }
    }

    /**
     * Gets the last detected gesture type.
     * Useful for analytics or debugging.
     *
     * @return The last detected gesture type, or null if no gesture has occurred
     */
    fun getLastGesture(): GestureType? = lastGestureType
}

/**
 * Configuration class for touch handler behavior.
 *
 * This class follows the Open/Closed Principle by allowing customization through
 * constructor parameters while providing sensible defaults.
 *
 * @property enableTapToScroll Whether tapping on the scrollbar should scroll to that position
 * @property enableFling Whether fling gestures are enabled
 * @property enableHapticFeedback Whether haptic feedback is enabled
 * @property enableDragHaptic Whether to provide haptic feedback during drag start
 * @property allowDragOutsideBounds Whether dragging outside scrollbar bounds is allowed
 * @property cancelOnMoveOutsideBounds Whether to cancel gesture when moving outside bounds
 */
data class TouchHandlerConfig(
    val enableTapToScroll: Boolean = true,
    val enableFling: Boolean = true,
    val enableHapticFeedback: Boolean = true,
    val enableDragHaptic: Boolean = false,
    val allowDragOutsideBounds: Boolean = true,
    val cancelOnMoveOutsideBounds: Boolean = false
) {
    init {
        // Validate conflicting configurations
        if (cancelOnMoveOutsideBounds && allowDragOutsideBounds) {
            throw IllegalArgumentException(
                "Cannot both allow drag outside bounds and cancel on move outside bounds"
            )
        }
    }

    companion object {
        /**
         * Default configuration with standard scrollbar behavior.
         */
        val Default = TouchHandlerConfig()

        /**
         * Configuration optimized for touch accessibility.
         * More forgiving with larger touch targets and tolerance for movement.
         */
        val Accessible = TouchHandlerConfig(
            enableTapToScroll = true,
            enableFling = false,
            enableHapticFeedback = true,
            enableDragHaptic = true,
            allowDragOutsideBounds = true,
            cancelOnMoveOutsideBounds = false
        )

        /**
         * Configuration for precise control.
         * Stricter bounds and no fling for precise scrolling.
         */
        val Precise = TouchHandlerConfig(
            enableTapToScroll = true,
            enableFling = false,
            enableHapticFeedback = true,
            enableDragHaptic = false,
            allowDragOutsideBounds = false,
            cancelOnMoveOutsideBounds = true
        )

        /**
         * Configuration with minimal features for simple drag-only scrollbar.
         */
        val Minimal = TouchHandlerConfig(
            enableTapToScroll = false,
            enableFling = false,
            enableHapticFeedback = false,
            enableDragHaptic = false,
            allowDragOutsideBounds = true,
            cancelOnMoveOutsideBounds = false
        )
    }
}
