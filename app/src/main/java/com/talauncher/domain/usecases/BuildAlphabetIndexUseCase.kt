package com.talauncher.domain.usecases

import com.talauncher.data.model.AppInfo
import com.talauncher.domain.model.AlphabetIndexEntry
import com.talauncher.domain.model.RECENT_APPS_INDEX_KEY

/**
 * Use case for building the alphabet index for app lists.
 *
 * Architecture:
 * - Follows Single Responsibility Principle (only builds alphabet index)
 * - Pure function with no side effects (functional approach)
 * - Stateless and easily testable
 *
 * Design Pattern: Use Case pattern (Clean Architecture)
 * SOLID:
 * - Single Responsibility: Only creates alphabet index structure
 * - Open/Closed: Logic can be extended for different indexing strategies
 * - Dependency Inversion: Works with abstract AppInfo model
 *
 * This use case creates an alphabet index sidebar that helps users quickly
 * navigate through a sorted list of apps.
 */
class BuildAlphabetIndexUseCase {

    /**
     * Builds an alphabet index from a list of apps.
     *
     * The index includes:
     * - A special "Recent Apps" entry (if recent apps exist)
     * - Letters A-Z for apps starting with those letters
     * - A "#" entry for apps starting with non-letters
     *
     * @param apps Sorted list of all apps
     * @param recentApps List of recently used apps
     * @return List of alphabet index entries
     */
    fun execute(apps: List<AppInfo>, recentApps: List<AppInfo>): List<AlphabetIndexEntry> {
        val entries = mutableListOf<AlphabetIndexEntry>()

        // Add "Recent Apps" entry if there are recent apps
        if (recentApps.isNotEmpty()) {
            entries += AlphabetIndexEntry(
                key = RECENT_APPS_INDEX_KEY,
                displayLabel = RECENT_APPS_INDEX_KEY,
                targetIndex = null,
                hasApps = true,
                previewAppName = recentApps.firstOrNull()?.appName
            )
        }

        // Group apps by first character
        val appsByFirstChar = groupAppsByFirstCharacter(apps)

        // Create entries for A-Z and #
        val alphabet = ('A'..'Z').map { it.toString() } + listOf("#")

        alphabet.forEach { char ->
            val appsForChar = appsByFirstChar[char] ?: emptyList()
            entries += AlphabetIndexEntry(
                key = char,
                displayLabel = char,
                targetIndex = if (appsForChar.isNotEmpty()) {
                    findFirstAppIndex(apps, char)
                } else null,
                hasApps = appsForChar.isNotEmpty(),
                previewAppName = appsForChar.firstOrNull()?.appName
            )
        }

        return entries
    }

    /**
     * Groups apps by their first character.
     * Letters are normalized to uppercase, non-letters go to "#".
     */
    private fun groupAppsByFirstCharacter(apps: List<AppInfo>): Map<String, List<AppInfo>> {
        return apps.groupBy { app ->
            val firstChar = app.appName.firstOrNull()?.toString()?.uppercase()
            when {
                firstChar == null -> "#"
                firstChar.matches(Regex("[A-Z]")) -> firstChar
                else -> "#"
            }
        }
    }

    /**
     * Finds the index of the first app starting with the given character.
     *
     * @param apps List of apps
     * @param char Character to search for
     * @return Index of first matching app, or -1 if not found
     */
    private fun findFirstAppIndex(apps: List<AppInfo>, char: String): Int? {
        val index = apps.indexOfFirst { app ->
            val firstChar = app.appName.firstOrNull()?.toString()?.uppercase()
            when {
                char == "#" -> firstChar == null || !firstChar.matches(Regex("[A-Z]"))
                else -> firstChar == char
            }
        }
        return index.takeIf { it != -1 }
    }
}
