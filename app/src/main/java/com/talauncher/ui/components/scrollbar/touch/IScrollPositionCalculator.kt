package com.talauncher.ui.components.scrollbar.touch

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/**
 * Interface for calculating scroll positions from touch coordinates.
 *
 * This interface follows the Dependency Inversion Principle by allowing the touch handler
 * to depend on an abstraction rather than a concrete implementation. It also follows the
 * Interface Segregation Principle by defining only position calculation methods.
 *
 * Implementations are responsible for:
 * - Converting touch coordinates to scroll positions
 * - Mapping touch positions to list item indices
 * - Validating that touches are within scrollbar bounds
 * - Handling different scrollbar orientations (vertical/horizontal)
 *
 * This interface is part of the scrollbar architecture but will be implemented in a
 * separate calculation layer.
 */
interface IScrollPositionCalculator {

    /**
     * Calculates the scroll position from a touch coordinate.
     *
     * @param touchPosition The coordinate of the touch
     * @param scrollbarBounds The bounds of the scrollbar
     * @return The scroll position as a value between 0.0 and 1.0, where 0.0 is the top/start
     *         and 1.0 is the bottom/end. Returns null if the touch is outside bounds.
     */
    fun calculateScrollPosition(touchPosition: Offset, scrollbarBounds: Rect): Float?

    /**
     * Calculates the list item index from a touch coordinate.
     *
     * @param touchPosition The coordinate of the touch
     * @param scrollbarBounds The bounds of the scrollbar
     * @param totalItems The total number of items in the list
     * @return The index of the item to scroll to, or null if the touch is outside bounds
     */
    fun calculateItemIndex(
        touchPosition: Offset,
        scrollbarBounds: Rect,
        totalItems: Int
    ): Int?

    /**
     * Checks if a touch position is within the scrollbar bounds.
     *
     * @param touchPosition The coordinate to check
     * @param scrollbarBounds The bounds of the scrollbar
     * @return True if the position is within bounds, false otherwise
     */
    fun isWithinBounds(touchPosition: Offset, scrollbarBounds: Rect): Boolean
}
