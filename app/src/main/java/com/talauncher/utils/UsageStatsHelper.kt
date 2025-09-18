package com.talauncher.utils

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import com.talauncher.data.model.AppUsage
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UsageStatsHelper(
    private val context: Context,
    private val permissionsHelper: PermissionsHelper
) {

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

    suspend fun getTodayUsageStats(): List<AppUsage> = withContext(Dispatchers.IO) {
        if (!permissionsHelper.hasUsageStatsPermission()) {
            return@withContext emptyList()
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

        usageStats?.mapNotNull { stats ->
            if (stats.totalTimeInForeground > 0) {
                AppUsage(
                    packageName = stats.packageName,
                    timeInForeground = stats.totalTimeInForeground
                )
            } else null
        } ?: emptyList()
    }

    suspend fun getPast48HoursUsageStats(): List<AppUsage> = withContext(Dispatchers.IO) {
        if (!permissionsHelper.hasUsageStatsPermission()) {
            return@withContext emptyList()
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

        packageUsageMap.map { (packageName, totalTime) ->
            AppUsage(
                packageName = packageName,
                timeInForeground = totalTime
            )
        }
    }

    suspend fun getTopUsedApps(limit: Int = 5): List<AppUsage> {
        return getPast48HoursUsageStats()
            .sortedByDescending { it.timeInForeground }
            .take(limit)
            .filter { it.timeInForeground > 0 }
    }

    fun isDefaultLauncher(): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val resolveInfo = context.packageManager.resolveActivity(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            resolveInfo?.activityInfo?.packageName == context.packageName
        } catch (e: Exception) {
            false
        }
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

    suspend fun getCurrentForegroundApp(): String? = withContext(Dispatchers.IO) {
        if (!permissionsHelper.hasUsageStatsPermission()) {
            return@withContext null
        }

        try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val endTime = System.currentTimeMillis()
            val startTime = endTime - (60 * 1000L) // Look back 1 minute

            val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
            var currentApp: String? = null

            if (usageEvents != null) {
                val event = UsageEvents.Event()
                while (usageEvents.hasNextEvent()) {
                    usageEvents.getNextEvent(event)

                    when (event.eventType) {
                        UsageEvents.Event.ACTIVITY_RESUMED -> {
                            currentApp = event.packageName
                        }
                        UsageEvents.Event.ACTIVITY_PAUSED -> {
                            if (currentApp == event.packageName) {
                                currentApp = null
                            }
                        }
                    }
                }
            }

            return@withContext currentApp
        } catch (e: Exception) {
            return@withContext null
        }
    }
}