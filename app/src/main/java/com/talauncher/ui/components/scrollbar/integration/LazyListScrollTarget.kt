package com.talauncher.ui.components.scrollbar.integration

import androidx.compose.foundation.lazy.LazyListState

/**
 * Adapter implementation that wraps [LazyListState] to conform to [IScrollableTarget] interface.
 *
 * This class follows the **Adapter Pattern** by converting the LazyListState API into the
 * standardized IScrollableTarget interface, enabling the scrollbar to work with LazyColumn
 * and LazyRow components without direct coupling.
 *
 * ## Design Patterns Applied
 * - **Adapter Pattern**: Adapts LazyListState to IScrollableTarget interface
 * - **Dependency Injection**: Accepts LazyListState and item count provider as constructor dependencies
 * - **Strategy Pattern**: Item count calculation is delegated to injectable lambda
 *
 * ## SOLID Principles
 * - **Single Responsibility**: Only adapts LazyListState to IScrollableTarget, no business logic
 * - **Open/Closed**: Closed for modification, but extensible through composition
 * - **Dependency Inversion**: Depends on abstractions (IScrollableTarget, lambda) not implementations
 * - **Liskov Substitution**: Can be used anywhere IScrollableTarget is expected
 *
 * ## Thread Safety
 * This class is thread-safe as long as the provided [LazyListState] is accessed from the
 * UI thread (Compose main thread). The [itemCountProvider] should also be thread-safe or
 * accessed only from the UI thread.
 *
 * ## Memory Management
 * - Lightweight: Only holds references to existing objects (no heavy allocations)
 * - No lifecycle management needed: LazyListState lifecycle is managed by Compose
 * - Lambda capture: Be careful with [itemCountProvider] captures to avoid memory leaks
 *
 * ## Usage Examples
 *
 * ### Basic Usage with Static List
 * ```kotlin
 * val lazyListState = rememberLazyListState()
 * val apps = remember { loadApps() }
 *
 * val scrollTarget = remember(lazyListState, apps) {
 *     LazyListScrollTarget(lazyListState) { apps.size }
 * }
 * ```
 *
 * ### Dynamic List with Filtering
 * ```kotlin
 * val lazyListState = rememberLazyListState()
 * var searchQuery by remember { mutableStateOf("") }
 * val filteredApps = remember(searchQuery) {
 *     apps.filter { it.appName.contains(searchQuery, ignoreCase = true) }
 * }
 *
 * val scrollTarget = remember(lazyListState) {
 *     LazyListScrollTarget(lazyListState) { filteredApps.size }
 * }
 * ```
 *
 * ### With Recent Apps Header
 * ```kotlin
 * val scrollTarget = remember(lazyListState) {
 *     LazyListScrollTarget(lazyListState) {
 *         // Account for section headers
 *         val headerCount = if (recentApps.isNotEmpty()) 1 else 0
 *         recentApps.size + headerCount + allApps.size
 *     }
 * }
 * ```
 *
 * @property lazyListState The LazyListState from LazyColumn/LazyRow to be adapted
 * @property itemCountProvider Lambda that returns the current total item count.
 *                            Called on-demand, so it can return dynamic values based on
 *                            current state (filtering, search, sections, etc.)
 *
 * @see IScrollableTarget for interface documentation
 * @see LazyListState for the underlying Compose state object
 */
