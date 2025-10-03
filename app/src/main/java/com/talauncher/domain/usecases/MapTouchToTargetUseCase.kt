package com.talauncher.domain.usecases

import com.talauncher.domain.model.Section
import com.talauncher.domain.model.SectionIndex
import kotlin.math.floor

/**
 * Result of mapping a touch position to a target in the list.
 *
 * Architecture:
 * - Immutable value object
 * - Contains all information needed to scroll and display preview
 *
 * @property sectionIndex Index of the section in the SectionIndex
 * @property section The section data
 * @property intraOffset Offset within the section [0..section.count-1]
 * @property globalIndex Global position in the app list
 * @property appName Name of the app at this position (for preview bubble)
 */
data class TouchTarget(
    val sectionIndex: Int,
    val section: Section,
    val intraOffset: Int,
    val globalIndex: Int,
    val appName: String
)

/**
 * Use case for mapping touch Y position to a specific app target.
 *
 * Architecture:
 * - Follows Single Responsibility Principle (only maps touch to target)
 * - Pure function with no side effects (functional approach)
 * - Stateless and easily testable
 * - O(1) computation using prefix sums
 *
 * Design Pattern: Use Case pattern (Clean Architecture)
 *
 * SOLID Principles:
 * - Single Responsibility: Only performs touch-to-target mapping
 * - Open/Closed: Logic can be extended without modification
 * - Dependency Inversion: Works with abstract SectionIndex model
 *
 * Performance:
 * - O(1) computation using precomputed cumulative arrays
 * - Zero allocations (reuses existing data structures)
 * - Suitable for 60fps touch tracking
 *
 * Algorithm:
 * 1. Normalize touch Y to [0..1] based on rail height and padding
 * 2. Map normalized Y to section index with clamping
 * 3. Compute intra-section fraction from touch position
 * 4. Convert fraction to intraOffset = floor(fraction * sectionCount)
 * 5. Compute globalIndex = cumulative[section] + intraOffset
 */
class MapTouchToTargetUseCase {

    /**
     * Maps touch Y position to a specific app target with per-app precision.
     *
     * @param touchY Touch Y coordinate in pixels
     * @param railHeight Total height of the rail in pixels
     * @param railTopPadding Top padding of the rail content in pixels
     * @param railBottomPadding Bottom padding of the rail content in pixels
     * @param sectionIndex The precomputed section index
     * @return TouchTarget containing all mapped data, or null if mapping failed
     */
    fun execute(
        touchY: Float,
        railHeight: Float,
        railTopPadding: Float,
        railBottomPadding: Float,
        sectionIndex: SectionIndex
    ): TouchTarget? {
        // Early return for empty or invalid state
        if (sectionIndex.isEmpty || railHeight <= 0f) {
            return null
        }

        // Step 1: Normalize touch Y to [0..1] accounting for padding
        val contentHeight = railHeight - railTopPadding - railBottomPadding
        if (contentHeight <= 0f) {
            return null
        }

        val normalizedY = ((touchY - railTopPadding) / contentHeight).coerceIn(0f, 1f)

        // Step 2: Map normalized Y to section index
        val sections = sectionIndex.sections
        val totalSections = sections.size

        // Compute which section the touch falls into
        val sectionIndexFloat = normalizedY * totalSections
        val targetSectionIndex = sectionIndexFloat.toInt().coerceIn(0, totalSections - 1)

        val section = sections[targetSectionIndex]

        // If section has no apps, return the first position
        if (section.count == 0) {
            return TouchTarget(
                sectionIndex = targetSectionIndex,
                section = section,
                intraOffset = 0,
                globalIndex = section.firstPosition,
                appName = section.appNames.firstOrNull() ?: ""
            )
        }

        // Step 3: Compute intra-section fraction
        // Each section occupies a fractional portion: 1.0 / totalSections
        val sectionFraction = 1.0f / totalSections
        val sectionStart = targetSectionIndex * sectionFraction
        val sectionEnd = sectionStart + sectionFraction

        // Local position within this section's range [0..1]
        val intraSectionFraction = if (sectionFraction > 0f) {
            ((normalizedY - sectionStart) / sectionFraction).coerceIn(0f, 1f)
        } else {
            0f
        }

        // Step 4: Convert fraction to intraOffset
        // For a section with N apps, we want indices [0..N-1]
        val intraOffset = floor(intraSectionFraction * section.count).toInt()
            .coerceIn(0, section.count - 1)

        // Step 5: Compute global index using prefix sums
        val globalIndex = sectionIndex.computeGlobalIndex(targetSectionIndex, intraOffset)
            ?: return null

        // Get app name for preview (with bounds checking)
        val appName = section.appNames.getOrNull(intraOffset) ?: section.appNames.firstOrNull() ?: ""

        return TouchTarget(
            sectionIndex = targetSectionIndex,
            section = section,
            intraOffset = intraOffset,
            globalIndex = globalIndex,
            appName = appName
        )
    }

    /**
     * Simplified version that maps to the first app in a section.
     * Used for tap-to-jump behavior.
     *
     * @param sectionKey The section key (e.g., "A", "B", "#")
     * @param sectionIndex The precomputed section index
     * @return TouchTarget for the first app in the section, or null if not found
     */
    fun executeForSectionStart(
        sectionKey: String,
        sectionIndex: SectionIndex
    ): TouchTarget? {
        val targetSectionIndex = sectionIndex.sectionKeyToIndex[sectionKey] ?: return null
        val section = sectionIndex.sections[targetSectionIndex]

        if (section.count == 0) {
            return null
        }

        val globalIndex = section.firstPosition
        val appName = section.appNames.firstOrNull() ?: ""

        return TouchTarget(
            sectionIndex = targetSectionIndex,
            section = section,
            intraOffset = 0,
            globalIndex = globalIndex,
            appName = appName
        )
    }
}
