package com.talauncher.ui.components.scrollbar.preview

import com.talauncher.data.model.AppInfo

/**
 * Preview content data for displaying app information.
 *
 * This data class encapsulates all information needed to render an app preview,
 * following the **Single Responsibility Principle** by managing only preview content data.
 *
 * @param appInfo The app information to display
 * @param currentPosition Current position in the list (1-based index)
 * @param totalItems Total number of items in the list
 * @param previewText Formatted preview text (e.g., "25 / 100")
 */
data class PreviewContent(
    val appInfo: AppInfo,
    val currentPosition: Int,
    val totalItems: Int,
    val previewText: String
) {
    init {
        require(currentPosition >= 1) { "Current position must be at least 1" }
        require(totalItems >= 1) { "Total items must be at least 1" }
        require(currentPosition <= totalItems) { "Current position cannot exceed total items" }
    }
}

/**
 * Interface for providing preview content based on scroll position.
 *
 * This interface follows the **Dependency Inversion Principle** by abstracting
 * content retrieval. Different data sources can provide content without changing
 * the preview UI implementation.
 *
 * Architecture pattern: **Strategy Pattern**
 * Different content retrieval strategies can be implemented (list-based, database-based, etc.).
 */
interface IPreviewContentProvider {
    /**
     * Retrieves preview content at the given normalized scroll position.
     *
     * @param normalizedPosition Current scroll position (0f to 1f)
     * @return PreviewContent if available, null if position is invalid or no content
     */
    fun getContentAt(normalizedPosition: Float): PreviewContent?

    /**
     * Retrieves preview content at a specific item index.
     *
     * @param index Item index (0-based)
     * @return PreviewContent if available, null if index is out of bounds
     */
    fun getContentAtIndex(index: Int): PreviewContent?

    /**
     * Gets the total number of items available.
     *
     * @return Total item count
     */
    fun getTotalItems(): Int
}

/**
 * Implementation of preview content provider for app lists.
 *
 * This class follows **Single Responsibility Principle** by handling only
 * content retrieval and formatting for app preview.
 *
 * Benefits:
 * - **Separation of Concerns**: Content logic separated from UI rendering
 * - **Testability**: Pure data operations, easy to test
 * - **Flexibility**: Can be extended for different list types
 *
 * @param appList List of apps to provide preview content for
 * @param formatProvider Optional custom format provider for preview text
 */
class AppPreviewContentProvider(
    private val appList: List<AppInfo>,
    private val formatProvider: IPreviewTextFormatter = DefaultPreviewTextFormatter
) : IPreviewContentProvider {

    override fun getContentAt(normalizedPosition: Float): PreviewContent? {
        require(normalizedPosition in 0f..1f) { "Normalized position must be between 0 and 1" }

        if (appList.isEmpty()) return null

        // Calculate index from normalized position
        val index = calculateIndexFromPosition(normalizedPosition)

        return getContentAtIndex(index)
    }

    override fun getContentAtIndex(index: Int): PreviewContent? {
        if (index < 0 || index >= appList.size) return null

        val appInfo = appList.getOrNull(index) ?: return null
        val position = index + 1 // Convert to 1-based for display
        val total = appList.size

        val previewText = formatProvider.formatPreviewText(position, total)

        return PreviewContent(
            appInfo = appInfo,
            currentPosition = position,
            totalItems = total,
            previewText = previewText
        )
    }

    override fun getTotalItems(): Int = appList.size

    /**
     * Calculates list index from normalized scroll position.
     *
     * Maps 0f..1f range to 0..size-1 range with proper rounding.
     *
     * @param normalizedPosition Scroll position (0f to 1f)
     * @return List index
     */
    private fun calculateIndexFromPosition(normalizedPosition: Float): Int {
        if (appList.isEmpty()) return 0

        // Map normalized position to index range
        val exactIndex = normalizedPosition * (appList.size - 1)

        // Round to nearest index
        return exactIndex.toInt().coerceIn(0, appList.size - 1)
    }
}

/**
 * Interface for formatting preview text.
 *
 * This interface follows the **Interface Segregation Principle** by providing
 * a focused interface for text formatting only.
 *
 * Allows different formatting styles (e.g., "25/100", "25 of 100", "Page 25") without
 * changing the content provider.
 */
interface IPreviewTextFormatter {
    /**
     * Formats preview text for the given position and total.
     *
     * @param currentPosition Current position (1-based)
     * @param totalItems Total number of items
     * @return Formatted preview text
     */
    fun formatPreviewText(currentPosition: Int, totalItems: Int): String
}

/**
 * Default preview text formatter.
 *
 * Formats preview text in the standard "X / Y" format.
 * Example: "25 / 100"
 */
object DefaultPreviewTextFormatter : IPreviewTextFormatter {
    override fun formatPreviewText(currentPosition: Int, totalItems: Int): String {
        return "$currentPosition / $totalItems"
    }
}

/**
 * Compact preview text formatter.
 *
 * Formats preview text in compact "X/Y" format without spaces.
 * Example: "25/100"
 */
object CompactPreviewTextFormatter : IPreviewTextFormatter {
    override fun formatPreviewText(currentPosition: Int, totalItems: Int): String {
        return "$currentPosition/$totalItems"
    }
}

/**
 * Verbose preview text formatter.
 *
 * Formats preview text in verbose "X of Y" format.
 * Example: "25 of 100"
 */
object VerbosePreviewTextFormatter : IPreviewTextFormatter {
    override fun formatPreviewText(currentPosition: Int, totalItems: Int): String {
        return "$currentPosition of $totalItems"
    }
}

/**
 * Empty content provider for when no preview is needed.
 *
 * This follows the **Null Object Pattern**, providing a safe default
 * that returns null for all operations.
 */
object EmptyPreviewContentProvider : IPreviewContentProvider {
    override fun getContentAt(normalizedPosition: Float): PreviewContent? = null
    override fun getContentAtIndex(index: Int): PreviewContent? = null
    override fun getTotalItems(): Int = 0
}

/**
 * Extension function to create a content provider from an app list.
 *
 * Convenience method following Kotlin's extension function pattern for better API ergonomics.
 *
 * @param formatter Optional text formatter (defaults to standard format)
 * @return AppPreviewContentProvider instance
 */
fun List<AppInfo>.toPreviewContentProvider(
    formatter: IPreviewTextFormatter = DefaultPreviewTextFormatter
): IPreviewContentProvider {
    return AppPreviewContentProvider(this, formatter)
}
