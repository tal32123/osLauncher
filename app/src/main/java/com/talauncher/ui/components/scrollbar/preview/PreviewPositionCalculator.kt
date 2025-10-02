package com.talauncher.ui.components.scrollbar.preview

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Configuration for preview position calculations.
 *
 * This data class encapsulates all parameters needed for preview positioning,
 * following the **Single Responsibility Principle** by managing only configuration data.
 *
 * @param horizontalOffset Offset from the scrollbar edge (default: 16dp to the left)
 * @param verticalCenteringEnabled Whether to center preview vertically at scroll position
 * @param topPadding Minimum padding from top of viewport
 * @param bottomPadding Minimum padding from bottom of viewport
 * @param preventOverflow Whether to prevent preview from going off-screen
 */
data class PreviewPositionConfig(
    val horizontalOffset: Dp = 16.dp,
    val verticalCenteringEnabled: Boolean = true,
    val topPadding: Dp = 8.dp,
    val bottomPadding: Dp = 8.dp,
    val preventOverflow: Boolean = true
) {
    init {
        require(horizontalOffset >= 0.dp) { "Horizontal offset must be non-negative" }
        require(topPadding >= 0.dp) { "Top padding must be non-negative" }
        require(bottomPadding >= 0.dp) { "Bottom padding must be non-negative" }
    }
}

/**
 * Calculator for preview popup positioning.
 *
 * This class follows **Single Responsibility Principle** by handling only position calculations.
 * It uses the **Strategy Pattern** to provide different positioning strategies based on configuration.
 *
 * Architecture benefits:
 * - **Separation of Concerns**: Position logic isolated from rendering logic
 * - **Testability**: Pure functions with no side effects, easy to unit test
 * - **Maintainability**: Position algorithm can be updated without touching UI code
 *
 * @param config Configuration for position calculations
 */
