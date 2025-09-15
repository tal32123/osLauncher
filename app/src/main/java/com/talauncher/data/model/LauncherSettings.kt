package com.talauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "launcher_settings")
data class LauncherSettings(
    @PrimaryKey val id: Int = 1,
    val isFocusModeEnabled: Boolean = false,
    val showTimeOnHomeScreen: Boolean = true,
    val showDateOnHomeScreen: Boolean = true,
    val isOnboardingCompleted: Boolean = false,
    val backgroundColor: String = "system", // "system", "black", "white", or hex color
    val enableHapticFeedback: Boolean = true,
    val showWallpaper: Boolean = true, // Whether to show device wallpaper or solid background
    val enableTimeLimitPrompt: Boolean = false, // Whether to prompt for time limits on app launch
    val enableMathChallenge: Boolean = false, // Whether to require math challenges to close apps
    val mathDifficulty: String = "easy" // "easy", "medium", "hard"
)