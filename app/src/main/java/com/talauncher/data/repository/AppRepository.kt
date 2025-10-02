package com.talauncher.data.repository

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import com.talauncher.data.database.AppDao
import com.talauncher.data.model.AppInfo
import com.talauncher.data.model.InstalledApp
import com.talauncher.utils.ErrorHandler
import com.talauncher.utils.ErrorUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class AppTimeLimitInfo(
    val minutes: Int,
    val usesDefault: Boolean
)

/**
 * Repository providing app data and launch helpers.
 *
 * @param context Context used for package queries and launching intents. An application context
 * is stored internally so launches work correctly from non-Activity components without holding
 * on to an activity reference.
 */
class AppRepository(
    private val appDao: AppDao,
    context: Context,
    private val settingsRepository: SettingsRepository,
    private val sessionRepository: SessionRepository? = null,
    private val errorHandler: ErrorHandler? = null
) {
    private val applicationContext: Context = context.applicationContext
    companion object {
        private const val TAG = "AppRepository"
    }
    fun getAllApps(): Flow<List<AppInfo>> = appDao.getAllApps()

    fun getAllVisibleApps(): Flow<List<AppInfo>> = appDao.getAllVisibleApps()


    fun getHiddenApps(): Flow<List<AppInfo>> = appDao.getHiddenApps()

    fun getDistractingApps(): Flow<List<AppInfo>> = appDao.getDistractingApps()

    suspend fun getApp(packageName: String): AppInfo? = appDao.getApp(packageName)

    suspend fun insertApp(app: AppInfo) = appDao.insertApp(app)


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

    suspend fun renameApp(packageName: String, newName: String) {
        val trimmedName = newName.trim()
        if (trimmedName.isEmpty()) {
            return
        }

        try {
            val existingApp = getApp(packageName)
            if (existingApp != null) {
                if (existingApp.appName == trimmedName) {
                    return
                }

                val updatedApp = existingApp.copy(appName = trimmedName)
                appDao.updateApp(updatedApp)
            } else {
                val baseInfo = getAppInfoFromPackage(packageName)
                if (baseInfo != null) {
                    insertApp(baseInfo.copy(appName = trimmedName))
                } else {
                    insertApp(AppInfo(packageName = packageName, appName = trimmedName))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error renaming app: $packageName", e)
            errorHandler?.showError(
                "Rename App Error",
                "Failed to rename app: ${e.localizedMessage ?: e.message ?: "Unknown error"}",
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
            val packageManager = applicationContext.packageManager
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
            val packageManager = applicationContext.packageManager
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

    suspend fun launchApp(packageName: String, plannedDuration: Int? = null): Boolean {
        try {
            if (plannedDuration != null && plannedDuration > 0 && sessionRepository != null) {
                sessionRepository.startSession(packageName, plannedDuration)
            }

            if (packageName == "android.settings") {
                // Special case for Device Settings
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                applicationContext.startActivity(intent)
            } else {
                val intent = applicationContext.packageManager.getLaunchIntentForPackage(packageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    applicationContext.startActivity(intent)
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

    suspend fun shouldShowTimeLimitPrompt(packageName: String): Boolean = withContext(Dispatchers.IO) {
        val settings = settingsRepository.getSettingsSync()
        if (!settings.enableTimeLimitPrompt) {
            return@withContext false
        }

        val app = getApp(packageName)
        return@withContext app?.isDistracting == true || app?.isHidden == true
    }

    suspend fun getAppTimeLimitInfo(packageName: String): AppTimeLimitInfo = withContext(Dispatchers.IO) {
        val settings = settingsRepository.getSettingsSync()
        val app = appDao.getApp(packageName)
        val override = app?.timeLimitMinutes
        val minutes = override ?: settings.defaultTimeLimitMinutes
        AppTimeLimitInfo(minutes = minutes, usesDefault = override == null)
    }

    suspend fun updateAppTimeLimit(packageName: String, minutes: Int?) = withContext(Dispatchers.IO) {
        val settings = settingsRepository.getSettingsSync()
        val sanitized = minutes?.coerceIn(5, 480)
        val effectiveValue = sanitized?.takeIf { it != settings.defaultTimeLimitMinutes }

        val existingApp = appDao.getApp(packageName)
        if (existingApp != null) {
            appDao.updateTimeLimit(packageName, effectiveValue)
        } else {
            val baseInfo = getAppInfoFromPackage(packageName)
            if (baseInfo != null) {
                insertApp(
                    baseInfo.copy(timeLimitMinutes = effectiveValue)
                )
            }
        }
    }

    suspend fun getActiveSessionForApp(packageName: String) = sessionRepository?.getActiveSessionForApp(packageName)

    fun closeCurrentApp() {
        try {
            // Move the current app to background by launching the home screen
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            applicationContext.startActivity(intent)
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

    suspend fun getAllAppsSync(): List<AppInfo> = appDao.getAllAppsSync()

    suspend fun syncInstalledApps() = withContext(Dispatchers.IO) {
        try {
            // Fetch data in parallel for better performance
            val installedAppsDeferred = async { getInstalledApps() }
            val existingAppsDeferred = async { getAllAppsSync() }

            val installedApps = installedAppsDeferred.await()
            val existingApps = existingAppsDeferred.await()

            // Create optimized lookups
            val existingAppMap = existingApps.associateBy { it.packageName }
            val installedPackages = installedApps.mapTo(mutableSetOf()) { it.packageName }

            // Process in chunks to avoid large memory allocations
            val chunkSize = 50
            val appsToInsert = mutableListOf<AppInfo>()
            val appsToDelete = mutableListOf<AppInfo>()

            // Collect new apps in chunks
            installedApps.chunked(chunkSize).forEach { chunk ->
                chunk.forEach { installedApp ->
                    if (!existingAppMap.containsKey(installedApp.packageName)) {
                        appsToInsert.add(
                            AppInfo(
                                packageName = installedApp.packageName,
                                appName = installedApp.appName
                            )
                        )
                    }
                }
            }

            // Add Device Settings entry if it doesn't exist
            val deviceSettingsPackage = "android.settings"
            if (!existingAppMap.containsKey(deviceSettingsPackage)) {
                appsToInsert.add(
                    AppInfo(
                        packageName = deviceSettingsPackage,
                        appName = "Device Settings"
                    )
                )
            }

            // Collect apps that are no longer installed
            val packagesToKeep = installedPackages + deviceSettingsPackage
            existingApps.chunked(chunkSize).forEach { chunk ->
                chunk.forEach { storedApp ->
                    if (!packagesToKeep.contains(storedApp.packageName)) {
                        appsToDelete.add(storedApp)
                    }
                }
            }

            // Perform batch operations with chunking for large datasets
            if (appsToInsert.isNotEmpty()) {
                appsToInsert.chunked(chunkSize).forEach { chunk ->
                    appDao.insertApps(chunk)
                }
            }
            if (appsToDelete.isNotEmpty()) {
                appsToDelete.chunked(chunkSize).forEach { chunk ->
                    appDao.deleteApps(chunk)
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

}

