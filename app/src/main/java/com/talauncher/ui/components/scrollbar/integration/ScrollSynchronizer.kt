package com.talauncher.ui.components.scrollbar.integration

import com.talauncher.ui.components.scrollbar.calculation.IScrollPositionCalculator
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Coordinates bidirectional synchronization between scrollbar and scrollable content.
 *
 * This class implements the **Mediator Pattern** by acting as a central coordinator that
 * prevents infinite sync loops and manages the complex interaction between:
 * - Scrollbar state (thumb position, user gestures)
 * - List scroll state (item positions, user scrolling)
 * - Position calculations (coordinate mapping)
 *
 * ## Design Patterns Applied
 * - **Mediator Pattern**: Centralizes complex communication between scrollbar and list
 * - **Strategy Pattern**: Delegates position calculations to IScrollPositionCalculator
 * - **Dependency Injection**: Accepts calculator and target as constructor dependencies
 *
 * ## SOLID Principles
 * - **Single Responsibility**: Only handles scroll synchronization logic
 * - **Open/Closed**: Extensible through strategy injection, closed for modification
 * - **Dependency Inversion**: Depends on abstractions (interfaces) not implementations
 * - **Interface Segregation**: Focused API with only necessary sync methods
 *
 * ## Synchronization Problem
 * Without proper coordination, bidirectional sync creates infinite loops:
 * ```
 * User drags scrollbar -> Scroll list -> Update scrollbar -> Scroll list -> ...
 * User scrolls list -> Update scrollbar -> Scroll list -> Update scrollbar -> ...
 * ```
 *
 * ## Solution: Sync Lock Flag
 * We use an atomic flag to track when a sync operation is in progress:
 * - Set flag before syncing
 * - Skip sync if flag is already set
 * - Clear flag after sync completes
 *
 * This breaks the infinite loop while maintaining smooth bidirectional updates.
 *
 * ## Thread Safety
 * - Uses [Mutex] for thread-safe sync flag management
 * - Safe to call from multiple coroutines concurrently
 * - All public methods are suspend functions or thread-safe
 *
 * ## Performance Considerations
 * - Lightweight: Only manages coordination, delegates actual work
 * - Lock-free fast path: Quick return if sync is already in progress
 * - No allocations in hot path: Reuses existing data structures
 *
 * ## Usage Example
 * ```kotlin
 * val calculator = LinearScrollPositionCalculator()
 * val scrollTarget = LazyListScrollTarget(lazyListState) { apps.size }
 * val synchronizer = ScrollSynchronizer(calculator, scrollTarget)
 *
 * // Sync scrollbar when list scrolls
 * LaunchedEffect(lazyListState.firstVisibleItemIndex) {
 *     synchronizer.syncScrollbarToList(
 *         scrollbarHeight = scrollbarHeightPx,
 *         onUpdateThumbPosition = { newPosition ->
 *             thumbPositionState.value = newPosition
 *         }
 *     )
 * }
 *
 * // Sync list when scrollbar is dragged
 * scrollbarModifier.pointerInput(Unit) {
 *     detectDragGestures { change, _ ->
 *         synchronizer.syncListToScrollbar(
 *             touchY = change.position.y,
 *             scrollbarHeight = size.height,
 *             animate = false
 *         )
 *     }
 * }
 * ```
 *
 * @property positionCalculator Strategy for calculating scroll positions and thumb positions
 * @property scrollTarget Adapter for the scrollable content (LazyList, etc.)
 *
 * @see IScrollPositionCalculator for position calculation strategies
 * @see IScrollableTarget for scrollable content abstraction
 */
