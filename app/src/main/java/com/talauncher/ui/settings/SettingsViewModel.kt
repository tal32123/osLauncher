package com.talauncher.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talauncher.data.model.AppInfo
import com.talauncher.data.model.InstalledApp
import com.talauncher.data.repository.AppRepository
import com.talauncher.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val appRepository: AppRepository,
    private val settingsRepository: SettingsRepository
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
                appRepository.getEssentialApps(),
                appRepository.getDistractingApps(),
                settingsRepository.getSettings()
            ) { essentialApps, distractingApps, settings ->
                _uiState.value = _uiState.value.copy(
                    essentialApps = essentialApps,
                    distractingApps = distractingApps,
                    isFocusModeEnabled = settings?.isFocusModeEnabled ?: false,
                    availableApps = allInstalledApps,
                    isLoading = false
                )
            }.collect { /* Data collection handled in the combine block */ }
        }
    }

    fun toggleEssentialApp(packageName: String) {
        viewModelScope.launch {
            val currentApp = appRepository.getApp(packageName)
            val installedApp = allInstalledApps.find { it.packageName == packageName }

            if (currentApp != null) {
                appRepository.updateEssentialStatus(packageName, !currentApp.isEssential)
            } else if (installedApp != null) {
                appRepository.insertApp(
                    AppInfo(
                        packageName = packageName,
                        appName = installedApp.appName,
                        isEssential = true,
                        isDistracting = false
                    )
                )
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
                        isEssential = false,
                        isDistracting = true
                    )
                )
            }
        }
    }

    fun toggleFocusMode() {
        viewModelScope.launch {
            settingsRepository.updateFocusMode(!_uiState.value.isFocusModeEnabled)
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

    fun isAppEssential(packageName: String): Boolean {
        return _uiState.value.essentialApps.any { it.packageName == packageName }
    }

    fun isAppDistracting(packageName: String): Boolean {
        return _uiState.value.distractingApps.any { it.packageName == packageName }
    }
}

data class SettingsUiState(
    val essentialApps: List<AppInfo> = emptyList(),
    val distractingApps: List<AppInfo> = emptyList(),
    val availableApps: List<InstalledApp> = emptyList(),
    val isFocusModeEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val searchQuery: String = ""
)