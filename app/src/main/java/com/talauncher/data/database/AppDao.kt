package com.talauncher.data.database

import androidx.room.*
import com.talauncher.data.model.AppInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM app_info WHERE isHidden = 0 ORDER BY appName ASC")
    fun getAllVisibleApps(): Flow<List<AppInfo>>

    @Query("SELECT * FROM app_info WHERE isPinned = 1 ORDER BY pinnedOrder ASC, appName ASC")
    fun getPinnedApps(): Flow<List<AppInfo>>

    @Query("SELECT * FROM app_info WHERE isHidden = 1 ORDER BY appName ASC")
    fun getHiddenApps(): Flow<List<AppInfo>>

    @Query("SELECT * FROM app_info WHERE isDistracting = 1")
    fun getDistractingApps(): Flow<List<AppInfo>>

    @Query("SELECT * FROM app_info WHERE packageName = :packageName")
    suspend fun getApp(packageName: String): AppInfo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: AppInfo)

    @Update
    suspend fun updateApp(app: AppInfo)

    @Delete
    suspend fun deleteApp(app: AppInfo)

    @Query("UPDATE app_info SET appName = :appName WHERE packageName = :packageName")
    suspend fun updateAppName(packageName: String, appName: String)

    @Query("UPDATE app_info SET isPinned = :isPinned, pinnedOrder = :order WHERE packageName = :packageName")
    suspend fun updatePinnedStatus(packageName: String, isPinned: Boolean, order: Int = 0)

    @Query("UPDATE app_info SET isHidden = :isHidden WHERE packageName = :packageName")
    suspend fun updateHiddenStatus(packageName: String, isHidden: Boolean)

    @Query("UPDATE app_info SET isDistracting = :isDistracting WHERE packageName = :packageName")
    suspend fun updateDistractingStatus(packageName: String, isDistracting: Boolean)

    @Query("SELECT MAX(pinnedOrder) FROM app_info WHERE isPinned = 1")
    suspend fun getMaxPinnedOrder(): Int?

    @Query("SELECT * FROM app_info")
    fun getAllApps(): Flow<List<AppInfo>>

    @Query("SELECT * FROM app_info")
    suspend fun getAllAppsSync(): List<AppInfo>
}