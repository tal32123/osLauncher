package com.talauncher.ui.components.scrollbar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Core state coordinator for the scrollbar component.
 *
 * This class implements the Mediator Pattern, coordinating between:
 * - Touch input handling
 * - Scroll position calculations
 * - Animation state management
 * - Visual state updates
 *
 * Architecture follows SOLID principles:
 * - Single Responsibility: Manages only scrollbar state coordination
 * - Open/Closed: Can be extended through composition, not modification
 * - Liskov Substitution: Can be replaced with custom implementations
 * - Interface Segregation: Provides focused, minimal interface
 * - Dependency Inversion: Depends on abstractions (config, callbacks)
 *
 * @param config Scrollbar configuration
 * @param density Current composition density for dp/px conversion
 * @param coroutineScope Scope for launching animations and delayed actions
 * @param onScrollToPosition Callback when scroll position should change
 */
class ScrollbarStateManager(
    private val config: ScrollbarConfig,
    private val density: Density,
    private val coroutineScope: CoroutineScope,
    private val onScrollToPosition: (Float) -> Unit
) {
    /**
     * Current interaction state.
     */
    private var _interactionState by mutableStateOf<ScrollbarInteractionState>(
        ScrollbarInteractionState.Idle
    )
    val interactionState: ScrollbarInteractionState
        get() = _interactionState

    /**
     * Current touch state.
     */
    private var _touchState by mutableStateOf<TouchState>(TouchState.None)
    val touchState: TouchState
        get() = _touchState

    /**
     * Current scroll position information.
     */
    private var _scrollPosition by mutableStateOf(
        ScrollPosition(
            offset = 0f,
            maxOffset = 0f,
            viewportHeight = 0f,
            contentHeight = 0f
        )
    )
    val scrollPosition: ScrollPosition
        get() = _scrollPosition

    /**
     * Animatable for smooth alpha transitions.
     */
    private val alphaAnimatable = Animatable(0f)

    /**
     * Current visibility state of the scrollbar.
     */
    private var _isVisible by mutableStateOf(false)
    val isVisible: Boolean
        get() = _isVisible

    /**
     * Job for auto-hide delay.
     */
    private var autoHideJob: Job? = null

    /**
     * Updates the scroll position information.
     *
     * This should be called whenever the scroll state changes.
     *
     * @param offset Current scroll offset
     * @param maxOffset Maximum scroll offset
     * @param viewportHeight Height of the viewport
     * @param contentHeight Total content height
     */
    fun updateScrollPosition(
        offset: Float,
        maxOffset: Float,
        viewportHeight: Float,
        contentHeight: Float
    ) {
        _scrollPosition = ScrollPosition(
            offset = offset.coerceAtLeast(0f),
            maxOffset = maxOffset.coerceAtLeast(0f),
            viewportHeight = viewportHeight.coerceAtLeast(0f),
            contentHeight = contentHeight.coerceAtLeast(0f)
        )

        // Show scrollbar when content changes or scrolling occurs
        if (_scrollPosition.isScrollable && _interactionState !is ScrollbarInteractionState.Dragging) {
            showScrollbar()
        }
    }

    /**
     * Calculates visual state based on current scroll position and interaction state.
     *
     * This method implements the calculation logic for thumb position and height,
     * following the Single Responsibility Principle.
     *
     * @param trackHeight Available height for the scrollbar track
     * @return Current visual state for rendering
     */
    fun calculateVisualState(trackHeight: Float): ScrollbarVisualState {
        if (!_scrollPosition.isScrollable || trackHeight <= 0f) {
            return ScrollbarVisualState(
                thumbOffsetY = 0f,
                thumbHeight = 0f,
                alpha = 0f,
                isVisible = false,
                currentColor = config.thumbColor
            )
        }

        // Calculate thumb height based on viewport ratio
        val minHeightPx = with(density) { config.thumbMinHeight.toPx() }
        val maxHeightPx = config.thumbMaxHeight?.let { with(density) { it.toPx() } }

        val calculatedHeight = (trackHeight * _scrollPosition.viewportRatio).coerceAtLeast(minHeightPx)
        val thumbHeight = if (maxHeightPx != null) {
            calculatedHeight.coerceAtMost(maxHeightPx)
        } else {
            calculatedHeight
        }.coerceAtMost(trackHeight)

        // Calculate thumb position
        val availableTrackHeight = trackHeight - thumbHeight
        val thumbOffsetY = if (_scrollPosition.maxOffset > 0f) {
            availableTrackHeight * _scrollPosition.normalizedPosition
        } else {
            0f
        }

        // Determine current color based on interaction state
        val currentColor = when (_interactionState) {
            is ScrollbarInteractionState.Dragging -> config.thumbColorDragging
            else -> config.thumbColor
        }

        return ScrollbarVisualState(
            thumbOffsetY = thumbOffsetY.coerceAtLeast(0f),
            thumbHeight = thumbHeight,
            alpha = alphaAnimatable.value,
            isVisible = _isVisible,
            currentColor = currentColor
        )
    }

    /**
     * Handles touch down event on the scrollbar.
     *
     * @param touchY Y position of the touch
     * @param currentThumbOffsetY Current thumb offset
     */
    fun handleTouchDown(touchY: Float, currentThumbOffsetY: Float) {
        cancelAutoHide()

        _touchState = TouchState.Started(
            startY = touchY,
            startScrollOffset = _scrollPosition.offset
        )

        _interactionState = ScrollbarInteractionState.Dragging(
            startY = touchY,
            currentY = touchY
        )

        showScrollbar()
    }

    /**
     * Handles touch move event during dragging.
     *
     * @param touchY Current Y position of the touch
     * @param trackHeight Total height of the scrollbar track
     */
    fun handleTouchMove(touchY: Float, trackHeight: Float) {
        val currentState = _interactionState as? ScrollbarInteractionState.Dragging ?: return

        _interactionState = currentState.copy(currentY = touchY)

        val startedState = _touchState as? TouchState.Started
        val movingState = _touchState as? TouchState.Moving

        val initialScrollOffset = startedState?.startScrollOffset
            ?: movingState?.initialScrollOffset
            ?: _scrollPosition.offset

        val deltaY = touchY - (startedState?.startY ?: movingState?.currentY ?: touchY)

        _touchState = TouchState.Moving(
            currentY = touchY,
            deltaY = deltaY,
            initialScrollOffset = initialScrollOffset
        )

        // Calculate new scroll position based on thumb drag
        if (trackHeight > 0f && _scrollPosition.maxOffset > 0f) {
            val visualState = calculateVisualState(trackHeight)
            val availableTrackHeight = trackHeight - visualState.thumbHeight

            if (availableTrackHeight > 0f) {
                // Calculate the target thumb offset
                val currentThumbY = currentState.startY + (touchY - currentState.startY)
                val normalizedThumbPosition = (currentThumbY / availableTrackHeight).coerceIn(0f, 1f)

                // Map to scroll offset
                val targetScrollOffset = normalizedThumbPosition * _scrollPosition.maxOffset

                onScrollToPosition(targetScrollOffset)
            }
        }
    }

    /**
     * Handles touch up event, ending the drag interaction.
     *
     * @param velocityY Final velocity when touch ended
     */
    fun handleTouchUp(velocityY: Float = 0f) {
        _touchState = TouchState.Ended(velocityY)
        _interactionState = ScrollbarInteractionState.Idle

        // Schedule auto-hide if enabled
        if (config.autoHideEnabled) {
            scheduleAutoHide()
        }

        // Reset touch state after a brief delay
        coroutineScope.launch {
            delay(100)
            if (_touchState is TouchState.Ended) {
                _touchState = TouchState.None
            }
        }
    }

    /**
     * Handles tap on the scrollbar track (outside the thumb).
     *
     * @param tapY Y position of the tap
     * @param trackHeight Total track height
     */
    fun handleTrackTap(tapY: Float, trackHeight: Float) {
        if (trackHeight <= 0f || _scrollPosition.maxOffset <= 0f) return

        val visualState = calculateVisualState(trackHeight)
        val availableTrackHeight = trackHeight - visualState.thumbHeight

        if (availableTrackHeight > 0f) {
            val normalizedPosition = (tapY / availableTrackHeight).coerceIn(0f, 1f)
            val targetScrollOffset = normalizedPosition * _scrollPosition.maxOffset

            onScrollToPosition(targetScrollOffset)
        }

        showScrollbar()
    }

    /**
     * Shows the scrollbar with fade-in animation.
     */
    private fun showScrollbar() {
        _isVisible = true
        coroutineScope.launch {
            alphaAnimatable.animateTo(
                targetValue = ScrollbarDefaults.AlphaValues.Visible,
                animationSpec = ScrollbarDefaults.AnimationSpecs.FadeInOut
            )
        }
    }

    /**
     * Hides the scrollbar with fade-out animation.
     */
    fun hideScrollbar() {
        coroutineScope.launch {
            alphaAnimatable.animateTo(
                targetValue = ScrollbarDefaults.AlphaValues.Hidden,
                animationSpec = ScrollbarDefaults.AnimationSpecs.FadeInOut
            )
            _isVisible = false
        }
    }

    /**
     * Schedules auto-hide after the configured delay.
     */
    private fun scheduleAutoHide() {
        cancelAutoHide()
        autoHideJob = coroutineScope.launch {
            _interactionState = ScrollbarInteractionState.AutoHiding(config.autoHideDelayMillis)
            delay(config.autoHideDelayMillis)
            if (_interactionState is ScrollbarInteractionState.AutoHiding) {
                hideScrollbar()
                _interactionState = ScrollbarInteractionState.Idle
            }
        }
    }

    /**
     * Cancels the scheduled auto-hide.
     */
    private fun cancelAutoHide() {
        autoHideJob?.cancel()
        autoHideJob = null
        if (_interactionState is ScrollbarInteractionState.AutoHiding) {
            _interactionState = ScrollbarInteractionState.Idle
        }
    }

    /**
     * Forces the scrollbar to become visible and cancels auto-hide.
     * Useful when programmatically scrolling.
     */
    fun forceShow() {
        cancelAutoHide()
        showScrollbar()
    }

    /**
     * Resets the auto-hide timer, keeping the scrollbar visible.
     */
    fun resetAutoHideTimer() {
        if (config.autoHideEnabled && _isVisible) {
            scheduleAutoHide()
        }
    }
}

