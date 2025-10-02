package com.talauncher.ui.components.scrollbar.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset

/**
 * Interface for providing scrollbar preview functionality.
 *
 * This interface follows the **Dependency Inversion Principle** by abstracting the preview
 * implementation details. Different preview styles (app preview, letter index, etc.) can
 * be implemented without changing the scrollbar component.
 *
 * Following SOLID principles:
 * - **Single Responsibility**: Manages only preview rendering and positioning
 * - **Open/Closed**: Open for extension (new preview types) but closed for modification
 * - **Interface Segregation**: Provides minimal interface needed for preview functionality
 *
 * Architecture pattern: **Strategy Pattern**
 * Different preview strategies can be swapped at runtime based on requirements.
 *
 * @see AppPreviewProvider for app-specific implementation
 */
interface IPreviewProvider {
    /**
     * Renders the preview UI at the calculated position.
     *
     * This composable function is responsible for displaying the preview popup
     * with appropriate content and styling.
     *
     * @param normalizedPosition Current scroll position normalized to 0f..1f range
     * @param scrollbarBounds Bounds of the scrollbar for positioning calculations
     * @param enableGlassmorphism Whether to apply glassmorphism effects
     */
    @Composable
    fun ProvidePreview(
        normalizedPosition: Float,
        scrollbarBounds: ScrollbarBounds,
        enableGlassmorphism: Boolean
    )

    /**
     * Calculates the optimal position for the preview popup.
     *
     * This method ensures the preview stays within screen bounds and positions
     * it appropriately relative to the scrollbar.
     *
     * @param normalizedPosition Current scroll position (0f to 1f)
     * @param scrollbarBounds Bounds of the scrollbar component
     * @param previewHeight Height of the preview popup
     * @param previewWidth Width of the preview popup
     * @return IntOffset representing the top-left corner of the preview
     */
    fun calculatePreviewPosition(
        normalizedPosition: Float,
        scrollbarBounds: ScrollbarBounds,
        previewHeight: Float,
        previewWidth: Float
    ): IntOffset
}

/**
 * Data class representing the bounds of the scrollbar component.
 *
 * This encapsulates positioning information needed for preview calculations,
 * following the **Single Responsibility Principle**.
 *
 * @param x X coordinate of the scrollbar (left edge)
 * @param y Y coordinate of the scrollbar (top edge)
 * @param width Width of the scrollbar
 * @param height Height of the scrollbar
 * @param viewportHeight Total height of the viewport
 */
data class ScrollbarBounds(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val viewportHeight: Float
) {
    /**
     * Right edge of the scrollbar.
     */
    val right: Float
        get() = x + width

    /**
     * Bottom edge of the scrollbar.
     */
    val bottom: Float
        get() = y + height

    init {
        require(width >= 0f) { "Width must be non-negative" }
        require(height >= 0f) { "Height must be non-negative" }
        require(viewportHeight > 0f) { "Viewport height must be positive" }
    }
}

/**
 * Empty preview provider that renders nothing.
 *
 * This follows the **Null Object Pattern**, providing a safe default
 * when no preview is needed. Eliminates null checks and provides
 * consistent behavior.
 */
object EmptyPreviewProvider : IPreviewProvider {
    @Composable
    override fun ProvidePreview(
        normalizedPosition: Float,
        scrollbarBounds: ScrollbarBounds,
        enableGlassmorphism: Boolean
    ) {
        // No preview rendered
    }

    override fun calculatePreviewPosition(
        normalizedPosition: Float,
        scrollbarBounds: ScrollbarBounds,
        previewHeight: Float,
        previewWidth: Float
    ): IntOffset = IntOffset.Zero
}
