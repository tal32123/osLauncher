package com.talauncher.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talauncher.data.model.AppInfo
import com.talauncher.data.model.InstalledApp
import com.talauncher.data.repository.AppRepository
import com.talauncher.data.repository.SettingsRepository
import com.talauncher.utils.UsageStatsHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val appRepository: AppRepository,
    private val settingsRepository: SettingsRepository,
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

            allInstalledApps = appRepository.getInstalledApps()

            combine(
                appRepository.getPinnedApps(),
                appRepository.getDistractingApps(),
                settingsRepository.getSettings()
            ) { pinnedApps, distractingApps, settings ->
                _uiState.value = _uiState.value.copy(
                    pinnedApps = pinnedApps,
                    distractingApps = distractingApps,
                    enableTimeLimitPrompt = settings?.enableTimeLimitPrompt ?: false,
                    enableMathChallenge = settings?.enableMathChallenge ?: false,
                    mathDifficulty = settings?.mathDifficulty ?: "easy",
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

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
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
    val enableTimeLimitPrompt: Boolean = false,
    val enableMathChallenge: Boolean = false,
    val mathDifficulty: String = "easy",
    val isLoading: Boolean = false,
    val searchQuery: String = ""
)