/**
 * Remembers a ScrollbarStateManager instance across recompositions.
 *
 * This composable function follows the Composition Local pattern,
 * creating and remembering the state manager with the current composition values.
 *
 * @param config Scrollbar configuration
 * @param coroutineScope Coroutine scope for animations
 * @param onScrollToPosition Callback for scroll position changes
 * @return Remembered ScrollbarStateManager instance
 */
@Composable
fun rememberScrollbarStateManager(
    config: ScrollbarConfig = ScrollbarDefaults.config(),
    coroutineScope: CoroutineScope,
    onScrollToPosition: (Float) -> Unit
): ScrollbarStateManager {
    val density = LocalDensity.current

    return remember(config, coroutineScope) {
        ScrollbarStateManager(
            config = config,
            density = density,
            coroutineScope = coroutineScope,
            onScrollToPosition = onScrollToPosition
        )
    }
}

/**
 * Creates a derived state for visual rendering.
 *
 * This follows the Observer Pattern, automatically updating when dependencies change.
 *
 * @param stateManager The state manager instance
 * @param trackHeight Current track height
 * @return State containing the current visual state
 */
@Composable
fun rememberScrollbarVisualState(
    stateManager: ScrollbarStateManager,
    trackHeight: Float
): State<ScrollbarVisualState> {
    return remember(stateManager, trackHeight) {
        derivedStateOf {
            stateManager.calculateVisualState(trackHeight)
        }
    }
}
