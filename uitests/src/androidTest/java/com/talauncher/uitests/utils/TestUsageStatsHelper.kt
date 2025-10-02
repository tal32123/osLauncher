package com.talauncher.uitests.utils

import android.content.Context
import com.talauncher.data.model.AppUsage
import com.talauncher.utils.UsageStatsHelper

/**
 * Mock implementation of UsageStatsHelper for UI tests.
 * Allows controlling default launcher state without requiring actual system settings.
 */
class TestUsageStatsHelper(
    context: Context,
    private var isDefaultLauncherState: Boolean = false
) : UsageStatsHelper(context) {

    override fun isDefaultLauncher(): Boolean {
        return isDefaultLauncherState
    }

    fun setDefaultLauncher(isDefault: Boolean) {
        isDefaultLauncherState = isDefault
    }

    override suspend fun getTodayUsageStats(hasPermission: Boolean): List<AppUsage> {
        return emptyList()
    }

    override suspend fun getPast48HoursUsageStats(hasPermission: Boolean): List<AppUsage> {
        return emptyList()
    }

    override suspend fun getTopUsedApps(hasPermission: Boolean, limit: Int): List<AppUsage> {
        return emptyList()
    }

    override suspend fun getCurrentForegroundApp(hasPermission: Boolean): String? {
        return null
    }
}
