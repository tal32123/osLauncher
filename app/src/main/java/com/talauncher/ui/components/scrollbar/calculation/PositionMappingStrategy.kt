package com.talauncher.ui.components.scrollbar.calculation

import kotlin.math.max
import kotlin.math.min

/**
 * Configuration for position calculation behavior.
 *
 * This data class encapsulates all configuration options for position calculators,
 * following the Dependency Inversion Principle by allowing behavior customization
 * without modifying calculator implementations.
 *
 * ## Design Pattern: Strategy Configuration
 * Provides a type-safe way to configure calculator strategies with sensible defaults.
 *
 * @property minThumbHeight Minimum thumb height in pixels for usability (default: 48dp)
 * @property snapToItems Whether to snap to exact item positions (default: true)
 * @property edgePadding Padding at top/bottom edges in pixels to prevent accidental scrolling (default: 8dp)
 * @property accelerationCurve The easing curve to apply for proportional calculators (default: Linear)
 */
data class PositionCalculatorConfig(
    val minThumbHeight: Float = 48f,
    val snapToItems: Boolean = true,
    val edgePadding: Float = 8f,
    val accelerationCurve: AccelerationCurve = AccelerationCurve.Linear
)

/**
 * Acceleration curves for position mapping.
 *
 * Defines different easing functions that can be applied to position calculations
 * for smoother or more responsive scrolling behavior.
 *
 * ## Use Cases
 * - **Linear**: Direct proportional mapping, predictable and simple
 * - **EaseInOut**: Smooth acceleration/deceleration, feels natural
 * - **Exponential**: Faster response in middle range, precision at edges
 *
 * @see ProportionalScrollPositionCalculator for usage
 */
enum class AccelerationCurve {
    /**
     * Linear mapping with no easing.
     * f(x) = x
     */
    Linear,

    /**
     * Smooth ease-in-out curve using cosine interpolation.
     * f(x) = (1 - cos(x * π)) / 2
     * Provides smooth acceleration at start and deceleration at end.
     */
    EaseInOut,

    /**
     * Exponential curve for faster middle-range response.
     * f(x) = x^2 for x < 0.5, 1 - (1-x)^2 for x >= 0.5
     * Allows quick navigation while maintaining edge precision.
     */
    Exponential
}

/**
 * Extension functions for common position calculation utilities.
 *
 * These pure functions provide reusable calculation logic that can be used
 * across different calculator implementations, following the DRY principle.
 */

/**
 * Coerces a value to a specified range.
 *
 * @param min Minimum allowed value
 * @param max Maximum allowed value
 * @return Value clamped to [min, max]
 */
fun Float.coerceInRange(min: Float, max: Float): Float = max(min, min(this, max))

/**
 * Coerces an Int to a specified range.
 *
 * @param min Minimum allowed value
 * @param max Maximum allowed value
 * @return Value clamped to [min, max]
 */
fun Int.coerceInRange(min: Int, max: Int): Int = max(min, min(this, max))

/**
 * Calculates normalized progress (0.0 to 1.0) from a value within a range.
 *
 * ## Formula
 * ```
 * progress = (value - min) / (max - min)
 * ```
 *
 * ## Edge Cases
 * - If max == min, returns 0.0 (prevents division by zero)
 * - Result is coerced to [0.0, 1.0]
 *
 * @param value Current value
 * @param min Minimum of range
 * @param max Maximum of range
 * @return Normalized progress in [0.0, 1.0]
 */
fun calculateNormalizedProgress(value: Float, min: Float, max: Float): Float {
    if (max <= min) return 0f
    return ((value - min) / (max - min)).coerceIn(0f, 1f)
}

/**
 * Interpolates linearly between two values.
 *
 * ## Formula
 * ```
 * result = start + (end - start) * fraction
 * ```
 *
 * @param start Start value (at fraction = 0.0)
 * @param end End value (at fraction = 1.0)
 * @param fraction Interpolation fraction [0.0, 1.0]
 * @return Interpolated value
 */
