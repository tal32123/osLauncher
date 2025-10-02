package com.talauncher.ui.components.scrollbar.integration

/**
 * Abstraction interface for scrollable UI components.
 *
 * This interface follows the **Adapter Pattern** and **Dependency Inversion Principle (DIP)**
 * by providing a common abstraction layer between the scrollbar implementation and various
 * scrollable components (LazyColumn, LazyRow, custom scrollable views, etc.).
 *
 * ## Design Patterns Applied
 * - **Adapter Pattern**: Allows different scrollable implementations to be adapted to a common interface
 * - **Strategy Pattern**: Different scroll behaviors can be plugged in through this interface
 * - **Interface Segregation**: Focused contract for scroll operations only
 *
 * ## SOLID Principles
 * - **Single Responsibility**: Only handles scroll position queries and scroll operations
 * - **Open/Closed**: Open for extension (new scrollable types), closed for modification
 * - **Dependency Inversion**: High-level scrollbar depends on this abstraction, not concrete implementations
 *
 * ## Thread Safety
 * Implementations must be thread-safe as they may be called from different coroutine contexts.
 * Use appropriate synchronization mechanisms (Mutex, StateFlow, etc.).
 *
 * ## Usage Example
 * ```kotlin
 * val lazyListState = rememberLazyListState()
 * val scrollTarget = remember(lazyListState) {
 *     LazyListScrollTarget(lazyListState) { filteredApps.size }
 * }
 *
 * ScrollbarWithTarget(
 *     scrollTarget = scrollTarget,
 *     modifier = Modifier.fillMaxHeight()
 * )
 * ```
 *
 * @see LazyListScrollTarget for LazyColumn/LazyRow adapter implementation
 * @see ScrollSynchronizer for bidirectional synchronization logic
 */
interface IScrollableTarget {

    /**
     * Gets the current scroll position information.
     *
     * This method should be lightweight and non-blocking as it may be called
     * frequently during scroll events and gesture tracking.
     *
     * @return Current scroll position containing item index and pixel offset
     */
    fun getCurrentPosition(): ScrollPosition

    /**
     * Instantly scrolls to the specified item without animation.
     *
     * This is a suspending function that completes when the scroll operation
     * finishes. Use this for programmatic scrolling when animation is not desired
     * (e.g., scrollbar dragging, jump-to-index).
     *
     * ## Implementation Notes
     * - Must handle out-of-bounds indices gracefully (coerce to valid range)
     * - Should not trigger unnecessary recomposition
     * - Must be cancellable via coroutine cancellation
     *
     * @param index Zero-based target item index to scroll to
     * @param scrollOffset Optional pixel offset within the target item (default: 0)
     *
     * @throws kotlinx.coroutines.CancellationException if cancelled
     */
    suspend fun scrollToItem(index: Int, scrollOffset: Int = 0)

    /**
     * Animates scrolling to the specified item with smooth animation.
     *
     * This is a suspending function that completes when the animation finishes
     * or is cancelled. The animation uses platform-default easing curves for
     * natural scroll behavior.
     *
     * ## Animation Behavior
     * - Short distances: Fast animation (150-250ms)
     * - Long distances: Longer animation with deceleration (300-500ms)
     * - Very long distances: May use multi-phase animation to maintain smoothness
     *
     * ## Implementation Notes
     * - Must handle out-of-bounds indices gracefully
     * - Should interrupt ongoing animations when called multiple times
     * - Must respect user's reduced motion preferences (accessibility)
     *
     * @param index Zero-based target item index to scroll to
     * @param scrollOffset Optional pixel offset within the target item (default: 0)
     *
     * @throws kotlinx.coroutines.CancellationException if cancelled
     */
    suspend fun animateScrollToItem(index: Int, scrollOffset: Int = 0)

    /**
     * Gets the total number of items in the scrollable content.
     *
     * This value may change dynamically as items are added/removed from the list.
     * Implementations should return the current count efficiently without triggering
     * expensive computations.
     *
     * ## Use Cases
     * - Scrollbar thumb size calculation
     * - Scroll position validation
     * - Touch position to item index mapping
     *
     * @return Total item count, or 0 if list is empty
     */
    fun getTotalItems(): Int

