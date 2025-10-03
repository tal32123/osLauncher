package com.talauncher.domain.usecases

import com.talauncher.data.model.AppInfo
import com.talauncher.data.model.AppUsage
import com.talauncher.utils.UsageStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Use case for retrieving recently used apps.
 *
 * Architecture:
 * - Follows Single Responsibility Principle (only handles recent apps logic)
 * - Depends on abstractions (UsageStatsHelper interface)
 * - Implements proper coroutine context switching
 *
 * Design Pattern: Use Case pattern (Clean Architecture)
 * SOLID:
 * - Single Responsibility: Only retrieves and filters recent apps
 * - Open/Closed: Can be extended with different sorting strategies
 * - Dependency Inversion: Depends on UsageStatsHelper abstraction
 *
 * @param usageStatsHelper Helper for accessing usage statistics
 */
class GetRecentAppsUseCase(
    private val usageStatsHelper: UsageStatsHelper?
) {
    /**
     * Gets the most recently used apps.
     *
     * Algorithm:
     * 1. Check if usage stats permission is granted
     * 2. Retrieve usage statistics from past 48 hours
     * 3. Filter to visible apps only (exclude hidden apps)
     * 4. Sort by usage time (most used first)
     * 5. Take up to the specified limit
     * 6. Fill remaining slots with alphabetically sorted apps if needed
     *
     * @param visibleApps List of apps that are not hidden
     * @param hiddenApps List of apps that are hidden
     * @param limit Maximum number of recent apps to return
     * @param hasPermission Whether usage stats permission is granted
     * @return List of recent apps (may be empty if no permission or no usage data)
     */
    suspend fun execute(
        visibleApps: List<AppInfo>,
        hiddenApps: List<AppInfo>,
        limit: Int,
        hasPermission: Boolean
    ): List<AppInfo> = withContext(Dispatchers.Default) {
        // Early return if conditions not met
        if (!hasPermission || usageStatsHelper == null) {
            return@withContext emptyList()
        }

        val sanitizedLimit = limit.coerceAtLeast(0)
        if (sanitizedLimit == 0) {
            return@withContext emptyList()
        }

        // Get usage statistics
        val usageStats = try {
            usageStatsHelper
                .getPast48HoursUsageStats(hasPermission)
                .filter { it.timeInForeground > 0 }
                .sortedByDescending { it.timeInForeground }
        } catch (e: Exception) {
            // If there's any error getting usage stats, return empty list
            return@withContext emptyList()
        }

        // Build lookup structures for efficient filtering
        val hiddenPackages = hiddenApps.mapTo(mutableSetOf()) { it.packageName }
        val appMap = visibleApps.associateBy { it.packageName }

        // Collect recent apps from usage stats
        val recentApps = collectRecentAppsFromUsage(
            usageStats = usageStats,
            appMap = appMap,
            hiddenPackages = hiddenPackages,
            limit = sanitizedLimit
        )

        // Fill remaining slots with alphabetically sorted apps if needed
        if (recentApps.size < sanitizedLimit) {
            fillRemainingSlots(
                recentApps = recentApps,
                visibleApps = visibleApps,
                hiddenPackages = hiddenPackages,
                limit = sanitizedLimit
            )
        } else {
            recentApps
        }
    }

    /**
     * Collects recent apps from usage statistics.
     */
    private fun collectRecentAppsFromUsage(
        usageStats: List<AppUsage>,
        appMap: Map<String, AppInfo>,
        hiddenPackages: Set<String>,
        limit: Int
    ): MutableList<AppInfo> {
        val recentApps = mutableListOf<AppInfo>()
        val seenPackages = mutableSetOf<String>()

        for (usageApp in usageStats) {
            // Skip hidden apps
            if (usageApp.packageName in hiddenPackages) {
                continue
            }

            // Get app info, skip if not in visible apps
            val app = appMap[usageApp.packageName] ?: continue

            // Skip duplicates
            if (!seenPackages.add(app.packageName)) {
                continue
            }

            recentApps += app

            if (recentApps.size == limit) {
                break
            }
        }

        return recentApps
    }

    /**
     * Fills remaining slots with alphabetically sorted apps.
     */
    private fun fillRemainingSlots(
        recentApps: MutableList<AppInfo>,
        visibleApps: List<AppInfo>,
        hiddenPackages: Set<String>,
        limit: Int
    ): List<AppInfo> {
        val seenPackages = recentApps.mapTo(mutableSetOf()) { it.packageName }

        val fallbackApps = visibleApps
            .asSequence()
            .filterNot { it.packageName in hiddenPackages }
            .filterNot { it.packageName in seenPackages }
            .sortedBy { it.appName.lowercase() }
            .toList()

        for (app in fallbackApps) {
            recentApps += app
            if (recentApps.size == limit) {
                break
            }
        }

        return recentApps
    }
}
