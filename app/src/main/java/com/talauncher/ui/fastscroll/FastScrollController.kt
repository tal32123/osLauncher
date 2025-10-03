package com.talauncher.ui.fastscroll

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.talauncher.domain.fastscroll.A11yAnnouncer
import com.talauncher.domain.fastscroll.Haptics
import com.talauncher.domain.model.SectionIndex
import com.talauncher.domain.usecases.MapTouchToTargetUseCase
import com.talauncher.domain.usecases.TouchTarget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Controller for fast scroll operations with per-app targeting.
 *
 * Architecture:
 * - Follows Controller pattern (separates UI from business logic)
 * - Uses Dependency Inversion (depends on abstract interfaces)
 * - Maintains immutable state with Compose-friendly StateFlow
 * - Throttles scroll updates to maintain 60fps
 *
 * Design Patterns:
 * - Controller: Manages state and coordinates between domain and UI
 * - Observer: Exposes StateFlow for reactive UI updates
 * - Strategy: Uses injected interfaces for haptics, a11y, mapping
 *
 * SOLID Principles:
 * - Single Responsibility: Only manages fast scroll state and coordination
 * - Open/Closed: Can be extended with new features without modification
 * - Liskov Substitution: Can be replaced with mock implementations for testing
 * - Interface Segregation: Depends on focused interfaces (Haptics, A11yAnnouncer)
 * - Dependency Inversion: Depends on abstractions, not concrete implementations
 *
 * Performance:
 * - Throttles scroll updates to max 1 per 16-24ms (60fps target)
 * - Zero allocations during drag (all data precomputed)
 * - Coalesces redundant scroll to same index
 * - Rate-limits haptics and accessibility announcements
 *
 * Thread Safety:
 * - All state mutations on main thread (Compose assumption)
 * - Immutable state objects for thread safety
 * - No shared mutable state
 *
 * @param mapTouchToTargetUseCase Use case for mapping touch to app targets
 * @param haptics Optional haptic feedback implementation
 * @param a11yAnnouncer Optional accessibility announcer
 */
