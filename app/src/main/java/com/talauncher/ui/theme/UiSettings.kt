package com.talauncher.ui.theme

import com.talauncher.data.model.ColorPaletteOption
import com.talauncher.data.model.LauncherSettings
import com.talauncher.data.model.UiDensityOption
import com.talauncher.ui.components.UiDensity

/**
 * Shared UI settings data class following DRY principle.
 * Extracts UI-related settings from LauncherSettings to avoid duplication
 * across ViewModels and components.
 */
data class UiSettings(
    val colorPalette: ColorPaletteOption = ColorPaletteOption.DEFAULT,
    val enableGlassmorphism: Boolean = false,
    val enableAnimations: Boolean = true,
    val uiDensity: UiDensityOption = UiDensityOption.COMFORTABLE,
    val showWallpaper: Boolean = false,
    val wallpaperBlurAmount: Float = 0f,
    val backgroundColor: String = "system",
    val backgroundOpacity: Float = 1f,
    val customWallpaperPath: String? = null
)

/**
 * Extension function to convert LauncherSettings to UiSettings.
 * Follows Single Responsibility Principle by isolating UI settings extraction.
 */
fun LauncherSettings.toUiSettings(): UiSettings = UiSettings(
    colorPalette = this.colorPalette,
    enableGlassmorphism = this.enableGlassmorphism,
    enableAnimations = this.enableAnimations,
    uiDensity = this.uiDensity,
    showWallpaper = this.showWallpaper,
    wallpaperBlurAmount = this.wallpaperBlurAmount,
    backgroundColor = this.backgroundColor,
    backgroundOpacity = this.backgroundOpacity,
    customWallpaperPath = this.customWallpaperPath
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
    UiDensityOption.COMFORTABLE -> UiDensity.Comfortable // Default fallback
}

/**
 * Extension function to get UiDensity from UiSettings.
 * Provides type-safe conversion following Single Responsibility Principle.
 */
fun UiSettings.getUiDensity(): UiDensity = this.uiDensity.toUiDensity()
