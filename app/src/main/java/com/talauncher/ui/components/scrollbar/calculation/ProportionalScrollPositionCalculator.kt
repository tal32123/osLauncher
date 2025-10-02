package com.talauncher.ui.components.scrollbar.calculation

import kotlin.math.cos
import kotlin.math.max
import kotlin.math.PI
import kotlin.math.pow

/**
 * Advanced proportional mapping implementation with acceleration curves.
 *
 * This implementation extends linear mapping with sophisticated easing functions
 * for smoother, more natural scrolling behavior. It's ideal for:
 * - Lists with varying item heights
 * - Enhanced user experience with smooth animations
 * - Large lists where precision at edges is important
 * - Touch interfaces where natural feel is critical
 *
 * ## Strategy Pattern Implementation
 * Implements [IScrollPositionCalculator] with configurable acceleration curves.
 * Supports multiple easing functions via [AccelerationCurve] enum.
 *
 * ## Mathematical Models
 *
 * ### Linear (same as LinearScrollPositionCalculator)
 * ```
 * f(x) = x
 * ```
 *
 * ### EaseInOut (Cosine-based smooth curve)
 * ```
 * f(x) = (1 - cos(x * π)) / 2
 * ```
 * - Smooth acceleration at start (ease in)
 * - Smooth deceleration at end (ease out)
 * - Natural feeling for human interaction
 *
 * ### Exponential (Quadratic easing)
 * ```
 * f(x) = x^2              for x < 0.5
 * f(x) = 1 - (1-x)^2      for x >= 0.5
 * ```
 * - Faster response in middle range
 * - Maintains precision at edges
 * - Good for large lists with quick navigation
 *
 * ## Configuration
 * Extends [LinearScrollPositionCalculator] configuration with:
 * - Acceleration curve selection
 * - All linear calculator options (padding, snapping, min height)
 *
 * @property config Configuration options including acceleration curve
 *
 * @see LinearScrollPositionCalculator for basic linear mapping
 */
