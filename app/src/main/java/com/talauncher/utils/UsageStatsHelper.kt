package com.talauncher.utils

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import com.talauncher.data.model.AppUsage
import java.util.*

class UsageStatsHelper(private val context: Context) {

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getTodayUsageStats(): List<AppUsage> {
        if (!hasUsageStatsPermission()) {
            return emptyList()
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        return usageStats?.mapNotNull { stats ->
            if (stats.totalTimeInForeground > 0) {
                AppUsage(
                    packageName = stats.packageName,
                    timeInForeground = stats.totalTimeInForeground
                )
            } else null
        } ?: emptyList()
    }

    fun getPast48HoursUsageStats(): List<AppUsage> {
        if (!hasUsageStatsPermission()) {
            return emptyList()
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - (48 * 60 * 60 * 1000L) // 48 hours ago

        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        // Group by package name and sum usage times across multiple days
        val packageUsageMap = mutableMapOf<String, Long>()

        usageStats?.forEach { stats ->
            if (stats.totalTimeInForeground > 0) {
                packageUsageMap[stats.packageName] =
                    (packageUsageMap[stats.packageName] ?: 0) + stats.totalTimeInForeground
            }
        }

        return packageUsageMap.map { (packageName, totalTime) ->
            AppUsage(
                packageName = packageName,
                timeInForeground = totalTime
            )
        }
    }

    fun getTopUsedApps(limit: Int = 5): List<AppUsage> {
        return getPast48HoursUsageStats()
            .sortedByDescending { it.timeInForeground }
            .take(limit)
            .filter { it.timeInForeground > 0 }
    }

    fun getAppName(packageName: String): String? {
        return try {
            val packageManager = context.packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }
}