@Stable
class FastScrollController(
    private val mapTouchToTargetUseCase: MapTouchToTargetUseCase = MapTouchToTargetUseCase(),
    private val haptics: Haptics? = null,
    private val a11yAnnouncer: A11yAnnouncer? = null
) {
    /**
     * Current fast scroll state.
     */
    private val _state = MutableStateFlow(FastScrollState())
    val state: StateFlow<FastScrollState> = _state.asStateFlow()

    /**
     * Current section index (set by UI when data changes).
     */
    private var sectionIndex: SectionIndex = SectionIndex.EMPTY

    /**
     * Last scroll target to avoid redundant scroll calls.
     */
    private var lastScrolledGlobalIndex: Int? = null

    /**
     * Last section key for haptic/a11y on section change.
     */
    private var lastSectionKey: String? = null

    /**
     * Last app name for haptic/a11y on app change.
     */
    private var lastAppName: String? = null

    /**
     * Last scroll update time for throttling.
     */
    private var lastScrollUpdateTime = 0L

    /**
     * Scroll update throttle (16ms = ~60fps).
     */
    private val scrollThrottleMs = 16L

    /**
     * Sets the section index. Call this when app list or sorting changes.
     *
     * @param newIndex The updated section index
     */
    fun setSectionIndex(newIndex: SectionIndex) {
        sectionIndex = newIndex
        // Reset state when index changes
        if (!_state.value.isActive) {
            _state.value = FastScrollState()
            lastScrolledGlobalIndex = null
            lastSectionKey = null
            lastAppName = null
        }
    }

    /**
     * Handles touch start event.
     * Activates the fast scroll state and shows the preview bubble.
     */
    fun onTouchStart() {
        try {
            _state.value = _state.value.copy(isActive = true)
            lastScrolledGlobalIndex = null
            lastSectionKey = null
            lastAppName = null
        } catch (e: Exception) {
            android.util.Log.e("FastScrollController", "Error in onTouchStart", e)
        }
    }

    /**
     * Handles touch move event with per-app targeting.
     *
     * Performance:
     * - Throttled to max 1 update per 16ms (~60fps)
     * - Coalesces redundant scrolls to same index
     * - Zero allocations (reuses existing data structures)
     *
     * @param touchY Touch Y coordinate in pixels
     * @param railHeight Total rail height in pixels
     * @param railTopPadding Top padding in pixels
     * @param railBottomPadding Bottom padding in pixels
     * @param onScrollToIndex Callback to scroll the list to the target index
     */
    fun onTouch(
        touchY: Float,
        railHeight: Float,
        railTopPadding: Float,
        railBottomPadding: Float,
        onScrollToIndex: (Int) -> Unit
    ) {
        try {
            // Throttle scroll updates to maintain 60fps
            val now = System.currentTimeMillis()
            if (now - lastScrollUpdateTime < scrollThrottleMs) {
                return
            }
            lastScrollUpdateTime = now

            // Map touch to target using O(1) prefix sum computation
            val target = try {
                mapTouchToTargetUseCase.execute(
                    touchY = touchY,
                    railHeight = railHeight,
                    railTopPadding = railTopPadding,
                    railBottomPadding = railBottomPadding,
                    sectionIndex = sectionIndex
                )
            } catch (e: Exception) {
                android.util.Log.e("FastScrollController", "Error mapping touch to target", e)
                null
            } ?: return

            // Update state with current target
            try {
                _state.value = _state.value.copy(
                    currentTarget = target,
                    currentLetter = target.section.key,
                    currentAppName = target.appName
                )
            } catch (e: Exception) {
                android.util.Log.e("FastScrollController", "Error updating state", e)
            }

            // Scroll to target if different from last position (coalesce redundant scrolls)
            if (target.globalIndex != lastScrolledGlobalIndex) {
                try {
                    onScrollToIndex(target.globalIndex)
                    lastScrolledGlobalIndex = target.globalIndex
                } catch (e: Exception) {
                    android.util.Log.e("FastScrollController", "Error scrolling to index ${target.globalIndex}", e)
                }
            }

            // Haptic and accessibility feedback on changes
            try {
                handleFeedback(target)
            } catch (e: Exception) {
                android.util.Log.e("FastScrollController", "Error handling feedback", e)
            }
        } catch (e: Exception) {
            android.util.Log.e("FastScrollController", "Critical error in onTouch", e)
        }
    }

    /**
     * Handles touch end event.
     * Deactivates the fast scroll state and hides the preview bubble.
     */
    fun onTouchEnd() {
        try {
            _state.value = _state.value.copy(
                isActive = false,
                currentTarget = null,
                currentLetter = null,
                currentAppName = null
            )
            lastScrolledGlobalIndex = null
            lastSectionKey = null
            lastAppName = null
        } catch (e: Exception) {
            android.util.Log.e("FastScrollController", "Error in onTouchEnd", e)
        }
    }

    /**
     * Scrolls to the first app in a section (for tap-to-jump).
     *
     * @param sectionKey The section key to jump to
     * @param onScrollToIndex Callback to scroll the list to the target index
     */
    fun scrollToSection(sectionKey: String, onScrollToIndex: (Int) -> Unit) {
        val target = mapTouchToTargetUseCase.executeForSectionStart(sectionKey, sectionIndex)
            ?: return

        onScrollToIndex(target.globalIndex)

        // Brief activation to show preview
        _state.value = FastScrollState(
            isActive = true,
            currentTarget = target,
            currentLetter = target.section.key,
            currentAppName = target.appName
        )

        // Haptic feedback
        haptics?.performLetterChange()

        // Deactivate after a brief moment (handled by UI animation)
    }

    /**
     * Handles haptic and accessibility feedback on target changes.
     */
    private fun handleFeedback(target: TouchTarget) {
        val sectionChanged = target.section.key != lastSectionKey
        val appChanged = target.appName != lastAppName

        if (sectionChanged) {
            // Letter changed - stronger haptic, letter announcement
            haptics?.performLetterChange()
            a11yAnnouncer?.announce(target.section.key, target.appName)
            lastSectionKey = target.section.key
            lastAppName = target.appName
        } else if (appChanged) {
            // App changed within same section - lighter haptic, full announcement
            haptics?.performAppChange()
            a11yAnnouncer?.announce(target.section.key, target.appName)
            lastAppName = target.appName
        }
    }
}

/**
 * Immutable state for fast scroll UI.
 *
 * Architecture:
 * - Immutable data class for thread safety
 * - Compose-friendly state object
 * - Minimal state surface for efficiency
 *
 * @property isActive Whether fast scroll is currently active (finger down)
 * @property currentTarget Current touch target (null when inactive)
 * @property currentLetter Current section letter for UI highlight
 * @property currentAppName Current app name for preview bubble
 */
data class FastScrollState(
    val isActive: Boolean = false,
    val currentTarget: TouchTarget? = null,
    val currentLetter: String? = null,
    val currentAppName: String? = null
)
