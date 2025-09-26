package com.talauncher.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.pager.PagerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Animation extensions following SOLID principles.
 * Single Responsibility: Each function handles one animation type.
 * Open/Closed: Can be extended with new animation types without modification.
 * Dependency Inversion: Depends on abstractions (UiSettings) not concrete implementations.
 */

/**
 * Extension function to animate pager scroll with respect to animation settings.
 * Uses spring animation when animations enabled, instant scroll when disabled.
 */
suspend fun PagerState.animateToPageRespectingSettings(
    page: Int,
    enableAnimations: Boolean = true
) {
    if (enableAnimations) {
        animateScrollToPage(page)
    } else {
        scrollToPage(page)
    }
}

/**
 * Coroutine-aware extension for pager navigation with animation control.
 * Follows the Command Pattern for encapsulating animation behavior.
 */
fun CoroutineScope.animatePagerToPage(
    pagerState: PagerState,
    page: Int,
    enableAnimations: Boolean = true
) {
    launch {
        pagerState.animateToPageRespectingSettings(page, enableAnimations)
    }
}

/**
 * Get appropriate animation spec based on settings.
 * Returns null for instant/no animation when disabled.
 */
fun getAnimationSpec(enableAnimations: Boolean): AnimationSpec<Float>? {
    return if (enableAnimations) {
        spring()
    } else {
        null
    }
}

/**
 * Get duration-based animation spec for components that need it.
 */
fun getAnimationDuration(enableAnimations: Boolean): Int {
    return if (enableAnimations) {
        300 // Default animation duration in ms
    } else {
        0   // Instant transition
    }
}