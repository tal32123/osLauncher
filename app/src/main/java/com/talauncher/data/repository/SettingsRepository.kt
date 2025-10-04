package com.talauncher.data.repository

import com.talauncher.data.database.SettingsDao
import com.talauncher.data.model.ColorPaletteOption
import com.talauncher.data.model.AppIconStyleOption
import com.talauncher.data.model.LauncherSettings
import com.talauncher.data.model.ThemeModeOption
import com.talauncher.data.model.UiDensityOption
import com.talauncher.data.model.WeatherDisplayOption
import com.talauncher.data.model.WeatherTemperatureUnit
import com.talauncher.data.model.AppSectionLayoutOption
import com.talauncher.data.model.AppDisplayStyleOption
import com.talauncher.data.model.IconColorOption
import com.talauncher.data.model.NewsRefreshInterval
import com.talauncher.data.model.NewsCategory
import kotlinx.coroutines.flow.Flow

class SettingsRepository(private val settingsDao: SettingsDao) {

    fun getSettings(): Flow<LauncherSettings?> = settingsDao.getSettings()

    suspend fun getSettingsSync(): LauncherSettings {
        return settingsDao.getSettingsSync() ?: LauncherSettings().also {
            settingsDao.insertSettings(it)
        }
    }


    suspend fun updateSettings(settings: LauncherSettings) {
        settingsDao.updateSettings(settings)
    }

    suspend fun completeOnboarding() {
        val settings = getSettingsSync()
        updateSettings(settings.copy(isOnboardingCompleted = true))
    }

    suspend fun isOnboardingCompleted(): Boolean {
        return getSettingsSync().isOnboardingCompleted
    }

    suspend fun updateTimeLimitPrompt(enabled: Boolean) {
        val settings = getSettingsSync()
        updateSettings(settings.copy(enableTimeLimitPrompt = enabled))
    }

    suspend fun updateDefaultTimeLimit(minutes: Int) {
        val settings = getSettingsSync()
        val sanitized = minutes.coerceIn(5, 480)
        updateSettings(settings.copy(defaultTimeLimitMinutes = sanitized))
    }

    suspend fun updateBuildInfo(
        commitHash: String?,
        commitMessage: String?,
        commitDate: String?,
        branch: String?,
        buildTime: String?
    ) {
        val settings = getSettingsSync()
        updateSettings(settings.copy(
            buildCommitHash = commitHash,
            buildCommitMessage = commitMessage,
            buildCommitDate = commitDate,
            buildBranch = branch,
            buildTime = buildTime
        ))
    }

    suspend fun updateRecentAppsLimit(limit: Int) {
        val settings = getSettingsSync()
        updateSettings(settings.copy(recentAppsLimit = limit.coerceIn(0, 50)))
    }

    suspend fun updateShowPhoneAction(enabled: Boolean) {
        val settings = getSettingsSync()
        updateSettings(settings.copy(showPhoneAction = enabled))
    }

    suspend fun updateShowMessageAction(enabled: Boolean) {
        val settings = getSettingsSync()
        updateSettings(settings.copy(showMessageAction = enabled))
    }

    suspend fun updateShowWhatsAppAction(enabled: Boolean) {
        val settings = getSettingsSync()
        updateSettings(settings.copy(showWhatsAppAction = enabled))
    }

    suspend fun updateWeatherDisplay(display: WeatherDisplayOption) {
        val settings = getSettingsSync()
        updateSettings(settings.copy(weatherDisplay = display))
    }

    suspend fun updateWeatherTemperatureUnit(unit: WeatherTemperatureUnit) {
        val settings = getSettingsSync()
        updateSettings(settings.copy(weatherTemperatureUnit = unit))
    }

    suspend fun updateWeatherLocation(lat: Double?, lon: Double?) {
        val settings = getSettingsSync()
        updateSettings(settings.copy(weatherLocationLat = lat, weatherLocationLon = lon))
    }

    suspend fun updateBackgroundColor(color: String) {
        val settings = getSettingsSync()
        val normalized = when (color.lowercase()) {
            "system", "black", "white" -> color.lowercase()
            else -> settings.backgroundColor
        }
        updateSettings(settings.copy(backgroundColor = normalized))
    }

    suspend fun updateShowWallpaper(show: Boolean) {
        val settings = getSettingsSync()
        updateSettings(settings.copy(showWallpaper = show))
    }

    suspend fun updateColorPalette(palette: ColorPaletteOption) {
        val settings = getSettingsSync()
        updateSettings(settings.copy(colorPalette = palette))
    }

    suspend fun updateAppIconStyle(style: AppIconStyleOption) {
        val settings = getSettingsSync()
        updateSettings(settings.copy(appIconStyle = style))
    }

    suspend fun setCustomPalette(
        palette: ColorPaletteOption,
        customColorOption: String?,
        customPrimaryColor: String?,
        customSecondaryColor: String?
    ) {
        val settings = getSettingsSync()
        updateSettings(
            settings.copy(
                colorPalette = palette,
                customColorOption = customColorOption,
                customPrimaryColor = customPrimaryColor,
                customSecondaryColor = customSecondaryColor
            )
        )
    }

