package com.talauncher.data.repository

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.talauncher.data.database.AppDao
import com.talauncher.data.model.AppInfo
import com.talauncher.data.model.InstalledApp
import com.talauncher.utils.ErrorHandler
import com.talauncher.utils.ErrorUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository providing app data and launch helpers.
 *
 * @param context Application context used for package queries and launching intents. Pass an
 * application context so launches work correctly from non-Activity components.
 */
class AppRepository(
    private val appDao: AppDao,
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val sessionRepository: SessionRepository? = null,
    private val errorHandler: ErrorHandler? = null
) {
    companion object {
        private const val TAG = "AppRepository"
        private val DISTRACTING_CATEGORIES = setOf(
            ApplicationInfo.CATEGORY_GAME,
            ApplicationInfo.CATEGORY_NEWS,
            ApplicationInfo.CATEGORY_VIDEO,
            ApplicationInfo.CATEGORY_AUDIO,
            ApplicationInfo.CATEGORY_IMAGE,
            ApplicationInfo.CATEGORY_SOCIAL
        )
    }
    fun getAllApps(): Flow<List<AppInfo>> = appDao.getAllApps()

    fun getAllVisibleApps(): Flow<List<AppInfo>> = appDao.getAllVisibleApps()

    fun getPinnedApps(): Flow<List<AppInfo>> = appDao.getPinnedApps()

    fun getHiddenApps(): Flow<List<AppInfo>> = appDao.getHiddenApps()

    fun getDistractingApps(): Flow<List<AppInfo>> = appDao.getDistractingApps()

    suspend fun getApp(packageName: String): AppInfo? = appDao.getApp(packageName)

    suspend fun insertApp(app: AppInfo) = appDao.insertApp(app)

    suspend fun pinApp(packageName: String) {
        try {
            // Get current app or create new one
            val app = getApp(packageName) ?: getAppInfoFromPackage(packageName)
            if (app != null) {
                val maxOrder = appDao.getMaxPinnedOrder() ?: 0
                appDao.updatePinnedStatus(packageName, true, maxOrder + 1)
                if (getApp(packageName) == null) {
                    insertApp(app.copy(isPinned = true, pinnedOrder = maxOrder + 1))
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception pinning app: $packageName", e)
            errorHandler?.showError(
                "Permission Error",
                "Unable to access app information for $packageName. Permission may be required.",
                e
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error pinning app: $packageName", e)
            errorHandler?.showError(
                "Pin App Error",
                "Failed to pin app: ${e.localizedMessage ?: e.message ?: "Unknown error"}",
                e
            )
        }
    }

    suspend fun unpinApp(packageName: String) {
        try {
            appDao.updatePinnedStatus(packageName, false)
        } catch (e: Exception) {
            Log.e(TAG, "Error unpinning app: $packageName", e)
            errorHandler?.showError(
                "Unpin App Error",
                "Failed to unpin app: ${e.localizedMessage ?: e.message ?: "Unknown error"}",
                e
            )
        }
    }

    suspend fun hideApp(packageName: String) {
        try {
            val app = getApp(packageName) ?: getAppInfoFromPackage(packageName)
            if (app != null) {
                appDao.updateHiddenStatus(packageName, true)
                if (getApp(packageName) == null) {
                    insertApp(app.copy(isHidden = true))
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception hiding app: $packageName", e)
            errorHandler?.showError(
                "Permission Error",
                "Unable to access app information for $packageName. Permission may be required.",
                e
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding app: $packageName", e)
            errorHandler?.showError(
                "Hide App Error",
                "Failed to hide app: ${e.localizedMessage ?: e.message ?: "Unknown error"}",
                e
            )
        }
    }

    suspend fun unhideApp(packageName: String) {
        try {
            appDao.updateHiddenStatus(packageName, false)
        } catch (e: Exception) {
            Log.e(TAG, "Error unhiding app: $packageName", e)
            errorHandler?.showError(
                "Unhide App Error",
                "Failed to unhide app: ${e.localizedMessage ?: e.message ?: "Unknown error"}",
                e
            )
        }
    }

    suspend fun updateDistractingStatus(packageName: String, isDistracting: Boolean) {
        try {
            appDao.updateDistractingStatus(packageName, isDistracting)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating distracting status for app: $packageName", e)
            errorHandler?.showError(
                "Update Status Error",
                "Failed to update distracting status: ${e.localizedMessage ?: e.message ?: "Unknown error"}",
                e
            )
        }
    }

    suspend fun getAppDisplayName(packageName: String): String {
        try {
            val storedName = getApp(packageName)?.appName
            if (storedName != null) {
                return storedName
            }
            return getAppInfoFromPackage(packageName)?.appName ?: packageName
        } catch (e: Exception) {
            Log.e(TAG, "Error getting display name for app: $packageName", e)
            return packageName // Fallback to package name
        }
    }

    private suspend fun getAppInfoFromPackage(packageName: String): AppInfo? =
        withContext(Dispatchers.IO) {
            val packageManager = context.packageManager
            try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                val appName = packageManager.getApplicationLabel(appInfo).toString()
                AppInfo(packageName = packageName, appName = appName)
            } catch (e: PackageManager.NameNotFoundException) {
                Log.w(TAG, "Package not found: $packageName")
                null
            } catch (e: SecurityException) {
                Log.e(TAG, "Security exception getting app info: $packageName", e)
                null
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error getting app info: $packageName", e)
                errorHandler?.showError(
                    "App Info Error",
                    "Failed to get information for $packageName: ${e.localizedMessage ?: e.message ?: "Unknown error"}",
                    e
                )
                null
            }
        }

    suspend fun getInstalledApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        try {
            val packageManager = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val activities = packageManager.queryIntentActivities(intent, 0)

            activities.map { resolveInfo ->
                val packageName = resolveInfo.activityInfo.packageName
                val appName = resolveInfo.loadLabel(packageManager).toString()
                val isSystemApp = (resolveInfo.activityInfo.applicationInfo.flags and
                    android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0

                InstalledApp(packageName, appName, isSystemApp)
            }.distinctBy { it.packageName }
                .sortedBy { it.appName }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception getting installed apps", e)
            errorHandler?.showError(
                "Permission Error",
                "Unable to access installed apps. This may require permission to query package information.",
                e
            )
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting installed apps", e)
            errorHandler?.showError(
                "Apps List Error",
                "Failed to get installed apps: ${e.localizedMessage ?: e.message ?: "Unknown error"}",
                e
            )
            emptyList()
        }
    }

    suspend fun launchApp(packageName: String, bypassFriction: Boolean = false, plannedDuration: Int? = null): Boolean {
        try {
            // Check if app is distracting and should show friction barrier
            val app = getApp(packageName)
            val settings = settingsRepository.getSettingsSync()

            val requiresFriction = !bypassFriction && plannedDuration == null && app?.isDistracting == true
            if (requiresFriction) {
                // Return false to indicate friction barrier should be shown
                return false
            }

            // Start session if time limit prompt is enabled and duration is provided
            if (settings.enableTimeLimitPrompt && plannedDuration != null && sessionRepository != null) {
                sessionRepository.startSession(packageName, plannedDuration)
            }

            if (packageName == "android.settings") {
                // Special case for Device Settings
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } else {
                val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } else {
                    Log.w(TAG, "No launch intent found for package: $packageName")
                    errorHandler?.showError(
                        "Launch Error",
                        "Unable to launch $packageName. The app may not be installed or may not have a launch activity.",
                        null
                    )
                    return false
                }
            }
            return true
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "Activity not found for package: $packageName", e)
            errorHandler?.showError(
                "App Not Found",
                "The app $packageName could not be launched. It may have been uninstalled.",
                e
            )
            return false
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception launching app: $packageName", e)
            errorHandler?.showError(
                "Permission Error",
                "Unable to launch $packageName due to permission restrictions.",
                e
            )
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error launching app: $packageName", e)
            errorHandler?.showError(
                "Launch Error",
                "Failed to launch $packageName: ${e.localizedMessage ?: e.message ?: "Unknown error"}",
                e
            )
            return false
        }
    }

    suspend fun shouldShowTimeLimitPrompt(packageName: String): Boolean {
        val settings = settingsRepository.getSettingsSync()
        if (!settings.enableTimeLimitPrompt) {
            return false
        }

        val app = getApp(packageName)
        if (app?.isDistracting == true || app?.isHidden == true) {
            return true
        }

        return isDistractingCategory(packageName)
    }

    suspend fun shouldShowMathChallenge(packageName: String): Boolean {
        val settings = settingsRepository.getSettingsSync()
        val app = getApp(packageName)
        return settings.enableMathChallenge && app?.isDistracting == true
    }

    suspend fun getActiveSessionForApp(packageName: String) = sessionRepository?.getActiveSessionForApp(packageName)

    fun closeCurrentApp() {
        try {
            // Move the current app to background by launching the home screen
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "No home app available to close current app", e)
            errorHandler?.showError(
                "Close App Error",
                "Unable to close the current app. No home screen app is available.",
                e
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception closing current app", e)
            errorHandler?.showError(
                "Permission Error",
                "Unable to close the current app due to permission restrictions.",
                e
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error closing current app", e)
            errorHandler?.showError(
                "Close App Error",
                "Failed to close current app: ${e.localizedMessage ?: e.message ?: "Unknown error"}",
                e
            )
        }
    }

    suspend fun endSessionForApp(packageName: String) = sessionRepository?.endSessionForApp(packageName)

    suspend fun getAllAppsSync(): List<AppInfo> = appDao.getAllAppsSync()

    suspend fun syncInstalledApps() = withContext(Dispatchers.IO) {
        try {
            val installedApps = getInstalledApps()
            val existingApps = getAllAppsSync()

            // Create a map of existing apps for quick lookup
            val existingAppMap = existingApps.associateBy { it.packageName }

            // Add new apps that don't exist in database
            installedApps.forEach { installedApp ->
                try {
                    if (!existingAppMap.containsKey(installedApp.packageName)) {
                        val appInfo = AppInfo(
                            packageName = installedApp.packageName,
                            appName = installedApp.appName
                        )
                        insertApp(appInfo)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error inserting app: ${installedApp.packageName}", e)
                }
            }

            // Add Device Settings entry if it doesn't exist
            val deviceSettingsPackage = "android.settings"
            if (!existingAppMap.containsKey(deviceSettingsPackage)) {
                try {
                    val deviceSettingsApp = AppInfo(
                        packageName = deviceSettingsPackage,
                        appName = "Device Settings"
                    )
                    insertApp(deviceSettingsApp)
                } catch (e: Exception) {
                    Log.e(TAG, "Error inserting device settings app", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing installed apps", e)
            errorHandler?.showError(
                "Sync Apps Error",
                "Failed to sync installed apps: ${e.localizedMessage ?: e.message ?: "Unknown error"}",
                e
            )
        }
    }

    private fun isDistractingCategory(packageName: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return false
        }

        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            val category = appInfo.category
            category != ApplicationInfo.CATEGORY_UNDEFINED && DISTRACTING_CATEGORIES.contains(category)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "Package not found when checking distracting category: $packageName")
            false
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception checking distracting category: $packageName", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error checking distracting category: $packageName", e)
            false
        }
    }
}
