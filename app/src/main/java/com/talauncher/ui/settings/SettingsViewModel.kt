package com.talauncher.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talauncher.data.model.AppInfo
import com.talauncher.data.model.InstalledApp
import com.talauncher.data.repository.AppRepository
import com.talauncher.data.repository.SettingsRepository
import com.talauncher.utils.PermissionsHelper
import com.talauncher.utils.UsageStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    private val appRepository: AppRepository,
    private val settingsRepository: SettingsRepository,
    val permissionsHelper: PermissionsHelper,
    val usageStatsHelper: UsageStatsHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var allInstalledApps: List<InstalledApp> = emptyList()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            allInstalledApps = withContext(Dispatchers.IO) {
                appRepository.getInstalledApps()
            }

            combine(
                appRepository.getPinnedApps(),
                appRepository.getDistractingApps(),
                settingsRepository.getSettings()
            ) { pinnedApps, distractingApps, settings ->
                _uiState.value = _uiState.value.copy(
                    pinnedApps = pinnedApps,
                    distractingApps = distractingApps,
                    backgroundColor = settings?.backgroundColor ?: "system",
                    showWallpaper = settings?.showWallpaper ?: true,
                    enableTimeLimitPrompt = settings?.enableTimeLimitPrompt ?: false,
                    enableMathChallenge = settings?.enableMathChallenge ?: false,
                    mathDifficulty = settings?.mathDifficulty ?: "easy",
                    sessionExpiryCountdownSeconds = settings?.sessionExpiryCountdownSeconds ?: 5,
                    recentAppsLimit = settings?.recentAppsLimit ?: 5,
                    showPhoneAction = settings?.showPhoneAction ?: true,
                    showMessageAction = settings?.showMessageAction ?: true,
                    showWhatsAppAction = settings?.showWhatsAppAction ?: true,
                    weatherDisplay = settings?.weatherDisplay ?: "off",
                    weatherTemperatureUnit = settings?.weatherTemperatureUnit ?: "celsius",
                    buildCommitHash = settings?.buildCommitHash,
                    buildCommitMessage = settings?.buildCommitMessage,
                    buildCommitDate = settings?.buildCommitDate,
                    buildBranch = settings?.buildBranch,
                    buildTime = settings?.buildTime,
                    colorPalette = settings?.colorPalette ?: "default",
                    wallpaperBlurAmount = settings?.wallpaperBlurAmount ?: 0f,
                    enableGlassmorphism = settings?.enableGlassmorphism ?: false,
                    uiDensity = settings?.uiDensity ?: "comfortable",
                    enableAnimations = settings?.enableAnimations ?: true,
                    availableApps = allInstalledApps,
                    isLoading = false
                )
            }.collect { /* Data collection handled in the combine block */ }
        }
    }

    fun toggleEssentialApp(packageName: String) {
        viewModelScope.launch {
            val currentApp = appRepository.getApp(packageName)
            if (currentApp?.isPinned == true) {
                appRepository.unpinApp(packageName)
            } else {
                appRepository.pinApp(packageName)
            }
        }
    }

    fun toggleDistractingApp(packageName: String) {
        viewModelScope.launch {
            val currentApp = appRepository.getApp(packageName)
            val installedApp = allInstalledApps.find { it.packageName == packageName }

            if (currentApp != null) {
                appRepository.updateDistractingStatus(packageName, !currentApp.isDistracting)
            } else if (installedApp != null) {
                appRepository.insertApp(
                    AppInfo(
                        packageName = packageName,
                        appName = installedApp.appName,
                        isPinned = false,
                        isHidden = false,
                        isDistracting = true
                    )
                )
            }
        }
    }


    fun toggleTimeLimitPrompt() {
        viewModelScope.launch {
            settingsRepository.updateTimeLimitPrompt(!_uiState.value.enableTimeLimitPrompt)
        }
    }

    fun toggleMathChallenge() {
        viewModelScope.launch {
            settingsRepository.updateMathChallenge(!_uiState.value.enableMathChallenge)
        }
    }

    fun updateMathDifficulty(difficulty: String) {
        viewModelScope.launch {
            settingsRepository.updateMathDifficulty(difficulty)
        }
    }

    fun updateSessionExpiryCountdown(seconds: Int) {
        viewModelScope.launch {
            settingsRepository.updateSessionExpiryCountdown(seconds)
        }
    }

    fun updateRecentAppsLimit(limit: Int) {
        viewModelScope.launch {
            settingsRepository.updateRecentAppsLimit(limit)
        }
    }

    fun toggleShowPhoneAction() {
        viewModelScope.launch {
            settingsRepository.updateShowPhoneAction(!_uiState.value.showPhoneAction)
        }
    }

    fun toggleShowMessageAction() {
        viewModelScope.launch {
            settingsRepository.updateShowMessageAction(!_uiState.value.showMessageAction)
        }
    }

    fun toggleShowWhatsAppAction() {
        viewModelScope.launch {
            settingsRepository.updateShowWhatsAppAction(!_uiState.value.showWhatsAppAction)
        }
    }

    fun updateWeatherDisplay(display: String) {
        viewModelScope.launch {
            settingsRepository.updateWeatherDisplay(display)

            // If enabling weather, request location permission if not already granted
            if (display != "off" && !permissionsHelper.hasLocationPermission()) {
                // Permission will be requested in the UI when user interacts
            }
        }
    }

    fun updateWeatherTemperatureUnit(unit: String) {
        viewModelScope.launch {
            settingsRepository.updateWeatherTemperatureUnit(unit)
        }
    }

    fun updateBackgroundColor(color: String) {
        viewModelScope.launch {
            settingsRepository.updateBackgroundColor(color)
        }
    }

    fun updateShowWallpaper(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateShowWallpaper(show)
        }
    }

    fun updateColorPalette(palette: String) {
        viewModelScope.launch {
            settingsRepository.updateColorPalette(palette)
        }
    }

    fun updateWallpaperBlur(amount: Float) {
        viewModelScope.launch {
            settingsRepository.updateWallpaperBlurAmount(amount)
        }
    }

    fun updateGlassmorphism(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateGlassmorphism(enabled)
        }
    }

    fun updateUiDensity(density: String) {
        viewModelScope.launch {
            settingsRepository.updateUiDensity(density)
        }
    }

    fun updateAnimationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAnimationsEnabled(enabled)
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun clearSearchQuery() {
        _uiState.value = _uiState.value.copy(searchQuery = "")
    }

    fun getFilteredApps(): List<InstalledApp> {
        val query = _uiState.value.searchQuery
        return if (query.isBlank()) {
            allInstalledApps
        } else {
            allInstalledApps.filter { app ->
                app.appName.contains(query, ignoreCase = true)
            }
        }
    }

    fun isAppPinned(packageName: String): Boolean {
        return _uiState.value.pinnedApps.any { it.packageName == packageName }
    }

    fun isAppDistracting(packageName: String): Boolean {
        return _uiState.value.distractingApps.any { it.packageName == packageName }
    }
}

data class SettingsUiState(
    val pinnedApps: List<AppInfo> = emptyList(),
    val distractingApps: List<AppInfo> = emptyList(),
    val availableApps: List<InstalledApp> = emptyList(),
    val backgroundColor: String = "system",
    val showWallpaper: Boolean = true,
    val enableTimeLimitPrompt: Boolean = false,
    val enableMathChallenge: Boolean = false,
    val mathDifficulty: String = "easy",
    val sessionExpiryCountdownSeconds: Int = 5,
    val recentAppsLimit: Int = 5,
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val showPhoneAction: Boolean = false,
    val showMessageAction: Boolean = false,
    val showWhatsAppAction: Boolean = false,
    val weatherDisplay: String = "off",
    val weatherTemperatureUnit: String = "celsius",
    val buildCommitHash: String? = null,
    val buildCommitMessage: String? = null,
    val buildCommitDate: String? = null,
    val buildBranch: String? = null,
    val buildTime: String? = null,
    val colorPalette: String = "default",
    val wallpaperBlurAmount: Float = 0f,
    val enableGlassmorphism: Boolean = false,
    val uiDensity: String = "comfortable",
    val enableAnimations: Boolean = true
)