class PreviewPositionCalculator(
    private val config: PreviewPositionConfig = PreviewPositionConfig()
) {
    /**
     * Calculates the optimal position for the preview popup.
     *
     * Algorithm:
     * 1. Calculate base horizontal position (to the left of scrollbar)
     * 2. Calculate vertical position based on scroll position
     * 3. Apply centering if enabled
     * 4. Apply overflow prevention to keep preview on screen
     *
     * @param normalizedPosition Current scroll position (0f to 1f)
     * @param scrollbarBounds Bounds of the scrollbar component
     * @param previewHeight Height of the preview popup (pixels)
     * @param previewWidth Width of the preview popup (pixels)
     * @param density Screen density for Dp to pixel conversion
     * @return IntOffset representing the top-left corner position
     */
    fun calculatePosition(
        normalizedPosition: Float,
        scrollbarBounds: ScrollbarBounds,
        previewHeight: Float,
        previewWidth: Float,
        density: Float = 1f
    ): IntOffset {
        // Validate inputs
        require(normalizedPosition in 0f..1f) { "Normalized position must be between 0 and 1" }
        require(previewHeight > 0f) { "Preview height must be positive" }
        require(previewWidth > 0f) { "Preview width must be positive" }

        // Calculate horizontal position (left of scrollbar with offset)
        val horizontalOffsetPx = config.horizontalOffset.value * density
        val x = calculateHorizontalPosition(
            scrollbarX = scrollbarBounds.x,
            previewWidth = previewWidth,
            offsetDp = horizontalOffsetPx
        )

        // Calculate vertical position based on scroll position
        val baseY = calculateBaseVerticalPosition(
            normalizedPosition = normalizedPosition,
            scrollbarBounds = scrollbarBounds,
            previewHeight = previewHeight
        )

        // Apply centering if enabled
        val centeredY = if (config.verticalCenteringEnabled) {
            centerVertically(baseY, previewHeight)
        } else {
            baseY
        }

        // Apply overflow prevention
        val finalY = if (config.preventOverflow) {
            preventVerticalOverflow(
                y = centeredY,
                previewHeight = previewHeight,
                scrollbarBounds = scrollbarBounds,
                density = density
            )
        } else {
            centeredY
        }

        return IntOffset(x.roundToInt(), finalY.roundToInt())
    }

    /**
     * Calculates horizontal position to the left of scrollbar.
     *
     * @param scrollbarX X position of scrollbar left edge
     * @param previewWidth Width of preview popup
     * @param offsetDp Additional offset in dp
     * @return X coordinate for preview left edge
     */
    private fun calculateHorizontalPosition(
        scrollbarX: Float,
        previewWidth: Float,
        offsetDp: Float
    ): Float {
        // Position to the left of scrollbar: scrollbar X - preview width - offset
        return scrollbarX - previewWidth - offsetDp
    }

    /**
     * Calculates base vertical position based on scroll position.
     *
     * Maps the normalized scroll position (0..1) to the scrollbar height,
     * determining where along the scrollbar the preview should appear.
     *
     * @param normalizedPosition Scroll position (0f to 1f)
     * @param scrollbarBounds Scrollbar bounds
     * @param previewHeight Height of preview
     * @return Base Y coordinate
     */
    private fun calculateBaseVerticalPosition(
        normalizedPosition: Float,
        scrollbarBounds: ScrollbarBounds,
        previewHeight: Float
    ): Float {
        // Calculate Y position along scrollbar track
        val trackY = scrollbarBounds.y + (normalizedPosition * scrollbarBounds.height)

        return trackY
    }

    /**
     * Centers preview vertically around the calculated position.
     *
     * @param baseY Base Y position
     * @param previewHeight Height of preview
     * @return Centered Y coordinate
     */
    private fun centerVertically(
        baseY: Float,
        previewHeight: Float
    ): Float {
        // Center the preview around the base position
        return baseY - (previewHeight / 2f)
    }

    /**
     * Prevents preview from overflowing screen bounds.
     *
     * This ensures the preview stays within the viewport with appropriate padding,
     * following **defensive programming** practices.
     *
     * @param y Current Y position
     * @param previewHeight Height of preview
     * @param scrollbarBounds Scrollbar bounds for viewport info
     * @param density Screen density for Dp to pixel conversion
     * @return Adjusted Y coordinate within bounds
     */
    private fun preventVerticalOverflow(
        y: Float,
        previewHeight: Float,
        scrollbarBounds: ScrollbarBounds,
        density: Float
    ): Float {
        val topPaddingPx = config.topPadding.value * density
        val bottomPaddingPx = config.bottomPadding.value * density

        val minY = scrollbarBounds.y + topPaddingPx
        val maxY = scrollbarBounds.y + scrollbarBounds.viewportHeight - previewHeight - bottomPaddingPx

        return y.coerceIn(minY, maxY.coerceAtLeast(minY))
    }

    /**
     * Calculates preview position for a specific item index.
     *
     * Convenience method for list-based previews where you know the item index.
     *
     * @param itemIndex Current item index
     * @param totalItems Total number of items
     * @param scrollbarBounds Scrollbar bounds
     * @param previewHeight Preview height
     * @param previewWidth Preview width
     * @param density Screen density for Dp to pixel conversion
     * @return IntOffset for preview position
     */
    fun calculatePositionForIndex(
        itemIndex: Int,
        totalItems: Int,
        scrollbarBounds: ScrollbarBounds,
        previewHeight: Float,
        previewWidth: Float,
        density: Float = 1f
    ): IntOffset {
        require(itemIndex >= 0) { "Item index must be non-negative" }
        require(totalItems > 0) { "Total items must be positive" }
        require(itemIndex < totalItems) { "Item index must be less than total items" }

        val normalizedPosition = if (totalItems > 1) {
            itemIndex.toFloat() / (totalItems - 1)
        } else {
            0f
        }

        return calculatePosition(
            normalizedPosition = normalizedPosition,
            scrollbarBounds = scrollbarBounds,
            previewHeight = previewHeight,
            previewWidth = previewWidth,
            density = density
        )
    }
}

