package com.talauncher.data.database

import androidx.room.*
import com.talauncher.data.model.LauncherSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM launcher_settings WHERE id = 1")
    fun getSettings(): Flow<LauncherSettings?>

    @Query("SELECT * FROM launcher_settings WHERE id = 1")
    suspend fun getSettingsSync(): LauncherSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: LauncherSettings)

    @Update
    suspend fun updateSettings(settings: LauncherSettings)

    @Query("UPDATE launcher_settings SET isFocusModeEnabled = :enabled WHERE id = 1")
    suspend fun updateFocusMode(enabled: Boolean)
}