fun lerp(start: Float, end: Float, fraction: Float): Float {
    return start + (end - start) * fraction
}

/**
 * Calculates the current scroll position from list state parameters.
 *
 * This function converts discrete scroll state (item index + offset) into a
 * continuous position value for smooth calculations.
 *
 * ## Formula
 * ```
 * scrollPosition = firstVisibleItemIndex + abs(firstVisibleItemOffset) / averageItemHeight
 * ```
 *
 * ## Assumptions
 * - Items have uniform height (or an average is acceptable)
 * - Offset is typically negative or zero
 * - Average item height is estimated from scrollbar/visible items
 *
 * @param firstVisibleItemIndex Zero-based index of first visible item
 * @param firstVisibleItemOffset Pixel offset of first visible item (typically <= 0)
 * @param estimatedItemHeight Estimated or average height of a single item
 * @return Continuous scroll position value
 */
fun calculateScrollPosition(
    firstVisibleItemIndex: Int,
    firstVisibleItemOffset: Float,
    estimatedItemHeight: Float
): Float {
    if (estimatedItemHeight <= 0f) return firstVisibleItemIndex.toFloat()

    // Offset is typically negative, so we use absolute value
    val offsetRatio = kotlin.math.abs(firstVisibleItemOffset) / estimatedItemHeight
    return firstVisibleItemIndex + offsetRatio
}

/**
 * Calculates the maximum scroll position for a list.
 *
 * The maximum scroll position is when the last item is at the bottom of the
 * visible area.
 *
 * ## Formula
 * ```
 * maxScrollPosition = max(0, totalItems - visibleItems)
 * ```
 *
 * @param totalItems Total number of items in the list
 * @param visibleItems Number of items visible at once
 * @return Maximum scroll position (minimum is 0)
 */
fun calculateMaxScrollPosition(totalItems: Int, visibleItems: Int): Int {
    return max(0, totalItems - visibleItems)
}

/**
 * Applies edge padding to a touch position.
 *
 * Reduces the effective touch area by padding amount at top and bottom edges,
 * helping prevent accidental scrolling to extreme positions.
 *
 * ## Behavior
 * - Touches within top padding region map to position 0
 * - Touches within bottom padding region map to position (height - padding)
 * - Touches in middle are linearly remapped
 *
 * @param touchY Original touch Y coordinate
 * @param totalHeight Total scrollbar height
 * @param padding Padding amount in pixels
 * @return Adjusted touch position with padding applied
 */
fun applyEdgePadding(touchY: Float, totalHeight: Float, padding: Float): Float {
    if (padding <= 0f || totalHeight <= padding * 2) return touchY

    return when {
        touchY < padding -> 0f
        touchY > totalHeight - padding -> totalHeight - padding * 2
        else -> touchY - padding
    }
}

/**
 * Snaps a fractional item index to the nearest integer.
 *
 * Used when snapToItems is enabled to ensure clean item-to-item navigation.
 *
 * @param itemIndex Fractional item index
 * @return Rounded item index
 */
fun snapToNearestItem(itemIndex: Float): Int {
    return kotlin.math.round(itemIndex).toInt()
}

/**
 * Calculates estimated item height from scrollbar and content dimensions.
 *
 * Provides a reasonable estimate of average item height for calculations
 * when exact item measurements aren't available.
 *
 * ## Formula
 * ```
 * itemHeight = scrollbarHeight / visibleItems
 * ```
 *
 * @param scrollbarHeight Total scrollbar container height
 * @param visibleItems Number of items visible at once
 * @return Estimated height per item (minimum 1f to prevent division by zero)
 */
fun estimateItemHeight(scrollbarHeight: Float, visibleItems: Int): Float {
    if (visibleItems <= 0) return scrollbarHeight
    return max(1f, scrollbarHeight / visibleItems)
}
