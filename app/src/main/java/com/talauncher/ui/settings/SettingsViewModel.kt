package com.talauncher.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talauncher.data.model.AppInfo
import com.talauncher.data.model.ColorPaletteOption
import com.talauncher.data.model.AppIconStyleOption
import com.talauncher.data.model.InstalledApp
import com.talauncher.data.model.ThemeModeOption
import com.talauncher.data.model.UiDensityOption
import com.talauncher.data.model.WeatherDisplayOption
import com.talauncher.data.model.WeatherTemperatureUnit
import com.talauncher.data.model.AppSectionLayoutOption
import com.talauncher.data.model.AppDisplayStyleOption
import com.talauncher.data.model.IconColorOption
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

/**
 * ViewModel for Settings screen managing UI state and coordinating with repositories.
 */
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
                appRepository.getDistractingApps(),
                settingsRepository.getSettings()
            ) { distractingApps, settings ->
                _uiState.value = _uiState.value.copy(
                    distractingApps = distractingApps,
                    backgroundColor = settings?.backgroundColor ?: "system",
                    showWallpaper = settings?.showWallpaper ?: true,
                    enableTimeLimitPrompt = settings?.enableTimeLimitPrompt ?: false,
                    recentAppsLimit = settings?.recentAppsLimit ?: 5,
                    defaultTimeLimitMinutes = settings?.defaultTimeLimitMinutes ?: 30,
                    showPhoneAction = settings?.showPhoneAction ?: true,
                    showMessageAction = settings?.showMessageAction ?: true,
                    showWhatsAppAction = settings?.showWhatsAppAction ?: true,
                    weatherDisplay = settings?.weatherDisplay ?: WeatherDisplayOption.DAILY,
                    weatherTemperatureUnit = settings?.weatherTemperatureUnit
                        ?: WeatherTemperatureUnit.CELSIUS,

                    buildCommitHash = settings?.buildCommitHash,
                    buildCommitMessage = settings?.buildCommitMessage,
                    buildCommitDate = settings?.buildCommitDate,
                    buildBranch = settings?.buildBranch,
                    buildTime = settings?.buildTime,
                    colorPalette = settings?.colorPalette ?: ColorPaletteOption.DEFAULT,
                    appIconStyle = settings?.appIconStyle ?: AppIconStyleOption.ORIGINAL,
                    customColorOption = settings?.customColorOption,
                    customPrimaryColor = settings?.customPrimaryColor,
                    customSecondaryColor = settings?.customSecondaryColor,
                    themeMode = settings?.themeMode ?: ThemeModeOption.SYSTEM,
                    wallpaperBlurAmount = settings?.wallpaperBlurAmount ?: 0f,
                    backgroundOpacity = settings?.backgroundOpacity ?: 1f,
                    enableGlassmorphism = settings?.enableGlassmorphism ?: true,
                    uiDensity = settings?.uiDensity ?: UiDensityOption.COMPACT,
                    enableAnimations = settings?.enableAnimations ?: true,
                    customWallpaperPath = settings?.customWallpaperPath,

                    // App section display settings
                    pinnedAppsLayout = settings?.pinnedAppsLayout ?: AppSectionLayoutOption.LIST,
                    pinnedAppsDisplayStyle = settings?.pinnedAppsDisplayStyle ?: AppDisplayStyleOption.ICON_AND_TEXT,
                    pinnedAppsIconColor = settings?.pinnedAppsIconColor ?: IconColorOption.ORIGINAL,

                    recentAppsLayout = settings?.recentAppsLayout ?: AppSectionLayoutOption.LIST,
                    recentAppsDisplayStyle = settings?.recentAppsDisplayStyle ?: AppDisplayStyleOption.ICON_AND_TEXT,
                    recentAppsIconColor = settings?.recentAppsIconColor ?: IconColorOption.ORIGINAL,

                    allAppsLayout = settings?.allAppsLayout ?: AppSectionLayoutOption.LIST,
                    allAppsDisplayStyle = settings?.allAppsDisplayStyle ?: AppDisplayStyleOption.ICON_AND_TEXT,
                    allAppsIconColor = settings?.allAppsIconColor ?: IconColorOption.ORIGINAL,

                    // Sidebar settings
                    sidebarActiveScale = settings?.sidebarActiveScale ?: 1.4f,
                    sidebarPopOutDp = settings?.sidebarPopOutDp ?: 16,
                    sidebarWaveSpread = settings?.sidebarWaveSpread ?: 1.5f,
                    fastScrollerActiveItemScale = settings?.fastScrollerActiveItemScale ?: 1.06f,
                    availableApps = allInstalledApps,
                    isLoading = false
                )
            }.collect { /* Data collection handled in the combine block */ }
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

    fun updateDefaultTimeLimit(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.updateDefaultTimeLimit(minutes)
        }
    }

    fun updateRecentAppsLimit(limit: Int) {
        viewModelScope.launch {
            settingsRepository.updateRecentAppsLimit(limit)
        }
    }

    fun updateAppTimeLimit(packageName: String, minutes: Int?) {
        viewModelScope.launch {
            appRepository.updateAppTimeLimit(packageName, minutes)
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

    fun updateShowWallpaper(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateShowWallpaper(enabled)
        }
    }

    fun updateBackgroundColor(color: String) {
        viewModelScope.launch {
            settingsRepository.updateBackgroundColor(color)
        }
    }

    fun updateColorPalette(palette: ColorPaletteOption) {
        viewModelScope.launch {
            if (palette == ColorPaletteOption.CUSTOM) {
                settingsRepository.setCustomPalette(
                    palette = ColorPaletteOption.CUSTOM,
                    customColorOption = _uiState.value.customColorOption,
                    customPrimaryColor = _uiState.value.customPrimaryColor,
                    customSecondaryColor = _uiState.value.customSecondaryColor
                )
            } else {
                settingsRepository.updateColorPalette(palette)
            }
        }
    }

    fun updateAppIconStyle(style: AppIconStyleOption) {
        viewModelScope.launch {
            settingsRepository.updateAppIconStyle(style)
        }
    }

    fun updateCustomColorOption(colorOption: String) {
        setCustomPalette(
            customColorOption = colorOption,
            customPrimaryColor = _uiState.value.customPrimaryColor,
            customSecondaryColor = _uiState.value.customSecondaryColor
        )
    }

    fun updateCustomPrimaryColor(primaryColor: String?) {
        setCustomPalette(
            customColorOption = _uiState.value.customColorOption,
            customPrimaryColor = primaryColor,
            customSecondaryColor = _uiState.value.customSecondaryColor
        )
    }

    fun updateCustomSecondaryColor(secondaryColor: String?) {
        setCustomPalette(
            customColorOption = _uiState.value.customColorOption,
            customPrimaryColor = _uiState.value.customPrimaryColor,
            customSecondaryColor = secondaryColor
        )
    }

    private fun setCustomPalette(
        customColorOption: String?,
        customPrimaryColor: String?,
        customSecondaryColor: String?
    ) {
        viewModelScope.launch {
            settingsRepository.setCustomPalette(
                palette = ColorPaletteOption.CUSTOM,
                customColorOption = customColorOption,
                customPrimaryColor = customPrimaryColor,
                customSecondaryColor = customSecondaryColor
            )
        }
    }

    fun updateThemeMode(mode: ThemeModeOption) {
        viewModelScope.launch {
            settingsRepository.updateThemeMode(mode)
        }
    }

    fun updateWallpaperBlur(amount: Float) {
        viewModelScope.launch {
            settingsRepository.updateWallpaperBlurAmount(amount)
        }
    }

    fun updateBackgroundOpacity(opacity: Float) {
        viewModelScope.launch {
            settingsRepository.updateBackgroundOpacity(opacity)
        }
    }

    fun updateCustomWallpaper(path: String?) {
        viewModelScope.launch {
            settingsRepository.updateCustomWallpaper(path)
        }
    }

    fun updateGlassmorphism(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateGlassmorphism(enabled)
        }
    }

    fun updateUiDensity(density: UiDensityOption) {
        viewModelScope.launch {
            settingsRepository.updateUiDensity(density)
        }
    }

    fun updateAnimationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAnimationsEnabled(enabled)
        }
    }

    // Sidebar settings
    fun updateSidebarActiveScale(scale: Float) {
        viewModelScope.launch {
            settingsRepository.updateSidebarActiveScale(scale)
        }
    }

    fun updateSidebarPopOutDp(popOut: Int) {
        viewModelScope.launch {
            settingsRepository.updateSidebarPopOutDp(popOut)
        }
    }

    fun updateSidebarWaveSpread(spread: Float) {
        viewModelScope.launch {
            settingsRepository.updateSidebarWaveSpread(spread)
        }
    }

    fun updateFastScrollerActiveItemScale(scale: Float) {
        viewModelScope.launch {
            settingsRepository.updateFastScrollerActiveItemScale(scale)
        }
    }

    fun updateWeatherDisplay(display: WeatherDisplayOption) {
        viewModelScope.launch {
            settingsRepository.updateWeatherDisplay(display)

            // If enabling weather, request location permission if not already granted
            if (display != WeatherDisplayOption.OFF && !permissionsHelper.hasLocationPermission()) {
                // Permission will be requested in the UI when user interacts
            }
        }
    }

    fun updateWeatherTemperatureUnit(unit: WeatherTemperatureUnit) {
        viewModelScope.launch {
            settingsRepository.updateWeatherTemperatureUnit(unit)
        }
    }

    // App section display settings update methods
    fun updatePinnedAppsLayout(layout: AppSectionLayoutOption) {
        viewModelScope.launch {
            settingsRepository.updatePinnedAppsLayout(layout)
        }
    }

    fun updatePinnedAppsDisplayStyle(style: AppDisplayStyleOption) {
        viewModelScope.launch {
            settingsRepository.updatePinnedAppsDisplayStyle(style)
        }
    }

    fun updatePinnedAppsIconColor(color: IconColorOption) {
        viewModelScope.launch {
            settingsRepository.updatePinnedAppsIconColor(color)
        }
    }

    fun updateRecentAppsLayout(layout: AppSectionLayoutOption) {
        viewModelScope.launch {
            settingsRepository.updateRecentAppsLayout(layout)
        }
    }

    fun updateRecentAppsDisplayStyle(style: AppDisplayStyleOption) {
        viewModelScope.launch {
            settingsRepository.updateRecentAppsDisplayStyle(style)
        }
    }

    fun updateRecentAppsIconColor(color: IconColorOption) {
        viewModelScope.launch {
            settingsRepository.updateRecentAppsIconColor(color)
        }
    }

    fun updateAllAppsLayout(layout: AppSectionLayoutOption) {
        viewModelScope.launch {
            settingsRepository.updateAllAppsLayout(layout)
        }
    }

    fun updateAllAppsDisplayStyle(style: AppDisplayStyleOption) {
        viewModelScope.launch {
            settingsRepository.updateAllAppsDisplayStyle(style)
        }
    }

    fun updateAllAppsIconColor(color: IconColorOption) {
        viewModelScope.launch {
            settingsRepository.updateAllAppsIconColor(color)
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


    fun isAppDistracting(packageName: String): Boolean {
        return _uiState.value.distractingApps.any { it.packageName == packageName }
    }
}

data class SettingsUiState(
    val distractingApps: List<AppInfo> = emptyList(),
    val availableApps: List<InstalledApp> = emptyList(),
    val backgroundColor: String = "system",
    val showWallpaper: Boolean = true,
    val enableTimeLimitPrompt: Boolean = false,
    val recentAppsLimit: Int = 5,
    val defaultTimeLimitMinutes: Int = 30,
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val showPhoneAction: Boolean = false,
    val showMessageAction: Boolean = false,
    val showWhatsAppAction: Boolean = false,
    val weatherDisplay: WeatherDisplayOption = WeatherDisplayOption.DAILY,
    val weatherTemperatureUnit: WeatherTemperatureUnit = WeatherTemperatureUnit.CELSIUS,

    val buildCommitHash: String? = null,
    val buildCommitMessage: String? = null,
    val buildCommitDate: String? = null,
    val buildBranch: String? = null,
    val buildTime: String? = null,
    val colorPalette: ColorPaletteOption = ColorPaletteOption.DEFAULT,
    val appIconStyle: AppIconStyleOption = AppIconStyleOption.ORIGINAL,
    val customColorOption: String? = null,
    val customPrimaryColor: String? = null,
    val customSecondaryColor: String? = null,
    val themeMode: ThemeModeOption = ThemeModeOption.SYSTEM,
    val wallpaperBlurAmount: Float = 0f,
    val backgroundOpacity: Float = 1f,
    val enableGlassmorphism: Boolean = true,
    val uiDensity: UiDensityOption = UiDensityOption.COMPACT,
    val enableAnimations: Boolean = true,
    val customWallpaperPath: String? = null,

    // App section display settings
    val pinnedAppsLayout: AppSectionLayoutOption = AppSectionLayoutOption.LIST,
    val pinnedAppsDisplayStyle: AppDisplayStyleOption = AppDisplayStyleOption.ICON_AND_TEXT,
    val pinnedAppsIconColor: IconColorOption = IconColorOption.ORIGINAL,

    val recentAppsLayout: AppSectionLayoutOption = AppSectionLayoutOption.LIST,
    val recentAppsDisplayStyle: AppDisplayStyleOption = AppDisplayStyleOption.ICON_AND_TEXT,
    val recentAppsIconColor: IconColorOption = IconColorOption.ORIGINAL,

    val allAppsLayout: AppSectionLayoutOption = AppSectionLayoutOption.LIST,
    val allAppsDisplayStyle: AppDisplayStyleOption = AppDisplayStyleOption.ICON_AND_TEXT,
    val allAppsIconColor: IconColorOption = IconColorOption.ORIGINAL,

    // Sidebar customization
    val sidebarActiveScale: Float = 1.4f,
    val sidebarPopOutDp: Int = 16,
    val sidebarWaveSpread: Float = 1.5f,
    val fastScrollerActiveItemScale: Float = 1.06f
)
