package com.talauncher.ui.theme

import com.talauncher.data.model.ColorPaletteOption
import com.talauncher.data.model.LauncherSettings
import com.talauncher.data.model.ThemeModeOption
import com.talauncher.data.model.UiDensityOption
import com.talauncher.ui.components.UiDensity

/**
 * Shared UI settings data class following DRY principle.
 * Extracts UI-related settings from LauncherSettings to avoid duplication
 * across ViewModels and components.
 */
data class UiSettings(
    val colorPalette: ColorPaletteOption = ColorPaletteOption.DEFAULT,
    val themeMode: ThemeModeOption = ThemeModeOption.SYSTEM,
    val enableGlassmorphism: Boolean = true,
    val enableAnimations: Boolean = true,
    val uiDensity: UiDensityOption = UiDensityOption.COMPACT,
    val showWallpaper: Boolean = false,
    val wallpaperBlurAmount: Float = 0f,
    val backgroundColor: String = "system",
    val backgroundOpacity: Float = 1f,
    val customWallpaperPath: String? = null,
    val customPrimaryColor: String? = null,
    val customSecondaryColor: String? = null
)

/**
 * Extension function to convert LauncherSettings to UiSettings.
 * Follows Single Responsibility Principle by isolating UI settings extraction.
 */
fun LauncherSettings.toUiSettings(): UiSettings = UiSettings(
    colorPalette = this.colorPalette,
    themeMode = this.themeMode,
    enableGlassmorphism = this.enableGlassmorphism,
    enableAnimations = this.enableAnimations,
    uiDensity = this.uiDensity,
    showWallpaper = this.showWallpaper,
    wallpaperBlurAmount = this.wallpaperBlurAmount,
    backgroundColor = this.backgroundColor,
    backgroundOpacity = this.backgroundOpacity,
    customWallpaperPath = this.customWallpaperPath,
    customPrimaryColor = this.customPrimaryColor,
    customSecondaryColor = this.customSecondaryColor
)

/**
 * Extension function to convert nullable LauncherSettings to UiSettings with defaults.
 * Provides safe defaults following the Null Object Pattern.
 */
fun LauncherSettings?.toUiSettingsOrDefault(): UiSettings =
    this?.toUiSettings() ?: UiSettings()

/**
 * Extension function to convert string uiDensity to UiDensity enum.
 * Follows the Null Object Pattern with safe defaults.
 */
fun UiDensityOption.toUiDensity(): UiDensity = when (this) {
    UiDensityOption.COMPACT -> UiDensity.Compact
    UiDensityOption.SPACIOUS -> UiDensity.Spacious
    UiDensityOption.COMFORTABLE -> UiDensity.Comfortable
}

/**
 * Extension function to get UiDensity from UiSettings.
 * Provides type-safe conversion following Single Responsibility Principle.
 */
fun UiSettings.getUiDensity(): UiDensity = this.uiDensity.toUiDensity()
