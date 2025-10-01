package com.talauncher.domain.usecases

import com.talauncher.data.model.AppInfo
import com.talauncher.utils.SearchScoring
import java.util.Locale

/**
 * Use case for searching and filtering apps.
 *
 * Architecture:
 * - Follows Single Responsibility Principle (only handles app search)
 * - Pure function with no side effects
 * - Stateless and easily testable
 *
 * Design Pattern: Use Case pattern (Clean Architecture)
 * SOLID:
 * - Single Responsibility: Only searches and scores apps
 * - Open/Closed: Can be extended with different scoring algorithms
 * - Dependency Inversion: Depends on SearchScoring utility abstraction
 *
 * This use case implements fuzzy search with relevance scoring for apps.
 */
class SearchAppsUseCase {

    /**
     * Searches apps based on a query string.
     *
     * Algorithm:
     * 1. Normalize and validate query
     * 2. Calculate relevance score for each app
     * 3. Filter apps with score > 0
     * 4. Sort by score (descending) then by name (ascending)
     *
     * @param query Search query string
     * @param apps List of apps to search through
     * @return Filtered and sorted list of apps matching the query
     */
    fun execute(query: String, apps: List<AppInfo>): List<AppInfo> {
        val normalizedQuery = query.trim()

        // Return empty list for blank queries
        if (normalizedQuery.isEmpty()) {
            return emptyList()
        }

        // Score each app and filter by relevance
        return apps.mapNotNull { app ->
            val score = SearchScoring.calculateRelevanceScore(normalizedQuery, app.appName)
            if (score > 0) {
                app to score
            } else {
                null
            }
        }
            // Sort by score (highest first), then alphabetically
            .sortedWith(
                compareByDescending<Pair<AppInfo, Int>> { it.second }
                    .thenBy { it.first.appName.lowercase(Locale.getDefault()) }
            )
            .map { it.first }
    }
}