class LazyListScrollTarget(
    private val lazyListState: LazyListState,
    private val itemCountProvider: () -> Int
) : IScrollableTarget {

    /**
     * Gets the current scroll position from LazyListState.
     *
     * This method directly delegates to [LazyListState.firstVisibleItemIndex] and
     * [LazyListState.firstVisibleItemScrollOffset], which are Compose State objects
     * that trigger recomposition when changed.
     *
     * ## Performance Characteristics
     * - O(1) complexity: Direct property access
     * - Recomposition trigger: Reading these values subscribes to scroll state changes
     * - Thread safety: Must be called from Compose UI thread
     *
     * ## Return Value Semantics
     * - Index: Zero-based position of first visible item
     * - Offset: Negative pixels if item is scrolled up, 0 if aligned at top
     *
     * @return Immutable snapshot of current scroll position
     */
    override fun getCurrentPosition(): ScrollPosition {
        return ScrollPosition(
            firstVisibleItemIndex = lazyListState.firstVisibleItemIndex,
            firstVisibleItemOffset = lazyListState.firstVisibleItemScrollOffset
        )
    }

    /**
     * Instantly scrolls to the specified item without animation.
     *
     * Delegates to [LazyListState.scrollToItem] which is a suspending function.
     * The operation completes when the scroll has finished.
     *
     * ## Behavior Details
     * - **Bounds checking**: LazyListState handles out-of-bounds indices gracefully
     * - **Interruption**: Can be cancelled via coroutine cancellation
     * - **Composition**: Triggers recomposition when scroll position changes
     *
     * ## Performance Characteristics
     * - O(1) for index jump: No layout calculation needed for scrollToItem
     * - Immediate visual update: No frame delays
     * - Suspends until scroll completes: Wait for layout to settle
     *
     * ## Use Cases
     * - Scrollbar thumb dragging (rapid, continuous updates)
     * - Programmatic navigation to specific items
     * - Restoring saved scroll positions
     *
     * @param index Zero-based target item index (will be coerced to valid range by LazyListState)
     * @param scrollOffset Pixel offset within item (positive scrolls down within item)
     *
     * @throws kotlinx.coroutines.CancellationException if the coroutine is cancelled
     */
    override suspend fun scrollToItem(index: Int, scrollOffset: Int) {
        lazyListState.scrollToItem(index, scrollOffset)
    }

    /**
     * Animates scrolling to the specified item with smooth animation.
     *
     * Delegates to [LazyListState.animateScrollToItem] which provides platform-default
     * animation with deceleration easing. The animation duration is dynamically calculated
     * based on scroll distance.
     *
     * ## Animation Characteristics
     * - **Easing curve**: DecelerateInterpolator-like (fast start, slow end)
     * - **Duration**: ~300-500ms for typical distances
     * - **Frame rate**: Targets 60 fps for smooth visual experience
     * - **Interruption**: Can be interrupted by user gestures or new scroll commands
     *
     * ## Behavior Details
     * - **Bounds checking**: Handles out-of-bounds indices gracefully
     * - **Accessibility**: Respects user's reduced motion preferences (may skip animation)
     * - **Multiple calls**: New animations interrupt previous ones
     *
     * ## Use Cases
     * - Scrollbar tap-to-jump (user expects smooth transition)
     * - Programmatic scrolling with visual feedback
     * - Search result navigation with smooth scrolling
     *
     * @param index Zero-based target item index (coerced to valid range)
     * @param scrollOffset Pixel offset within item (positive scrolls down within item)
     *
     * @throws kotlinx.coroutines.CancellationException if the animation is cancelled
     */
    override suspend fun animateScrollToItem(index: Int, scrollOffset: Int) {
        lazyListState.animateScrollToItem(index, scrollOffset)
    }

    /**
     * Gets the total number of items from the injected provider lambda.
     *
     * This delegates to [itemCountProvider] which allows for dynamic item counting
     * that can change based on filtering, search, or content updates.
     *
     * ## Design Rationale
     * We use a lambda provider instead of directly accessing LazyListState.layoutInfo.totalItemsCount
     * because:
     * 1. **Dynamic content**: Item count may change due to filtering, search, etc.
     * 2. **Section headers**: May need to account for headers/footers in count
     * 3. **Flexibility**: Allows custom counting logic for complex list structures
     * 4. **Reactivity**: Provider can capture changing state variables
     *
     * ## Performance Considerations
     * - Called frequently during scrolling and gesture tracking
     * - Provider should be lightweight (O(1) complexity preferred)
     * - Avoid expensive computations in the lambda
     * - Consider memoizing results if calculation is expensive
     *
     * ## Example Providers
     * ```kotlin
     * // Simple static list
     * { myList.size }
     *
     * // Filtered list
     * { filteredApps.size }
     *
     * // With sections
     * { recentApps.size + (if (recentApps.isNotEmpty()) 1 else 0) + allApps.size }
     * ```
     *
     * @return Current total item count from provider, or 0 if provider returns negative
     */
    override fun getTotalItems(): Int {
        return itemCountProvider().coerceAtLeast(0)
    }

    /**
     * Extracts visible items information from LazyListState.layoutInfo.
     *
     * This method reads the layout information that Compose calculates during composition
     * and layout phases. The information is accurate and reflects the current visible state.
     *
     * ## Data Source: LazyListLayoutInfo
     * - `totalItemsCount`: Total items known to LazyList (may differ from itemCountProvider)
     * - `visibleItemsInfo`: List of currently visible items with positions
     * - `viewportSize`: Dimensions of the scrollable viewport
     * - `beforeContentPadding`: Padding before first item
     * - `afterContentPadding`: Padding after last item
     *
     * ## Visible Item Count Calculation
     * We count the actual visible items from layoutInfo instead of estimating,
     * providing accurate information for scrollbar calculations.
     *
     * ## Edge Cases Handled
     * - **Empty list**: Returns safe defaults (all zeros)
     * - **Single item**: Sets first and last indices to same value
     * - **Partially visible items**: Counted as visible if any portion is in viewport
     * - **Content padding**: Not included in visible item count
     *
     * ## Performance Characteristics
     * - O(1) for layoutInfo access: Compose maintains this data
     * - O(n) for visible item iteration: n is typically small (10-20 items)
     * - Recomposition trigger: Reading layoutInfo subscribes to layout changes
     *
     * ## Thread Safety
     * Must be called from Compose UI thread as it accesses Compose State objects.
     *
     * @return Immutable snapshot of visible items information
     */
    override fun getVisibleItemsInfo(): VisibleItemsInfo {
        val layoutInfo = lazyListState.layoutInfo
        val visibleItems = layoutInfo.visibleItemsInfo

        // Calculate actual visible item count from layout info
        val visibleCount = visibleItems.size

        // Get first and last visible indices with safety checks
        val firstIndex = visibleItems.firstOrNull()?.index ?: 0
        val lastIndex = visibleItems.lastOrNull()?.index ?: 0
        val firstOffset = lazyListState.firstVisibleItemScrollOffset

        // Use item count provider for total count (supports dynamic lists)
        val totalCount = getTotalItems()

        // Extract viewport height from layout info
        val viewportHeight = layoutInfo.viewportSize.height.toFloat()

        return VisibleItemsInfo(
            totalItemCount = totalCount,
            visibleItemCount = visibleCount,
            firstVisibleItemIndex = firstIndex,
            firstVisibleItemOffset = firstOffset,
            lastVisibleItemIndex = lastIndex,
            viewportHeight = viewportHeight
        )
    }
}

/**
 * Extension function to create a LazyListScrollTarget from LazyListState.
 *
 * This provides a convenient factory method that can be used in Compose code
 * with `remember` to create the adapter.
 *
 * ## Usage Example
 * ```kotlin
 * val lazyListState = rememberLazyListState()
 * val scrollTarget = remember(lazyListState) {
 *     lazyListState.asScrollTarget { filteredApps.size }
 * }
 * ```
 *
 * @param itemCountProvider Lambda that returns the current total item count
 * @return LazyListScrollTarget adapter wrapping this LazyListState
 */
fun LazyListState.asScrollTarget(itemCountProvider: () -> Int): LazyListScrollTarget {
    return LazyListScrollTarget(this, itemCountProvider)
}