    /**
     * Gets information about currently visible items in the viewport.
     *
     * This information is used for:
     * - Calculating scrollbar thumb position
     * - Determining scroll progress percentage
     * - Optimizing rendering and caching strategies
     *
     * ## Performance Considerations
     * This method may be called frequently during scrolling. Implementations
     * should cache results when possible and avoid expensive computations.
     *
     * @return Information about visible items including indices and offsets
     */
    fun getVisibleItemsInfo(): VisibleItemsInfo
}

/**
 * Data class representing the current scroll position.
 *
 * Provides a snapshot of the scroll state at a point in time. This immutable
 * data structure is thread-safe and can be safely passed between coroutines.
 *
 * ## Calculation Examples
 * ```kotlin
 * // Scrolled to third item with 100px offset
 * ScrollPosition(firstVisibleItemIndex = 2, firstVisibleItemOffset = -100)
 *
 * // At the very top of the list
 * ScrollPosition(firstVisibleItemIndex = 0, firstVisibleItemOffset = 0)
 *
 * // Partially scrolled past first item
 * ScrollPosition(firstVisibleItemIndex = 0, firstVisibleItemOffset = -50)
 * ```
 *
 * @property firstVisibleItemIndex Zero-based index of the first visible item
 * @property firstVisibleItemOffset Pixel offset of the first visible item.
 *                                   Typically negative when item is partially scrolled off-screen,
 *                                   or 0 when item's top edge is aligned with viewport top.
 */
data class ScrollPosition(
    val firstVisibleItemIndex: Int,
    val firstVisibleItemOffset: Int
)

/**
 * Data class containing information about visible items in the viewport.
 *
 * Used by scrollbar calculations to determine thumb position and size.
 * This immutable snapshot can be safely shared across threads.
 *
 * ## Calculation Logic
 * ```kotlin
 * val visibleRatio = visibleItemCount.toFloat() / totalItemCount
 * val thumbHeight = visibleRatio * scrollbarHeight
 *
 * val scrollProgress = firstVisibleItemIndex.toFloat() / totalItemCount
 * val thumbPosition = scrollProgress * (scrollbarHeight - thumbHeight)
 * ```
 *
 * ## Edge Cases
 * - Empty list: `totalItemCount = 0, visibleItemCount = 0`
 * - All items visible: `visibleItemCount == totalItemCount`
 * - Large list: `visibleItemCount << totalItemCount` (small thumb)
 *
 * @property totalItemCount Total number of items in the scrollable content
 * @property visibleItemCount Approximate number of items currently visible in viewport
 * @property firstVisibleItemIndex Zero-based index of first visible item
 * @property firstVisibleItemOffset Pixel offset of first visible item (typically negative or 0)
 * @property lastVisibleItemIndex Zero-based index of last visible item
 * @property viewportHeight Total height of the scrollable viewport in pixels
 */
data class VisibleItemsInfo(
    val totalItemCount: Int,
    val visibleItemCount: Int,
    val firstVisibleItemIndex: Int,
    val firstVisibleItemOffset: Int,
    val lastVisibleItemIndex: Int,
    val viewportHeight: Float
) {
    /**
     * Calculates the scroll progress as a percentage.
     *
     * @return Progress value from 0.0 (top) to 1.0 (bottom), or 0.0 if list is empty
     */
    fun calculateScrollProgress(): Float {
        if (totalItemCount == 0) return 0f
        val maxScroll = (totalItemCount - visibleItemCount).coerceAtLeast(0)
        if (maxScroll == 0) return 0f
        return (firstVisibleItemIndex.toFloat() / maxScroll).coerceIn(0f, 1f)
    }

    /**
     * Checks if all content is currently visible (no scrolling needed).
     *
     * @return true if the entire list fits in the viewport
     */
    fun isFullyVisible(): Boolean = visibleItemCount >= totalItemCount

    /**
     * Checks if scrolled to the top of the list.
     *
     * @return true if viewing the first item at offset 0
     */
    fun isAtTop(): Boolean = firstVisibleItemIndex == 0 && firstVisibleItemOffset == 0

    /**
     * Checks if scrolled to the bottom of the list.
     *
     * @return true if viewing the last item
     */
    fun isAtBottom(): Boolean = lastVisibleItemIndex >= totalItemCount - 1
}
