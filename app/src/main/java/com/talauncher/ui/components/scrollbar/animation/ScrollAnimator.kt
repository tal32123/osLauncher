package com.talauncher.ui.components.scrollbar.animation

import androidx.compose.animation.core.Animatable
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Implementation of scroll animation functionality for scrollbars.
 *
 * This class manages scroll position and visibility animations using Jetpack Compose's
 * Animatable API. It ensures thread-safe animation state management and proper
 * coroutine lifecycle handling.
 *
 * **Design Pattern**: Strategy Pattern - Implements IScrollAnimator strategy
 * **SOLID Principles**:
 * - Single Responsibility: Manages only scroll animations
 * - Dependency Inversion: Depends on AnimationConfig abstraction
 * - Open/Closed: Extensible through configuration without modification
 *
 * @property config Animation configuration containing durations, easing functions, etc.
 */
class ScrollAnimator(
    private val config: AnimationConfig = AnimationConfig.Default
) : IScrollAnimator {

    /**
     * Animatable for scroll position changes.
     * Using Float to support smooth interpolation between positions.
     */
    private val scrollAnimatable = Animatable(0f)

    /**
     * Animatable for visibility (alpha) changes.
     * Range: 0f (invisible) to 1f (fully visible)
     */
    private val visibilityAnimatable = Animatable(1f)

    /**
     * Job tracking the current scroll animation.
     * Nullable to indicate no active animation.
     */
    private var scrollJob: Job? = null

    /**
     * Job tracking the current visibility animation.
     * Nullable to indicate no active animation.
     */
    private var visibilityJob: Job? = null

    /**
     * Thread-safe lock for animation state mutations.
     * Prevents race conditions when cancelling and starting animations.
     */
    private val animationLock = Any()

    override suspend fun animateToItem(
        targetIndex: Int,
        currentIndex: Int,
        onProgress: ((Float) -> Unit)?
    ) {
        // Cancel any existing scroll animation
        synchronized(animationLock) {
            scrollJob?.cancel()
            scrollJob = null
        }

        val distance = abs(targetIndex - currentIndex)

        // Skip animation for same position or invalid indices
        if (distance == 0 || targetIndex < 0 || currentIndex < 0) {
            onProgress?.invoke(1f)
            return
        }

        // Create coroutine scope for this animation
        coroutineScope {
            val job = launch {
                try {
                    // Snap to current position
                    scrollAnimatable.snapTo(currentIndex.toFloat())

                    // Animate to target with dynamic duration
                    val animationSpec = config.createScrollSpec(distance)

                    scrollAnimatable.animateTo(
                        targetValue = targetIndex.toFloat(),
                        animationSpec = animationSpec
                    ) {
                        // Calculate progress: 0f at start, 1f at end
                        val progress = if (distance > 0) {
                            abs(value - currentIndex) / distance.toFloat()
                        } else {
                            1f
                        }
                        onProgress?.invoke(progress.coerceIn(0f, 1f))
                    }

                    // Ensure final callback at completion
                    onProgress?.invoke(1f)
                } finally {
                    synchronized(animationLock) {
                        if (scrollJob == coroutineContext[Job]) {
                            scrollJob = null
                        }
                    }
                }
            }

            synchronized(animationLock) {
                scrollJob = job
            }

            // Wait for animation completion
            job.join()
        }
    }

    override suspend fun animateVisibility(
        visible: Boolean,
        onProgress: ((Float) -> Unit)?
    ) {
        // Cancel any existing visibility animation
        synchronized(animationLock) {
            visibilityJob?.cancel()
            visibilityJob = null
        }

        val targetAlpha = if (visible) 1f else 0f

        // Skip if already at target
        if (visibilityAnimatable.value == targetAlpha) {
            onProgress?.invoke(targetAlpha)
            return
        }

        coroutineScope {
            val job = launch {
                try {
                    visibilityAnimatable.animateTo(
                        targetValue = targetAlpha,
                        animationSpec = config.visibilityAnimationSpec
                    ) {
                        onProgress?.invoke(value)
                    }

                    // Ensure final callback
                    onProgress?.invoke(targetAlpha)
                } finally {
                    synchronized(animationLock) {
                        if (visibilityJob == coroutineContext[Job]) {
                            visibilityJob = null
                        }
                    }
                }
            }

            synchronized(animationLock) {
                visibilityJob = job
            }

            // Wait for animation completion
            job.join()
        }
    }

    override fun cancelAnimations() {
        synchronized(animationLock) {
            scrollJob?.cancel()
            scrollJob = null
            visibilityJob?.cancel()
            visibilityJob = null
        }
    }

    override fun isAnimating(): Boolean {
        synchronized(animationLock) {
            return scrollJob?.isActive == true || visibilityJob?.isActive == true
        }
    }

    /**
     * Gets the current scroll animation value.
     * Useful for reading intermediate animation states.
     *
     * @return Current scroll position as Float
     */
    fun getCurrentScrollValue(): Float = scrollAnimatable.value

    /**
     * Gets the current visibility (alpha) value.
     *
     * @return Current alpha value (0f to 1f)
     */
    fun getCurrentVisibilityValue(): Float = visibilityAnimatable.value

    /**
     * Immediately snaps scroll position to target without animation.
     * Useful for initialization or user-triggered instant scrolls.
     *
     * @param position Target position to snap to
     */
    suspend fun snapToPosition(position: Int) {
        synchronized(animationLock) {
            scrollJob?.cancel()
            scrollJob = null
        }
        scrollAnimatable.snapTo(position.toFloat())
    }

    /**
     * Immediately snaps visibility to target state without animation.
     *
     * @param visible Target visibility state
     */
    suspend fun snapToVisibility(visible: Boolean) {
        synchronized(animationLock) {
            visibilityJob?.cancel()
            visibilityJob = null
        }
        visibilityAnimatable.snapTo(if (visible) 1f else 0f)
    }
}
