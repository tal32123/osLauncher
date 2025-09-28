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
    val mathDifficulty: MathDifficulty = MathDifficulty.EASY,
    val sessionExpiryCountdownSeconds: Int = 5, // Number of seconds to block the app before prompting for action
    val recentAppsLimit: Int = 10, // Number of recent apps to show in insights
    val defaultTimeLimitMinutes: Int = 30, // Default time limit applied to distracting apps
    val showPhoneAction: Boolean = true,
    val showMessageAction: Boolean = true,
    val showWhatsAppAction: Boolean = true,
    val weatherDisplay: WeatherDisplayOption = WeatherDisplayOption.DAILY,
    val weatherTemperatureUnit: WeatherTemperatureUnit = WeatherTemperatureUnit.CELSIUS,
    val weatherLocationLat: Double? = null,
    val weatherLocationLon: Double? = null,
    val buildCommitHash: String? = null,
    val buildCommitMessage: String? = null,
    val buildCommitDate: String? = null,
    val buildBranch: String? = null,
    val buildTime: String? = null,

    // New 2025 minimalist UI customization options
    val colorPalette: ColorPaletteOption = ColorPaletteOption.DEFAULT,
    val customColorOption: String? = null, // Selected custom color from ColorPalettes.CustomColorOptions
    val wallpaperBlurAmount: Float = 0f, // 0.0 (no blur) to 1.0 (max blur)
    val enableGlassmorphism: Boolean = true, // Enable glass-like translucent effects
    val uiDensity: UiDensityOption = UiDensityOption.COMFORTABLE,
    val cardCornerRadius: Int = 12, // Corner radius for cards in dp (8-24)
    val enableDynamicColors: Boolean = false, // Use Material You dynamic colors when available
    val backgroundOpacity: Float = 1.0f, // Background opacity when wallpaper is shown (0.0-1.0)
    val customWallpaperPath: String? = null, // Path to custom wallpaper image
    val accentColor: String? = null, // Custom accent color override (hex)
    val fontScale: Float = 1.0f, // Text size scaling factor (0.8-1.4)
    val enableAnimations: Boolean = true, // Enable smooth UI animations
    val cardElevation: Int = 2 // Card elevation in dp (0-8)
)
