package com.talauncher.data.repository

import com.talauncher.data.database.SettingsDao
import com.talauncher.data.model.LauncherSettings
import java.util.Locale
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

    suspend fun updateMathDifficulty(difficulty: String) {
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



    suspend fun updateWeatherDisplay(display: String) {
        val settings = getSettingsSync()
        val allowedValues = setOf("off", "daily", "hourly")
        val normalized = display.lowercase().takeIf { it in allowedValues }
            ?: settings.weatherDisplay
        updateSettings(settings.copy(weatherDisplay = normalized))
    }

    suspend fun updateWeatherTemperatureUnit(unit: String) {
        val normalizedUnit = if (unit.lowercase() == "fahrenheit") "fahrenheit" else "celsius"
        val settings = getSettingsSync()
        updateSettings(settings.copy(weatherTemperatureUnit = normalizedUnit))
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

    suspend fun updateColorPalette(palette: String) {
        val settings = getSettingsSync()
        val allowedPalettes = setOf("default", "warm", "cool", "monochrome", "nature")
        val normalized = palette.lowercase().takeIf { it in allowedPalettes }
            ?: settings.colorPalette
        updateSettings(settings.copy(colorPalette = normalized))
    }

    suspend fun updateWallpaperBlurAmount(blur: Float) {
        val settings = getSettingsSync()
        updateSettings(settings.copy(wallpaperBlurAmount = blur.coerceIn(0f, 1f)))
    }

    suspend fun updateGlassmorphism(enabled: Boolean) {
        val settings = getSettingsSync()
        updateSettings(settings.copy(enableGlassmorphism = enabled))
    }

    suspend fun updateUiDensity(density: String) {
        val settings = getSettingsSync()
        val allowedDensity = setOf("compact", "comfortable", "spacious")
        val normalized = density.lowercase().takeIf { it in allowedDensity }
            ?: settings.uiDensity
        updateSettings(settings.copy(uiDensity = normalized))
    }

    suspend fun updateAnimationsEnabled(enabled: Boolean) {
        val settings = getSettingsSync()
        updateSettings(settings.copy(enableAnimations = enabled))
    }
}
