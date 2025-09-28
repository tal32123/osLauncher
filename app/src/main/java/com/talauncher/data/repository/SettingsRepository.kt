package com.talauncher.data.repository

import com.talauncher.data.database.SettingsDao
import com.talauncher.data.model.ColorPaletteOption
import com.talauncher.data.model.LauncherSettings
import com.talauncher.data.model.MathDifficulty
import com.talauncher.data.model.UiDensityOption
import com.talauncher.data.model.WeatherDisplayOption
import com.talauncher.data.model.WeatherTemperatureUnit
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

    suspend fun updateMathChallenge(enabled: Boolean) {
        val settings = getSettingsSync()
        updateSettings(settings.copy(enableMathChallenge = enabled))
    }

    suspend fun updateMathDifficulty(difficulty: MathDifficulty) {
        val settings = getSettingsSync()
        updateSettings(settings.copy(mathDifficulty = difficulty))
    }

    suspend fun updateSessionExpiryCountdown(seconds: Int) {
        val settings = getSettingsSync()
        updateSettings(settings.copy(sessionExpiryCountdownSeconds = seconds.coerceIn(0, 30)))
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

    suspend fun updateCustomColorOption(customColorOption: String?) {
        val settings = getSettingsSync()
        updateSettings(settings.copy(customColorOption = customColorOption))
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
}