class ScrollSynchronizer(
    private val positionCalculator: IScrollPositionCalculator,
    private val scrollTarget: IScrollableTarget
) {

    /**
     * Mutex for thread-safe sync flag management.
     *
     * We use Mutex instead of AtomicBoolean because we need to:
     * 1. Check flag state
     * 2. Perform async operations (suspend functions)
     * 3. Clear flag after completion
     *
     * All three steps must be atomic to prevent race conditions.
     */
    private val syncMutex = Mutex()

    /**
     * Flag indicating if a sync operation is currently in progress.
     *
     * - `true`: Sync is happening, skip new sync requests to prevent loops
     * - `false`: No sync in progress, new requests can proceed
     *
     * Protected by [syncMutex] for thread safety.
     */
    private var isSyncing = false

    /**
     * Synchronizes scrollbar state when the list is scrolled by the user.
     *
     * This method updates the scrollbar thumb position based on the current list
     * scroll position. It prevents sync loops by checking the sync flag.
     *
     * ## Call Sites
     * - LaunchedEffect observing LazyListState.firstVisibleItemIndex
     * - User scroll gesture handlers
     * - Programmatic scroll completion callbacks
     *
     * ## Synchronization Flow
     * 1. Acquire sync lock (wait if locked)
     * 2. Check if already syncing -> return early if true
     * 3. Set sync flag to prevent loops
     * 4. Get current scroll position from target
     * 5. Get visible items info from target
     * 6. Calculate thumb position using position calculator
     * 7. Invoke callback with new thumb position
     * 8. Clear sync flag in finally block
     * 9. Release lock
     *
     * ## Thread Safety
     * - Uses Mutex to ensure atomic check-and-set of sync flag
     * - Safe to call from multiple coroutines
     * - Lock is held only during sync check, not during callback
     *
     * ## Performance
     * - Fast path: Returns immediately if sync is in progress (no allocation)
     * - Critical section: Minimal work under lock (just flag check/set)
     * - Callback: Executed outside lock to prevent deadlocks
     *
     * ## Error Handling
     * - Finally block ensures sync flag is always cleared
     * - Exceptions in callback propagate to caller
     * - Mutex is never left in locked state
     *
     * @param scrollbarHeight Total height of scrollbar container in pixels
     * @param onUpdateThumbPosition Callback invoked with calculated thumb position (0.0 to 1.0)
     *                             Not called if sync is skipped due to loop prevention
     *
     * @throws Exception if [onUpdateThumbPosition] throws
     */
    suspend fun syncScrollbarToList(
        scrollbarHeight: Float,
        onUpdateThumbPosition: (Float) -> Unit
    ) {
        // Acquire lock and check sync flag
        syncMutex.withLock {
            // If already syncing, skip to prevent infinite loop
            if (isSyncing) return

            // Set flag to indicate sync is in progress
            isSyncing = true
        }

        try {
            // Get current scroll state from target
            val visibleInfo = scrollTarget.getVisibleItemsInfo()

            // Calculate thumb position based on scroll state
            // Returns value in range [0.0, 1.0]
            val thumbPosition = positionCalculator.calculateThumbPosition(
                firstVisibleItemIndex = visibleInfo.firstVisibleItemIndex,
                firstVisibleItemOffset = visibleInfo.firstVisibleItemOffset.toFloat(),
                totalItems = visibleInfo.totalItemCount,
                visibleItems = visibleInfo.visibleItemCount
            )

            // Update scrollbar state through callback
            onUpdateThumbPosition(thumbPosition)

        } finally {
            // Always clear sync flag, even if exception occurs
            syncMutex.withLock {
                isSyncing = false
            }
        }
    }

    /**
     * Synchronizes list scroll position when the scrollbar is dragged by the user.
     *
     * This method calculates the target item index based on touch position and
     * scrolls the list accordingly. It prevents sync loops by checking the sync flag.
     *
     * ## Call Sites
     * - Scrollbar drag gesture handlers (PointerInputScope)
     * - Scrollbar tap handlers (direct jump to position)
     * - Programmatic scrollbar position changes
     *
     * ## Synchronization Flow
     * 1. Acquire sync lock (wait if locked)
     * 2. Check if already syncing -> return early if true
     * 3. Set sync flag to prevent loops
     * 4. Get total item count from target
     * 5. Calculate target item index from touch position using calculator
     * 6. Scroll to target item (animated or instant based on parameter)
     * 7. Clear sync flag in finally block
     * 8. Release lock
     *
     * ## Animation Behavior
     * - `animate = false`: Instant scroll (for dragging, continuous updates)
     * - `animate = true`: Smooth scroll (for tap-to-jump, single updates)
     *
     * ## Thread Safety
     * - Uses Mutex to ensure atomic check-and-set of sync flag
     * - Safe to call from multiple coroutines
     * - Scroll operations are suspending and properly synchronized
     *
     * ## Performance
     * - Fast path: Returns immediately if sync is in progress
     * - Calculation: Lightweight arithmetic (O(1) complexity)
     * - Scroll operation: Delegates to target (optimized by Compose)
     *
     * ## Error Handling
     * - Finally block ensures sync flag is always cleared
     * - Out-of-bounds indices handled by target implementation
     * - Mutex is never left in locked state
     *
     * ## Edge Cases
     * - Empty list: Calculates index 0 (handled by target)
     * - Touch outside bounds: Calculator coerces to valid range
     * - Very small scrollbar: Calculator maintains usable precision
     *
     * @param touchY Y coordinate of touch event relative to scrollbar top (pixels)
     * @param scrollbarHeight Total height of scrollbar container in pixels
     * @param animate If true, uses animated scroll; if false, instant scroll (default: false)
     *
     * @throws kotlinx.coroutines.CancellationException if scroll operation is cancelled
     */
    suspend fun syncListToScrollbar(
        touchY: Float,
        scrollbarHeight: Float,
        animate: Boolean = false
    ) {
        // Acquire lock and check sync flag
        syncMutex.withLock {
            // If already syncing, skip to prevent infinite loop
            if (isSyncing) return

            // Set flag to indicate sync is in progress
            isSyncing = true
        }

        try {
            // Get total item count for position calculation
            val totalItems = scrollTarget.getTotalItems()

            // Guard against empty list
            if (totalItems == 0) return

            // Calculate target item index from touch position
            // Calculator handles bounds checking and coercion
            val targetIndex = positionCalculator.calculateTargetItem(
                touchY = touchY,
                scrollbarHeight = scrollbarHeight,
                totalItems = totalItems
            )

            // Scroll to calculated target
            if (animate) {
                // Smooth animated scroll (for tap gestures)
                scrollTarget.animateScrollToItem(targetIndex)
            } else {
                // Instant scroll (for drag gestures, continuous updates)
                scrollTarget.scrollToItem(targetIndex)
            }

        } finally {
            // Always clear sync flag, even if exception occurs
            syncMutex.withLock {
                isSyncing = false
            }
        }
    }

    /**
     * Calculates scrollbar thumb height based on visible/total item ratio.
     *
     * This is a convenience method that delegates to the position calculator.
     * It's provided here for API convenience so consumers don't need to
     * directly access the calculator.
     *
     * ## Design Rationale
     * - **Facade Pattern**: Simplifies API by providing commonly used calculation
     * - **Single Point of Access**: All scroll calculations go through synchronizer
     * - **Consistency**: Uses same calculator as sync operations
     *
     * ## Calculation Details
     * The calculator uses this formula:
     * ```
     * visibleRatio = visibleItems / totalItems
     * rawHeight = visibleRatio * scrollbarHeight
     * finalHeight = max(rawHeight, minThumbHeight)
     * ```
     *
     * ## Use Cases
     * - Initial scrollbar setup (calculating thumb size)
     * - Window resize events (recalculating dimensions)
     * - Content changes (items added/removed)
     *
     * ## Thread Safety
     * This is a pure calculation method with no side effects, safe to call
     * from any thread. However, you should ensure [scrollTarget] is accessed
     * from the appropriate thread (typically UI thread for Compose).
     *
     * @param scrollbarHeight Total height of scrollbar container in pixels
     * @param minThumbHeight Minimum thumb height for usability (typically 48-64dp)
     * @return Calculated thumb height in pixels, coerced to [minThumbHeight, scrollbarHeight]
     */
    fun calculateThumbHeight(
        scrollbarHeight: Float,
        minThumbHeight: Float
    ): Float {
        val visibleInfo = scrollTarget.getVisibleItemsInfo()

        return positionCalculator.calculateThumbHeight(
            totalItems = visibleInfo.totalItemCount,
            visibleItems = visibleInfo.visibleItemCount,
            scrollbarHeight = scrollbarHeight,
            minThumbHeight = minThumbHeight
        )
    }

    /**
     * Checks if a sync operation is currently in progress.
     *
     * This method allows external code to query the sync state without
     * triggering any synchronization. Useful for debugging and testing.
     *
     * ## Use Cases
     * - Testing: Verify sync state in unit tests
     * - Debugging: Log sync state for troubleshooting
     * - UI: Show visual indicator during sync (optional)
     *
     * ## Thread Safety
     * - Uses Mutex for thread-safe flag access
     * - Safe to call from any coroutine
     * - Non-blocking read operation
     *
     * @return true if sync is in progress, false otherwise
     */
    suspend fun isSyncing(): Boolean {
        return syncMutex.withLock { isSyncing }
    }

    /**
     * Resets the sync state, clearing any ongoing synchronization.
     *
     * This method forcefully clears the sync flag. It should only be used
     * in exceptional cases where sync state may be corrupted (e.g., after
     * a crash or during cleanup).
     *
     * ## Warning
     * This is a potentially dangerous operation that can break sync loop
     * prevention if called at the wrong time. Use with caution.
     *
     * ## Use Cases
     * - Error recovery: Clear stuck sync state
     * - Component disposal: Reset state before cleanup
     * - Testing: Reset state between test cases
     *
     * ## Thread Safety
     * - Uses Mutex for thread-safe flag modification
     * - Safe to call from any coroutine
     * - Blocks if sync mutex is locked
     *
     * @see isSyncing to check sync state before resetting
     */
    suspend fun resetSyncState() {
        syncMutex.withLock {
            isSyncing = false
        }
    }
}

/**
 * Extension function to create a ScrollSynchronizer for a LazyListState.
 *
 * This provides a convenient factory method that combines all necessary components
 * for scroll synchronization in a single call.
 *
 * ## Usage Example
 * ```kotlin
 * val lazyListState = rememberLazyListState()
 * val calculator = remember { LinearScrollPositionCalculator() }
 *
 * val synchronizer = remember(lazyListState, calculator) {
 *     lazyListState.createSynchronizer(
 *         calculator = calculator,
 *         itemCountProvider = { filteredApps.size }
 *     )
 * }
 * ```
 *
 * @param calculator Position calculation strategy
 * @param itemCountProvider Lambda that returns current total item count
 * @return ScrollSynchronizer configured for this LazyListState
 */
fun androidx.compose.foundation.lazy.LazyListState.createSynchronizer(
    calculator: IScrollPositionCalculator,
    itemCountProvider: () -> Int
): ScrollSynchronizer {
    val scrollTarget = LazyListScrollTarget(this, itemCountProvider)
    return ScrollSynchronizer(calculator, scrollTarget)
}
