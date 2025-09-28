package com.talauncher.utils

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.annotation.VisibleForTesting
import com.talauncher.data.model.AppUsage
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

open class UsageStatsHelper(
    private val context: Context
) {
    companion object {
        private const val CACHE_DURATION_MS = 5 * 60 * 1000L // 5 minutes
    }

    private var cachedTodayUsage: List<AppUsage>? = null
    private var cachedTodayUsageTime: Long = 0L
    private var cachedPast48HoursUsage: List<AppUsage>? = null
    private var cachedPast48HoursUsageTime: Long = 0L
    @Volatile
    private var defaultLauncherOverride: Boolean? = null

    suspend fun getTodayUsageStats(hasPermission: Boolean): List<AppUsage> = withContext(Dispatchers.IO) {
        if (!hasPermission) {
            return@withContext emptyList()
        }

        val currentTime = System.currentTimeMillis()

        // Check if cache is still valid
        if (cachedTodayUsage != null && (currentTime - cachedTodayUsageTime) < CACHE_DURATION_MS) {
            return@withContext cachedTodayUsage!!
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

        val result = usageStats?.mapNotNull { stats ->
            if (stats.totalTimeInForeground > 0) {
                AppUsage(
                    packageName = stats.packageName,
                    timeInForeground = stats.totalTimeInForeground,
                    lastTimeUsed = stats.lastTimeUsed
                )
            } else null
        } ?: emptyList()

        // Cache the result
        cachedTodayUsage = result
        cachedTodayUsageTime = currentTime

        result
    }

    suspend fun getPast48HoursUsageStats(hasPermission: Boolean): List<AppUsage> = withContext(Dispatchers.IO) {
        if (!hasPermission) {
            return@withContext emptyList()
        }

        val currentTime = System.currentTimeMillis()

        // Check if cache is still valid
        if (cachedPast48HoursUsage != null && (currentTime - cachedPast48HoursUsageTime) < CACHE_DURATION_MS) {
            return@withContext cachedPast48HoursUsage!!
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
        val packageLastUsedMap = mutableMapOf<String, Long>()

        usageStats?.forEach { stats ->
            if (stats.totalTimeInForeground > 0) {
                packageUsageMap[stats.packageName] =
                    (packageUsageMap[stats.packageName] ?: 0) + stats.totalTimeInForeground
                packageLastUsedMap[stats.packageName] =
                    maxOf(packageLastUsedMap[stats.packageName] ?: 0, stats.lastTimeUsed)
            }
        }

        val result = packageUsageMap.map { (packageName, totalTime) ->
            AppUsage(
                packageName = packageName,
                timeInForeground = totalTime,
                lastTimeUsed = packageLastUsedMap[packageName] ?: 0
            )
        }

        // Cache the result
        cachedPast48HoursUsage = result
        cachedPast48HoursUsageTime = currentTime

        result
    }

    suspend fun getTopUsedApps(hasPermission: Boolean, limit: Int = 5): List<AppUsage> {
        return getPast48HoursUsageStats(hasPermission)
            .sortedByDescending { it.timeInForeground }
            .take(limit)
            .filter { it.timeInForeground > 0 }
    }

    open fun isDefaultLauncher(): Boolean {
        defaultLauncherOverride?.let { return it }
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

    @VisibleForTesting
    fun overrideIsDefaultLauncher(isDefaultLauncher: Boolean?) {
        defaultLauncherOverride = isDefaultLauncher
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

    suspend fun getCurrentForegroundApp(hasPermission: Boolean): String? = withContext(Dispatchers.IO) {
        if (!hasPermission) {
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
