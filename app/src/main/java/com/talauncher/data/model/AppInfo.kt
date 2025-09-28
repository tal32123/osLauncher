package com.talauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_info")
data class AppInfo(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isHidden: Boolean = false,
    val isDistracting: Boolean = false,
    val timeLimitMinutes: Int? = null
)

data class InstalledApp(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean = false
)

data class AppUsage(
    val packageName: String,
    val timeInForeground: Long,
    val lastTimeUsed: Long = 0L
)