    suspend fun updateThemeMode(mode: ThemeModeOption) {
        val settings = getSettingsSync()
        updateSettings(settings.copy(themeMode = mode))
    }

    suspend fun updateWallpaperBlurAmount(blur: Float) {
        val settings = getSettingsSync()
        updateSettings(settings.copy(wallpaperBlurAmount = blur.coerceIn(0f, 1f)))
    }

    suspend fun updateBackgroundOpacity(opacity: Float) {
        val settings = getSettingsSync()
        updateSettings(settings.copy(backgroundOpacity = opacity.coerceIn(0f, 1f)))
    }

    suspend fun updateCustomWallpaper(path: String?) {
        val settings = getSettingsSync()
        updateSettings(settings.copy(customWallpaperPath = path))
    }

    suspend fun updateGlassmorphism(enabled: Boolean) {
        val settings = getSettingsSync()
        updateSettings(settings.copy(enableGlassmorphism = enabled))
    }

    suspend fun updateUiDensity(density: UiDensityOption) {
        val settings = getSettingsSync()
        updateSettings(settings.copy(uiDensity = density))
    }

    suspend fun updateAnimationsEnabled(enabled: Boolean) {
        val settings = getSettingsSync()
        updateSettings(settings.copy(enableAnimations = enabled))
    }

    // App section display settings update methods
    suspend fun updatePinnedAppsLayout(layout: AppSectionLayoutOption) {
        val settings = getSettingsSync()
        updateSettings(settings.copy(pinnedAppsLayout = layout))
    }

    suspend fun updatePinnedAppsDisplayStyle(style: AppDisplayStyleOption) {
        val settings = getSettingsSync()
        updateSettings(settings.copy(pinnedAppsDisplayStyle = style))
    }

    suspend fun updatePinnedAppsIconColor(color: IconColorOption) {
        val settings = getSettingsSync()
        updateSettings(settings.copy(pinnedAppsIconColor = color))
    }

    suspend fun updateRecentAppsLayout(layout: AppSectionLayoutOption) {
        val settings = getSettingsSync()
        updateSettings(settings.copy(recentAppsLayout = layout))
    }

    suspend fun updateRecentAppsDisplayStyle(style: AppDisplayStyleOption) {
        val settings = getSettingsSync()
        updateSettings(settings.copy(recentAppsDisplayStyle = style))
    }

    suspend fun updateRecentAppsIconColor(color: IconColorOption) {
        val settings = getSettingsSync()
        updateSettings(settings.copy(recentAppsIconColor = color))
    }

    suspend fun updateAllAppsLayout(layout: AppSectionLayoutOption) {
        val settings = getSettingsSync()
        updateSettings(settings.copy(allAppsLayout = layout))
    }

    suspend fun updateAllAppsDisplayStyle(style: AppDisplayStyleOption) {
        val settings = getSettingsSync()
        updateSettings(settings.copy(allAppsDisplayStyle = style))
    }

    suspend fun updateAllAppsIconColor(color: IconColorOption) {
        val settings = getSettingsSync()
        updateSettings(settings.copy(allAppsIconColor = color))
    }

    suspend fun updateSearchLayout(layout: AppSectionLayoutOption) {
        val settings = getSettingsSync()
        updateSettings(settings.copy(searchLayout = layout))
    }

    suspend fun updateSearchDisplayStyle(style: AppDisplayStyleOption) {
        val settings = getSettingsSync()
        updateSettings(settings.copy(searchDisplayStyle = style))
    }

    suspend fun updateSearchIconColor(color: IconColorOption) {
        val settings = getSettingsSync()
        updateSettings(settings.copy(searchIconColor = color))
    }

    // Sidebar / Alphabet Index customization
    suspend fun updateSidebarActiveScale(scale: Float) {
        val settings = getSettingsSync()
        val clamped = scale.coerceIn(1.0f, 3.0f)
        updateSettings(settings.copy(sidebarActiveScale = clamped))
    }

    suspend fun updateSidebarPopOutDp(popOut: Int) {
        val settings = getSettingsSync()
        val clamped = popOut.coerceIn(0, 64)
        updateSettings(settings.copy(sidebarPopOutDp = clamped))
    }

    suspend fun updateSidebarWaveSpread(spread: Float) {
        val settings = getSettingsSync()
        val clamped = spread.coerceIn(0.0f, 5.0f)
        updateSettings(settings.copy(sidebarWaveSpread = clamped))
    }

    suspend fun updateFastScrollerActiveItemScale(scale: Float) {
        val settings = getSettingsSync()
        val clamped = scale.coerceIn(1.0f, 1.2f)
        updateSettings(settings.copy(fastScrollerActiveItemScale = clamped))
    }

    // News settings
    suspend fun updateNewsRefreshInterval(interval: NewsRefreshInterval) {
        val settings = getSettingsSync()
        updateSettings(settings.copy(newsRefreshInterval = interval))
    }

    suspend fun updateNewsCategories(categories: Set<NewsCategory>) {
        val settings = getSettingsSync()
        val csv = categories.joinToString(",") { it.name }
        updateSettings(settings.copy(newsCategoriesCsv = csv))
    }
}
