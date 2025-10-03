package com.talauncher.domain.model

/**
 * Constant for the "Recent Apps" index key.
 */
const val RECENT_APPS_INDEX_KEY = "*"

/**
 * Constant for the "Pinned Apps" index key.
 */
const val PINNED_APPS_INDEX_KEY = "📌"

/**
 * Represents a single entry in the alphabet index.
 *
 * Architecture:
 * - Domain model following Clean Architecture principles
 * - Immutable data class for thread safety
 * - Part of domain layer (independent of UI framework)
 *
 * @property key Unique identifier for this entry (e.g., "A", "B", "#", "*" for recent)
 * @property displayLabel Text to display in the index
 * @property targetIndex Index in the list to scroll to (null if no apps for this letter)
 * @property hasApps Whether this entry has any apps
 * @property previewAppName Optional app name for preview (future enhancement)
 */
data class AlphabetIndexEntry(
    val key: String,
    val displayLabel: String,
    val targetIndex: Int?,
    val hasApps: Boolean,
    val previewAppName: String? = null
)

/**
 * Represents a single section in the alphabet index with full metadata.
 *
 * Architecture:
 * - Immutable domain model for thread safety
 * - Encapsulates all data needed for a section bucket
 * - Part of domain layer (framework-independent)
 *
 * SOLID Principles:
 * - Single Responsibility: Represents section data structure only
 * - Open/Closed: Can be extended with additional properties without modification
 *
 * @property key Unique identifier for this section (e.g., "A", "B", "#", "*")
 * @property displayLabel Text to display in UI
 * @property firstPosition Starting adapter index for this section
 * @property count Number of items in this section
 * @property appNames List of app names in this section (for preview)
 */
data class Section(
    val key: String,
    val displayLabel: String,
    val firstPosition: Int,
    val count: Int,
    val appNames: List<String> = emptyList()
)

/**
 * Immutable index structure with precomputed prefix sums for O(1) touch-to-app mapping.
 *
 * Architecture:
 * - Domain model following Clean Architecture and SOLID principles
 * - Completely immutable and thread-safe
 * - All data precomputed at construction time (no runtime allocations)
 * - Enables O(1) mapping from (section, intraOffset) → global app index
 *
 * Design Patterns:
 * - Value Object: Immutable data structure with no identity
 * - Builder pattern supported via factory methods
 *
 * SOLID Principles:
 * - Single Responsibility: Only holds section index data and structure
 * - Open/Closed: Can be extended with additional computed properties
 * - Dependency Inversion: Abstract data structure, not tied to UI
 *
 * Performance:
 * - O(1) section lookup and global index computation
 * - Zero allocations during touch mapping operations
 * - Prefix sums enable instant cumulative position calculation
 *
 * @property sections Ordered list of sections (buckets) in display order
 * @property totalCount Total number of apps across all sections
 */
data class SectionIndex(
    val sections: List<Section>,
    val totalCount: Int
) {
    /**
     * Cached cumulative prefix sums for O(1) global index lookup.
     * cumulative[i] = sum of counts for sections [0..i-1]
     * This allows computing globalIndex = cumulative[section] + intraOffset
     */
    val cumulative: List<Int> = buildCumulativeArray()

    /**
     * Maps section keys to their indices for fast lookup.
     */
    val sectionKeyToIndex: Map<String, Int> = sections
        .mapIndexed { index, section -> section.key to index }
        .toMap()

    /**
     * Builds prefix sum array for O(1) global index computation.
     *
     * Performance: O(n) construction, O(1) lookup
     * Memory: O(n) where n = number of sections
     */
    private fun buildCumulativeArray(): List<Int> {
        val result = mutableListOf(0) // First section starts at 0
        var sum = 0
        sections.forEachIndexed { index, section ->
            if (index > 0) {
                result.add(sum)
            }
            sum += section.count
        }
        return result
    }

    /**
     * Gets the section at the specified index, or null if out of bounds.
     * Thread-safe, O(1) operation.
     */
    fun getSectionAt(index: Int): Section? =
        sections.getOrNull(index)

    /**
     * Gets the section with the specified key, or null if not found.
     * Thread-safe, O(1) operation via map lookup.
     */
    fun getSectionByKey(key: String): Section? =
        sectionKeyToIndex[key]?.let { sections[it] }

    /**
     * Computes the global app index from section index and intra-section offset.
     *
     * Performance: O(1) computation using prefix sums
     * Thread-safe: No mutations, pure function
     *
     * @param sectionIndex Index of the section
     * @param intraOffset Offset within the section [0..section.count-1]
     * @return Global app list index, or null if inputs are invalid
     */
    fun computeGlobalIndex(sectionIndex: Int, intraOffset: Int): Int? {
        if (sectionIndex !in sections.indices) return null
        val section = sections[sectionIndex]
        if (intraOffset !in 0 until section.count) return null
        return cumulative[sectionIndex] + intraOffset
    }

    /**
     * Returns true if the index is empty (no sections or no apps).
     */
    val isEmpty: Boolean
        get() = sections.isEmpty() || totalCount == 0

    companion object {
        /**
         * Empty index instance for initialization and empty states.
         */
        val EMPTY = SectionIndex(emptyList(), 0)
    }
}
