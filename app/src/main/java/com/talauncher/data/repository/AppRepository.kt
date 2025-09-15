package com.talauncher.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.talauncher.data.database.AppDao
import com.talauncher.data.model.AppInfo
import com.talauncher.data.model.InstalledApp
import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val appDao: AppDao,
    private val context: Context
) {
    fun getAllApps(): Flow<List<AppInfo>> = appDao.getAllApps()

    fun getEssentialApps(): Flow<List<AppInfo>> = appDao.getEssentialApps()

    fun getDistractingApps(): Flow<List<AppInfo>> = appDao.getDistractingApps()

    suspend fun getApp(packageName: String): AppInfo? = appDao.getApp(packageName)

    suspend fun insertApp(app: AppInfo) = appDao.insertApp(app)

    suspend fun updateEssentialStatus(packageName: String, isEssential: Boolean) {
        appDao.updateEssentialStatus(packageName, isEssential)
    }

    suspend fun updateDistractingStatus(packageName: String, isDistracting: Boolean) {
        appDao.updateDistractingStatus(packageName, isDistracting)
    }

    fun getInstalledApps(): List<InstalledApp> {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val activities = packageManager.queryIntentActivities(intent, 0)

        return activities.map { resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName
            val appName = resolveInfo.loadLabel(packageManager).toString()
            val isSystemApp = (resolveInfo.activityInfo.applicationInfo.flags and
                android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0

            InstalledApp(packageName, appName, isSystemApp)
        }.distinctBy { it.packageName }
            .sortedBy { it.appName }
    }

    suspend fun launchApp(packageName: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        intent?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(it)
        }
    }
}