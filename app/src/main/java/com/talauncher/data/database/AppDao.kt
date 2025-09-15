package com.talauncher.data.database

import androidx.room.*
import com.talauncher.data.model.AppInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM app_info")
    fun getAllApps(): Flow<List<AppInfo>>

    @Query("SELECT * FROM app_info WHERE isEssential = 1")
    fun getEssentialApps(): Flow<List<AppInfo>>

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

    @Query("UPDATE app_info SET isEssential = :isEssential WHERE packageName = :packageName")
    suspend fun updateEssentialStatus(packageName: String, isEssential: Boolean)

    @Query("UPDATE app_info SET isDistracting = :isDistracting WHERE packageName = :packageName")
    suspend fun updateDistractingStatus(packageName: String, isDistracting: Boolean)
}