package com.talauncher.data.repository

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.talauncher.data.database.AppDao
import com.talauncher.data.model.AppInfo
import com.talauncher.data.model.InstalledApp
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
    private val sessionRepository: SessionRepository? = null
) {
    fun getAllApps(): Flow<List<AppInfo>> = appDao.getAllApps()

    fun getAllVisibleApps(): Flow<List<AppInfo>> = appDao.getAllVisibleApps()

    fun getPinnedApps(): Flow<List<AppInfo>> = appDao.getPinnedApps()

    fun getHiddenApps(): Flow<List<AppInfo>> = appDao.getHiddenApps()

    fun getDistractingApps(): Flow<List<AppInfo>> = appDao.getDistractingApps()

    suspend fun getApp(packageName: String): AppInfo? = appDao.getApp(packageName)

    suspend fun insertApp(app: AppInfo) = appDao.insertApp(app)

    suspend fun pinApp(packageName: String) {
        // Get current app or create new one
        val app = getApp(packageName) ?: getAppInfoFromPackage(packageName)
        if (app != null) {
            val maxOrder = appDao.getMaxPinnedOrder() ?: 0
            appDao.updatePinnedStatus(packageName, true, maxOrder + 1)
            if (getApp(packageName) == null) {
                insertApp(app.copy(isPinned = true, pinnedOrder = maxOrder + 1))
            }
        }
    }

    suspend fun unpinApp(packageName: String) {
        appDao.updatePinnedStatus(packageName, false)
    }

    suspend fun hideApp(packageName: String) {
        val app = getApp(packageName) ?: getAppInfoFromPackage(packageName)
        if (app != null) {
            appDao.updateHiddenStatus(packageName, true)
            if (getApp(packageName) == null) {
                insertApp(app.copy(isHidden = true))
            }
        }
    }

    suspend fun unhideApp(packageName: String) {
        appDao.updateHiddenStatus(packageName, false)
    }

    suspend fun updateDistractingStatus(packageName: String, isDistracting: Boolean) {
        appDao.updateDistractingStatus(packageName, isDistracting)
    }

    private suspend fun getAppInfoFromPackage(packageName: String): AppInfo? {
        val packageManager = context.packageManager
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val appName = packageManager.getApplicationLabel(appInfo).toString()
            AppInfo(packageName = packageName, appName = appName)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getInstalledApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
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
    }

    suspend fun launchApp(packageName: String, bypassFriction: Boolean = false, plannedDuration: Int? = null): Boolean {
        // Check if app is distracting and should show friction barrier
        val app = getApp(packageName)
        val settings = settingsRepository.getSettingsSync()

        if (!bypassFriction && app?.isDistracting == true) {
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
            intent?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(it)
            }
        }
        return true
    }

    suspend fun shouldShowTimeLimitPrompt(packageName: String): Boolean {
        val settings = settingsRepository.getSettingsSync()
        val app = getApp(packageName)
        return settings.enableTimeLimitPrompt && app?.isDistracting == true
    }

    suspend fun shouldShowMathChallenge(packageName: String): Boolean {
        val settings = settingsRepository.getSettingsSync()
        val app = getApp(packageName)
        return settings.enableMathChallenge && app?.isDistracting == true
    }

    suspend fun getActiveSessionForApp(packageName: String) = sessionRepository?.getActiveSessionForApp(packageName)

    fun closeCurrentApp() {
        // Move the current app to background by launching the home screen
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    suspend fun endSessionForApp(packageName: String) = sessionRepository?.endSessionForApp(packageName)

    suspend fun getAllAppsSync(): List<AppInfo> = appDao.getAllAppsSync()

    suspend fun syncInstalledApps() = withContext(Dispatchers.IO) {
        val installedApps = getInstalledApps()
        val existingApps = getAllAppsSync()

        // Create a map of existing apps for quick lookup
        val existingAppMap = existingApps.associateBy { it.packageName }

        // Add new apps that don't exist in database
        installedApps.forEach { installedApp ->
            if (!existingAppMap.containsKey(installedApp.packageName)) {
                val appInfo = AppInfo(
                    packageName = installedApp.packageName,
                    appName = installedApp.appName
                )
                insertApp(appInfo)
            }
        }

        // Add Device Settings entry if it doesn't exist
        val deviceSettingsPackage = "android.settings"
        if (!existingAppMap.containsKey(deviceSettingsPackage)) {
            val deviceSettingsApp = AppInfo(
                packageName = deviceSettingsPackage,
                appName = "Device Settings"
            )
            insertApp(deviceSettingsApp)
        }
    }
}
