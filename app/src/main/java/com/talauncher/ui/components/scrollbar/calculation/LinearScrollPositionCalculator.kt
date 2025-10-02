package com.talauncher.ui.components.scrollbar.calculation

import kotlin.math.max

/**
 * Linear proportional mapping implementation for scrollbar position calculations.
 *
 * This implementation provides simple, predictable position mapping using direct
 * linear proportions. It's the most straightforward calculator, ideal for:
 * - Lists with uniform item heights
 * - Predictable, direct user interaction
 * - Performance-critical scenarios (no complex math)
 *
 * ## Strategy Pattern Implementation
 * Implements [IScrollPositionCalculator] with linear mapping strategy.
 * Can be swapped with [ProportionalScrollPositionCalculator] for different behavior.
 *
 * ## Mathematical Model
 * All calculations use simple linear proportions:
 * ```
 * targetItem = (touchY / scrollbarHeight) * totalItems
 * thumbPosition = (scrollPosition / maxScrollPosition)
 * thumbHeight = (visibleItems / totalItems) * scrollbarHeight
 * ```
 *
 * ## Configuration
 * Supports optional configuration for:
 * - Edge padding to prevent accidental edge scrolling
 * - Item snapping for precise selection
 * - Minimum thumb height for usability
 *
 * @property config Configuration options for calculation behavior
 *
 * @see ProportionalScrollPositionCalculator for advanced easing curves
 */
