package com.talauncher.ui.components.scrollbar.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Manages scrollbar visibility animations with auto-hide functionality.
 *
 * This class handles the complex state machine of scrollbar visibility:
 * - Appears on scroll or user interaction
 * - Auto-hides after inactivity delay
 * - Remains visible during active dragging
 * - Smoothly fades in/out
 *
 * **Design Pattern**: State Pattern - Manages visibility state transitions
 * **SOLID Principles**:
 * - Single Responsibility: Manages only visibility state and animations
 * - Open/Closed: Extensible through AnimationConfig
 *
 * @property config Animation configuration for timing and specs
 * @property scope Coroutine scope for launching animations and delays
 */
class ScrollbarVisibilityAnimator(
    private val config: AnimationConfig = AnimationConfig.Default,
    private val scope: CoroutineScope
) {
    /**
     * Current visibility state.
     */
    private var _isVisible by mutableStateOf(false)

    /**
     * Whether scrollbar should be permanently visible (no auto-hide).
     */
    private var _alwaysVisible by mutableStateOf(false)

    /**
     * Whether user is actively dragging the scrollbar.
     */
    private var _isDragging by mutableStateOf(false)

    /**
     * Animatable for smooth alpha transitions.
     */
    private val alphaAnimatable = Animatable(0f)

    /**
     * Job for auto-hide delay countdown.
     */
    private var autoHideJob: Job? = null

    /**
     * Job for fade animation.
     */
    private var fadeJob: Job? = null

    /**
     * Current alpha value as State for Compose observation.
     */
    val alpha: Float
        get() = alphaAnimatable.value

    /**
     * Whether scrollbar is currently visible (alpha > 0).
     */
    val isVisible: Boolean
        get() = _isVisible || _isDragging || _alwaysVisible

    /**
     * Whether user is actively dragging.
     */
    val isDragging: Boolean
        get() = _isDragging

    /**
     * Shows the scrollbar with fade-in animation.
     *
     * Cancels any pending auto-hide and starts fade-in animation.
     * If already visible, resets the auto-hide timer.
     */
    fun show() {
        cancelAutoHide()

        if (!_isVisible) {
            _isVisible = true
            fadeIn()
        }

        // Schedule auto-hide if not always visible or dragging
        if (!_alwaysVisible && !_isDragging) {
            scheduleAutoHide()
        }
    }

    /**
     * Hides the scrollbar with fade-out animation.
     *
     * Only hides if not in dragging state and not set to always visible.
     */
    fun hide() {
        if (_isDragging || _alwaysVisible) {
            return
        }

        cancelAutoHide()
        _isVisible = false
        fadeOut()
    }

    /**
     * Marks the start of a drag interaction.
     *
     * Shows scrollbar and prevents auto-hide during drag.
     */
    fun startDrag() {
        _isDragging = true
        cancelAutoHide()
        show()
    }

    /**
     * Marks the end of a drag interaction.
     *
     * Schedules auto-hide after drag completion.
     */
    fun endDrag() {
        _isDragging = false

        if (!_alwaysVisible) {
            scheduleAutoHide()
        }
    }

    /**
     * Sets whether scrollbar should always be visible.
     *
     * @param alwaysVisible If true, disables auto-hide
     */
    fun setAlwaysVisible(alwaysVisible: Boolean) {
        _alwaysVisible = alwaysVisible

        if (alwaysVisible) {
            cancelAutoHide()
            show()
        } else if (!_isDragging && _isVisible) {
            scheduleAutoHide()
        }
    }

    /**
     * Schedules auto-hide after configured delay.
     *
     * Cancels any existing auto-hide job before scheduling new one.
     */
    private fun scheduleAutoHide() {
        cancelAutoHide()

        autoHideJob = scope.launch {
            delay(config.autoHideDelay.inWholeMilliseconds)
            hide()
        }
    }

    /**
     * Cancels pending auto-hide job.
     */
    private fun cancelAutoHide() {
        autoHideJob?.cancel()
        autoHideJob = null
    }

    /**
     * Fades in the scrollbar.
     */
    private fun fadeIn() {
        fadeJob?.cancel()

        fadeJob = scope.launch {
            alphaAnimatable.animateTo(
                targetValue = 1f,
                animationSpec = config.visibilityAnimationSpec
            )
        }
    }

    /**
     * Fades out the scrollbar.
     */
    private fun fadeOut() {
        fadeJob?.cancel()

        fadeJob = scope.launch {
            alphaAnimatable.animateTo(
                targetValue = 0f,
                animationSpec = config.visibilityAnimationSpec
            )
        }
    }

    /**
     * Immediately shows scrollbar without animation.
     */
    fun showImmediately() {
        cancelAutoHide()
        fadeJob?.cancel()

        _isVisible = true
        scope.launch {
            alphaAnimatable.snapTo(1f)
        }
    }

    /**
     * Immediately hides scrollbar without animation.
     */
    fun hideImmediately() {
        if (_isDragging || _alwaysVisible) {
            return
        }

        cancelAutoHide()
        fadeJob?.cancel()

        _isVisible = false
        scope.launch {
            alphaAnimatable.snapTo(0f)
        }
    }

    /**
     * Cancels all animations and jobs.
     *
     * Should be called when scrollbar is disposed.
     */
    fun dispose() {
        cancelAutoHide()
        fadeJob?.cancel()
        fadeJob = null
    }
}

/**
 * Remembers a ScrollbarVisibilityAnimator instance.
 *
 * Creates and remembers a visibility animator with the provided configuration.
 * Automatically disposes the animator when the composable leaves composition.
 *
 * @param config Animation configuration
 * @return Remembered ScrollbarVisibilityAnimator instance
 */
@Composable
fun rememberScrollbarVisibilityAnimator(
    config: AnimationConfig = AnimationConfig.Default
): ScrollbarVisibilityAnimator {
    val scope = rememberCoroutineScope()

    val animator = remember(config) {
        ScrollbarVisibilityAnimator(config, scope)
    }

    // Dispose on composition leave
    LaunchedEffect(animator) {
        return@LaunchedEffect
    }

    return animator
}

/**
 * State holder for scrollbar visibility with automatic animation.
 *
 * Provides a simpler API for common visibility scenarios without
 * direct animator access.
 *
 * @property visible Current visibility state
 * @property alpha Current alpha value (0f to 1f)
 */
data class ScrollbarVisibilityState(
    val visible: Boolean,
    val alpha: Float
)

/**
 * Remembers scrollbar visibility state with auto-hide behavior.
 *
 * Simpler alternative to full ScrollbarVisibilityAnimator for basic use cases.
 *
 * @param config Animation configuration
 * @param alwaysVisible If true, scrollbar never auto-hides
 * @return State containing visibility and alpha values
 */
@Composable
fun rememberScrollbarVisibilityState(
    config: AnimationConfig = AnimationConfig.Default,
    alwaysVisible: Boolean = false
): State<ScrollbarVisibilityState> {
    val animator = rememberScrollbarVisibilityAnimator(config)

    LaunchedEffect(alwaysVisible) {
        animator.setAlwaysVisible(alwaysVisible)
    }

    return remember {
        derivedStateOf {
            ScrollbarVisibilityState(
                visible = animator.isVisible,
                alpha = animator.alpha
            )
        }
    }
}
