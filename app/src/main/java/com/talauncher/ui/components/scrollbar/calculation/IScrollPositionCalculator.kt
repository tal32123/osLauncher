package com.talauncher.ui.components.scrollbar.calculation

/**
 * Interface defining position calculation strategies for scrollbar interactions.
 *
 * This interface follows the Strategy Pattern to allow different implementations
 * of position calculations for scrollbar behavior. It provides methods to:
 * - Map touch positions to list item indices
 * - Calculate scrollbar thumb position based on scroll state
 * - Calculate scrollbar thumb height based on visible/total item ratio
 *
 * ## Responsibilities (Single Responsibility Principle)
 * - Position mapping: Touch coordinates to item indices
 * - Thumb positioning: Scroll state to visual thumb position
 * - Thumb sizing: Content ratio to visual thumb height
 *
 * ## Implementation Contract
 * All calculations must:
 * - Return values within valid ranges (coerced bounds)
 * - Handle edge cases (empty lists, single items, zero dimensions)
 * - Use floating-point arithmetic for smooth interpolation
 * - Be deterministic and pure functions (no side effects)
 *
 * @see LinearScrollPositionCalculator for simple proportional mapping
 * @see ProportionalScrollPositionCalculator for advanced easing curves
 */
interface IScrollPositionCalculator {

    /**
     * Calculates the target list item index based on touch position.
     *
     * Maps a touch Y coordinate on the scrollbar to the corresponding item index
     * in the list. The calculation considers:
     * - Total scrollbar height (container dimensions)
     * - Total number of items in the list
     * - Edge padding to prevent accidental edge scrolling
     * - Item snapping for precise selection
     *
     * ## Mathematical Model
     * Basic formula: `itemIndex = (touchY / scrollbarHeight) * totalItems`
     * With refinements for padding, snapping, and bounds checking.
     *
     * ## Edge Cases Handled
     * - `touchY < 0`: Returns 0 (first item)
     * - `touchY > scrollbarHeight`: Returns totalItems - 1 (last item)
     * - `totalItems == 0`: Returns 0 (empty list)
     * - `scrollbarHeight == 0`: Returns 0 (invalid dimensions)
     *
     * @param touchY The Y coordinate of the touch event relative to scrollbar top (pixels)
     * @param scrollbarHeight The total height of the scrollbar container (pixels)
     * @param totalItems The total number of items in the scrollable list
     * @return The zero-based index of the target item, coerced to [0, totalItems-1]
     *
     * @see calculateThumbPosition for the inverse operation
     */
    fun calculateTargetItem(
        touchY: Float,
        scrollbarHeight: Float,
        totalItems: Int
    ): Int

    /**
     * Calculates the scrollbar thumb position based on current scroll state.
     *
     * Maps the current scroll position (first visible item index and offset) to
     * a normalized thumb position in the range [0.0, 1.0], where:
     * - 0.0 = thumb at top of scrollbar (viewing first item)
     * - 1.0 = thumb at bottom of scrollbar (viewing last item)
     *
     * ## Mathematical Model
     * ```
     * scrollPosition = firstVisibleItemIndex + (firstVisibleItemOffset / itemHeight)
     * progress = scrollPosition / maxScrollPosition
     * thumbPosition = progress * (1.0 - thumbHeightRatio)
     * ```
     *
     * The calculation accounts for thumb height to ensure the thumb bottom never
     * exceeds the scrollbar bounds when at maximum scroll position.
     *
     * ## Use Cases
     * - Updating thumb position during scroll events
     * - Rendering thumb in correct position
     * - Animating thumb movement
     *
     * @param firstVisibleItemIndex Zero-based index of the first visible item
     * @param firstVisibleItemOffset Pixel offset of the first visible item (negative or zero)
     * @param totalItems Total number of items in the list
     * @param visibleItems Approximate number of items visible on screen
     * @return Normalized thumb position in range [0.0, 1.0]
     *
     * @see calculateTargetItem for the inverse operation
     */
    fun calculateThumbPosition(
        firstVisibleItemIndex: Int,
        firstVisibleItemOffset: Float,
        totalItems: Int,
        visibleItems: Int
    ): Float

    /**
     * Calculates the scrollbar thumb height based on visible/total item ratio.
     *
     * Determines the thumb height as a proportion of the total scrollbar height,
     * based on how much content is visible at once. The calculation follows
     * standard scrollbar behavior:
     *
     * ## Mathematical Model
     * ```
     * visibleRatio = visibleItems / totalItems
     * thumbHeight = visibleRatio * scrollbarHeight
     * clampedHeight = max(thumbHeight, minThumbHeight)
     * ```
     *
     * ## Behavior
     * - More visible items = larger thumb (indicates less content to scroll)
     * - Fewer visible items = smaller thumb (indicates more content to scroll)
     * - All items visible = thumb fills entire scrollbar (no scrolling needed)
     * - Minimum thumb height ensures usability even with thousands of items
     *
     * ## Edge Cases Handled
     * - `totalItems == 0`: Returns minThumbHeight
     * - `visibleItems >= totalItems`: Returns scrollbarHeight (all visible)
     * - Very small ratio: Returns minThumbHeight for usability
     *
     * @param totalItems Total number of items in the list
     * @param visibleItems Approximate number of items visible on screen
     * @param scrollbarHeight Total height of the scrollbar container (pixels)
     * @param minThumbHeight Minimum thumb height for usability (pixels, typically 48-64dp)
     * @return Calculated thumb height in pixels, coerced to [minThumbHeight, scrollbarHeight]
     *
     * @see calculateThumbPosition which uses this height in its calculations
     */
    fun calculateThumbHeight(
        totalItems: Int,
        visibleItems: Int,
        scrollbarHeight: Float,
        minThumbHeight: Float
    ): Float
}