class LinearScrollPositionCalculator(
    private val config: PositionCalculatorConfig = PositionCalculatorConfig()
) : IScrollPositionCalculator {

    /**
     * Calculates target item using linear proportion mapping.
     *
     * ## Implementation Details
     * 1. Apply edge padding if configured (reduces effective touch area)
     * 2. Calculate effective scrollbar height (total - 2*padding)
     * 3. Calculate raw position as linear proportion: `touchY / height * totalItems`
     * 4. Snap to nearest item if configured
     * 5. Coerce result to valid range [0, totalItems-1]
     *
     * ## Edge Case Handling
     * - Empty list (totalItems = 0): Returns 0
     * - Zero height: Returns 0
     * - Negative touchY: Coerced to 0
     * - TouchY beyond height: Coerced to totalItems-1
     *
     * ## Time Complexity: O(1)
     *
     * @param touchY Touch Y coordinate relative to scrollbar top (pixels)
     * @param scrollbarHeight Total scrollbar container height (pixels)
     * @param totalItems Total number of items in the list
     * @return Zero-based target item index in range [0, totalItems-1]
     */
    override fun calculateTargetItem(
        touchY: Float,
        scrollbarHeight: Float,
        totalItems: Int
    ): Int {
        // Handle edge cases
        if (totalItems <= 0) return 0
        if (scrollbarHeight <= 0f) return 0

        // Apply edge padding to touch position
        val effectiveHeight = max(0f, scrollbarHeight - config.edgePadding * 2)
        if (effectiveHeight <= 0f) return 0

        val adjustedTouchY = applyEdgePadding(touchY, scrollbarHeight, config.edgePadding)

        // Calculate linear proportion
        val normalizedPosition = calculateNormalizedProgress(
            value = adjustedTouchY,
            min = 0f,
            max = effectiveHeight
        )

        // Map to item index
        val rawItemIndex = normalizedPosition * (totalItems - 1)

        // Apply snapping if configured
        val targetIndex = if (config.snapToItems) {
            snapToNearestItem(rawItemIndex)
        } else {
            rawItemIndex.toInt()
        }

        // Coerce to valid range
        return targetIndex.coerceInRange(0, totalItems - 1)
    }

    /**
     * Calculates thumb position using linear proportion mapping.
     *
     * ## Implementation Details
     * 1. Calculate current scroll position from index and offset
     * 2. Calculate maximum scroll position (totalItems - visibleItems)
     * 3. Calculate scroll progress as proportion: `scrollPosition / maxScrollPosition`
     * 4. Calculate thumb height ratio
     * 5. Adjust position to account for thumb height: `progress * (1 - thumbHeightRatio)`
     *
     * ## Why adjust for thumb height?
     * When scrolled to bottom, the thumb's bottom edge (not center) should align
     * with the scrollbar bottom. Without adjustment, the thumb would extend beyond bounds.
     *
     * ## Edge Case Handling
     * - All items visible (visibleItems >= totalItems): Returns 0.0 (no scroll)
     * - Zero items: Returns 0.0
     * - Negative offset: Uses absolute value for calculation
     *
     * ## Time Complexity: O(1)
     *
     * @param firstVisibleItemIndex Zero-based index of first visible item
     * @param firstVisibleItemOffset Pixel offset of first visible item (typically <= 0)
     * @param totalItems Total number of items
     * @param visibleItems Number of items visible on screen
     * @return Normalized thumb position in range [0.0, 1.0]
     */
    override fun calculateThumbPosition(
        firstVisibleItemIndex: Int,
        firstVisibleItemOffset: Float,
        totalItems: Int,
        visibleItems: Int
    ): Float {
        // Handle edge cases
        if (totalItems <= 0 || visibleItems <= 0) return 0f
        if (visibleItems >= totalItems) return 0f // All items visible, no scrolling

        // Calculate current scroll position
        val maxScrollPosition = calculateMaxScrollPosition(totalItems, visibleItems)
        if (maxScrollPosition <= 0) return 0f

        // Estimate item height and calculate scroll position
        val estimatedItemHeight = 100f // Reasonable default for calculation
        val currentScrollPosition = calculateScrollPosition(
            firstVisibleItemIndex,
            firstVisibleItemOffset,
            estimatedItemHeight
        )

        // Calculate scroll progress (0.0 to 1.0)
        val scrollProgress = calculateNormalizedProgress(
            value = currentScrollPosition,
            min = 0f,
            max = maxScrollPosition.toFloat()
        )

        // Calculate thumb height ratio
        val thumbHeightRatio = (visibleItems.toFloat() / totalItems.toFloat()).coerceIn(0f, 1f)

        // Adjust position to account for thumb height
        // This ensures thumb bottom aligns with scrollbar bottom at max scroll
        val adjustedPosition = scrollProgress * (1f - thumbHeightRatio)

        return adjustedPosition.coerceIn(0f, 1f)
    }

    /**
     * Calculates thumb height based on visible/total item ratio.
     *
     * ## Implementation Details
     * 1. Calculate visible ratio: `visibleItems / totalItems`
     * 2. Calculate proportional height: `ratio * scrollbarHeight`
     * 3. Clamp to minimum height for usability
     * 4. Coerce to scrollbar bounds
     *
     * ## Design Rationale
     * - Larger thumb = less content to scroll (more content visible)
     * - Smaller thumb = more content to scroll (less content visible)
     * - Minimum height ensures thumb is always grabbable
     *
     * ## Edge Case Handling
     * - All items visible: Returns full scrollbar height
     * - Zero items: Returns minimum height
     * - Very large lists (1000+ items): Returns minimum height
     *
     * ## Time Complexity: O(1)
     *
     * @param totalItems Total number of items in list
     * @param visibleItems Number of items visible at once
     * @param scrollbarHeight Total scrollbar container height
     * @param minThumbHeight Minimum thumb height for usability
     * @return Calculated thumb height in pixels, in range [minThumbHeight, scrollbarHeight]
     */
    override fun calculateThumbHeight(
        totalItems: Int,
        visibleItems: Int,
        scrollbarHeight: Float,
        minThumbHeight: Float
    ): Float {
        // Handle edge cases
        if (totalItems <= 0) return minThumbHeight
        if (visibleItems >= totalItems) return scrollbarHeight // All items visible
        if (scrollbarHeight <= 0f) return minThumbHeight

        // Calculate visible ratio
        val visibleRatio = visibleItems.toFloat() / totalItems.toFloat()

        // Calculate proportional height
        val proportionalHeight = visibleRatio * scrollbarHeight

        // Apply minimum height and coerce to bounds
        val finalHeight = max(minThumbHeight, proportionalHeight)

        return finalHeight.coerceIn(minThumbHeight, scrollbarHeight)
    }
}
