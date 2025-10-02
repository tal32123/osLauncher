package com.talauncher.ui.components.scrollbar.animation

/**
 * Interface defining scrollbar animation capabilities.
 *
 * This interface follows the Interface Segregation Principle (ISP) by providing
 * a focused contract for scroll animation operations. It enables dependency injection
 * and facilitates testing through mock implementations.
 *
 * **Design Pattern**: Strategy Pattern - Allows different animation strategies
 * **SOLID Principle**: Interface Segregation - Focused, single-purpose interface
 */
interface IScrollAnimator {
    /**
     * Animates the scroll position to the specified target item.
     *
     * This is a suspending function that completes when the animation finishes
     * or is cancelled. The animation duration is dynamically calculated based
     * on the scroll distance.
     *
     * @param targetIndex The target item index to scroll to
     * @param currentIndex The current scroll position index
     * @param onProgress Optional callback invoked with progress values (0f to 1f)
     *                   during the animation. Useful for updating UI state.
     *
     * @throws kotlinx.coroutines.CancellationException if the animation is cancelled
     */
    suspend fun animateToItem(
        targetIndex: Int,
        currentIndex: Int,
        onProgress: ((Float) -> Unit)? = null
    )

    /**
     * Animates the scrollbar visibility (fade in/out).
     *
     * This is a suspending function that animates the alpha value from the
     * current state to the target visibility state.
     *
     * @param visible Target visibility state (true = fade in, false = fade out)
     * @param onProgress Optional callback invoked with alpha values (0f to 1f)
     *
     * @throws kotlinx.coroutines.CancellationException if the animation is cancelled
     */
    suspend fun animateVisibility(
        visible: Boolean,
        onProgress: ((Float) -> Unit)? = null
    )

    /**
     * Cancels all ongoing animations immediately.
     *
     * This method should be called when the scrollbar is being disposed
     * or when animations need to be interrupted (e.g., user interaction).
     * Safe to call multiple times and when no animations are running.
     */
    fun cancelAnimations()

    /**
     * Checks if any animation is currently running.
     *
     * @return true if scroll or visibility animations are in progress
     */
    fun isAnimating(): Boolean
}