class ProportionalScrollPositionCalculator(
    private val config: PositionCalculatorConfig = PositionCalculatorConfig()
) : IScrollPositionCalculator {

    /**
     * Calculates target item using proportional mapping with acceleration curves.
     *
     * ## Implementation Details
     * 1. Apply edge padding if configured
     * 2. Calculate normalized position (0.0 to 1.0)
     * 3. Apply acceleration curve easing function
     * 4. Map eased position to item index
     * 5. Snap to nearest item if configured
     * 6. Coerce to valid range
     *
     * ## Acceleration Curve Impact
     * - **Linear**: Direct mapping, no easing
     * - **EaseInOut**: Slow start/end, faster middle (feels natural)
     * - **Exponential**: Very fast middle, precise edges (good for large lists)
     *
     * ## Example (1000 items, EaseInOut)
     * - Touch at 10% → Item ~50 (slower near top)
     * - Touch at 50% → Item ~500 (linear in middle)
     * - Touch at 90% → Item ~950 (slower near bottom)
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

        // Apply edge padding
        val effectiveHeight = max(0f, scrollbarHeight - config.edgePadding * 2)
        if (effectiveHeight <= 0f) return 0

        val adjustedTouchY = applyEdgePadding(touchY, scrollbarHeight, config.edgePadding)

        // Calculate normalized position (0.0 to 1.0)
        val normalizedPosition = calculateNormalizedProgress(
            value = adjustedTouchY,
            min = 0f,
            max = effectiveHeight
        )

        // Apply acceleration curve
        val easedPosition = applyAccelerationCurve(normalizedPosition, config.accelerationCurve)

        // Map to item index
        val rawItemIndex = easedPosition * (totalItems - 1)

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
     * Calculates thumb position using proportional mapping.
     *
     * ## Implementation Details
     * This uses linear mapping for thumb position since acceleration curves
     * should only affect user input (touch to item), not visual feedback
     * (scroll to thumb position). This maintains visual consistency.
     *
     * ## Design Rationale
     * - User input gets acceleration curves for better feel
     * - Visual feedback remains linear for predictability
     * - Thumb position directly reflects actual scroll position
     *
     * The implementation is identical to [LinearScrollPositionCalculator]
     * to maintain consistent visual feedback regardless of input curve.
     *
     * ## Time Complexity: O(1)
     *
     * @param firstVisibleItemIndex Zero-based index of first visible item
     * @param firstVisibleItemOffset Pixel offset of first visible item
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
        if (visibleItems >= totalItems) return 0f

        // Calculate max scroll position
        val maxScrollPosition = calculateMaxScrollPosition(totalItems, visibleItems)
        if (maxScrollPosition <= 0) return 0f

        // Estimate item height and calculate scroll position
        val estimatedItemHeight = 100f
        val currentScrollPosition = calculateScrollPosition(
            firstVisibleItemIndex,
            firstVisibleItemOffset,
            estimatedItemHeight
        )

        // Calculate scroll progress
        val scrollProgress = calculateNormalizedProgress(
            value = currentScrollPosition,
            min = 0f,
            max = maxScrollPosition.toFloat()
        )

        // Calculate thumb height ratio
        val thumbHeightRatio = (visibleItems.toFloat() / totalItems.toFloat()).coerceIn(0f, 1f)

        // Adjust position for thumb height
        val adjustedPosition = scrollProgress * (1f - thumbHeightRatio)

        return adjustedPosition.coerceIn(0f, 1f)
    }

    /**
     * Calculates thumb height based on visible/total ratio.
     *
     * ## Implementation Details
     * Identical to [LinearScrollPositionCalculator.calculateThumbHeight].
     * Thumb sizing is independent of acceleration curves and remains proportional
     * for consistent visual feedback.
     *
     * ## Time Complexity: O(1)
     *
     * @param totalItems Total number of items
     * @param visibleItems Number of items visible at once
     * @param scrollbarHeight Total scrollbar height
     * @param minThumbHeight Minimum thumb height
     * @return Calculated thumb height in pixels
     */
    override fun calculateThumbHeight(
        totalItems: Int,
        visibleItems: Int,
        scrollbarHeight: Float,
        minThumbHeight: Float
    ): Float {
        // Handle edge cases
        if (totalItems <= 0) return minThumbHeight
        if (visibleItems >= totalItems) return scrollbarHeight
        if (scrollbarHeight <= 0f) return minThumbHeight

        // Calculate visible ratio
        val visibleRatio = visibleItems.toFloat() / totalItems.toFloat()

        // Calculate proportional height
        val proportionalHeight = visibleRatio * scrollbarHeight

        // Apply minimum height
        val finalHeight = max(minThumbHeight, proportionalHeight)

        return finalHeight.coerceIn(minThumbHeight, scrollbarHeight)
    }

    /**
     * Applies the configured acceleration curve to a normalized position.
     *
     * ## Mathematical Implementations
     *
     * ### Linear
     * ```kotlin
     * f(x) = x
     * ```
     * No transformation, direct mapping.
     *
     * ### EaseInOut (Cosine-based)
     * ```kotlin
     * f(x) = (1 - cos(x * π)) / 2
     * ```
     * - At x=0: f(0) = 0 (starts at 0)
     * - At x=0.5: f(0.5) = 0.5 (linear in middle)
     * - At x=1: f(1) = 1 (ends at 1)
     * - Smooth curve throughout
     *
     * ### Exponential (Quadratic easing)
     * ```kotlin
     * if (x < 0.5):
     *     f(x) = 2 * x^2
     * else:
     *     f(x) = 1 - 2 * (1 - x)^2
     * ```
     * - Mirror-symmetric around center
     * - Faster in middle, precise at edges
     *
     * ## Input/Output Contract
     * - Input: normalized position in [0.0, 1.0]
     * - Output: eased position in [0.0, 1.0]
     * - Pure function (no side effects)
     *
     * @param normalizedPosition Input position in range [0.0, 1.0]
     * @param curve Acceleration curve to apply
     * @return Eased position in range [0.0, 1.0]
     */
    private fun applyAccelerationCurve(normalizedPosition: Float, curve: AccelerationCurve): Float {
        // Ensure input is in valid range
        val position = normalizedPosition.coerceIn(0f, 1f)

        return when (curve) {
            AccelerationCurve.Linear -> {
                // No transformation
                position
            }

            AccelerationCurve.EaseInOut -> {
                // Cosine-based smooth ease in-out
                // f(x) = (1 - cos(x * π)) / 2
                val easedValue = (1f - cos(position * PI.toFloat())) / 2f
                easedValue.coerceIn(0f, 1f)
            }

            AccelerationCurve.Exponential -> {
                // Quadratic easing with symmetry
                // Ease in for first half, ease out for second half
                val easedValue = if (position < 0.5f) {
                    // First half: f(x) = 2 * x^2
                    2f * position.pow(2)
                } else {
                    // Second half: f(x) = 1 - 2 * (1-x)^2
                    val reversed = 1f - position
                    1f - 2f * reversed.pow(2)
                }
                easedValue.coerceIn(0f, 1f)
            }
        }
    }
}
