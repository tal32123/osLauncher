package com.talauncher.infrastructure.fastscroll

import com.talauncher.domain.fastscroll.CollationProvider
import java.text.Collator
import java.util.Locale

/**
 * Default implementation of locale-aware collation and bucketing.
 *
 * Architecture:
 * - Implements CollationProvider interface (Dependency Inversion)
 * - Uses Java Collator for locale-aware sorting
 * - Provides consistent bucketing rules for A-Z and #
 *
 * SOLID Principles:
 * - Single Responsibility: Only handles collation and bucketing
 * - Dependency Inversion: Implements abstract CollationProvider interface
 * - Open/Closed: Can be extended for locale-specific rules
 *
 * Performance:
 * - Reuses Collator instance (not created per call)
 * - Efficient string comparisons using native collation
 *
 * Locale Support:
 * - Respects current system locale
 * - Can be extended for locale-specific bucket rules
 *
 * @param locale The locale to use for collation (defaults to system locale)
 */
class CollationProviderImpl(
    locale: Locale = Locale.getDefault()
) : CollationProvider {

    private val collator: Collator = Collator.getInstance(locale).apply {
        strength = Collator.PRIMARY // Ignore case and accents for base comparison
    }

    /**
     * Sorts a list of items by name using locale-aware collation.
     * Maintains stable sort (preserves original order for equal elements).
     *
     * Performance: O(n log n) where n = items.size
     *
     * @param items List of items to sort
     * @param getName Function to extract the name from each item
     * @return Sorted list
     */
    override fun <T> sortByName(items: List<T>, getName: (T) -> String): List<T> {
        return items.sortedWith { a, b ->
            collator.compare(getName(a), getName(b))
        }
    }

    /**
     * Determines the bucket key for a given app name.
     * Returns "A"-"Z" for letters, "#" for non-alphabetic characters.
     *
     * Rules:
     * - First character is a letter (A-Z, case-insensitive) → return uppercase letter
     * - First character is a digit, symbol, or emoji → return "#"
     * - Empty or null → return "#"
     *
     * Performance: O(1) operation
     *
     * @param appName The app name to bucket
     * @return Bucket key (e.g., "A", "B", "#")
     */
    override fun getBucketKey(appName: String): String {
        val firstChar = appName.firstOrNull()?.toString()?.uppercase() ?: return "#"
        return when {
            firstChar.matches(Regex("[A-Z]")) -> firstChar
            else -> "#"
        }
    }

    /**
     * Gets the display label for a bucket key.
     * Currently returns the key as-is, but can be extended for locale-specific labels.
     *
     * @param bucketKey The bucket key
     * @return Display label
     */
    override fun getDisplayLabel(bucketKey: String): String {
        return bucketKey
    }
}
