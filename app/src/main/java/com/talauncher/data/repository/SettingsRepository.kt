package com.talauncher.data.repository

import com.talauncher.data.database.SettingsDao
import com.talauncher.data.model.LauncherSettings
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

    suspend fun updateRecentAppsLimit(limit: Int) {
        val settings = getSettingsSync()
        updateSettings(settings.copy(recentAppsLimit = limit.coerceIn(1, 50)))
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
}
