package com.talauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "launcher_settings")
data class LauncherSettings(
    @PrimaryKey val id: Int = 1,
    val showTimeOnHomeScreen: Boolean = true,
    val showDateOnHomeScreen: Boolean = true,
    val isOnboardingCompleted: Boolean = false,
    val backgroundColor: String = "system", // "system", "black", "white", or hex color
    val enableHapticFeedback: Boolean = true,
    val showWallpaper: Boolean = true, // Whether to show device wallpaper or solid background
    val enableTimeLimitPrompt: Boolean = false, // Whether to prompt for time limits on app launch
    val enableMathChallenge: Boolean = false, // Whether to require math challenges to close apps
    val mathDifficulty: String = "easy", // "easy", "medium", "hard"
    val sessionExpiryCountdownSeconds: Int = 5, // Number of seconds to block the app before prompting for action
    val recentAppsLimit: Int = 10, // Number of recent apps to show in insights
    val showPhoneAction: Boolean = true,
    val showMessageAction: Boolean = true,
    val showWhatsAppAction: Boolean = true,
    val weatherDisplay: String = "off", // "off", "daily", "hourly"
    val weatherTemperatureUnit: String = "celsius", // "celsius" or "fahrenheit"
    val weatherLocationLat: Double? = null,
    val weatherLocationLon: Double? = null,
    val buildCommitHash: String? = null,
    val buildCommitMessage: String? = null,
    val buildCommitDate: String? = null,
    val buildBranch: String? = null,
    val buildTime: String? = null
)
