package com.talauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "launcher_settings")
data class LauncherSettings(
    @PrimaryKey val id: Int = 1,
    val isFocusModeEnabled: Boolean = false,
    val showTimeOnHomeScreen: Boolean = true,
    val showDateOnHomeScreen: Boolean = true
)