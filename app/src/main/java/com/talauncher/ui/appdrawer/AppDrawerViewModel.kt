package com.talauncher.ui.appdrawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talauncher.data.model.InstalledApp
import com.talauncher.data.repository.AppRepository
import com.talauncher.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppDrawerViewModel(
    private val appRepository: AppRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppDrawerUiState())
    val uiState: StateFlow<AppDrawerUiState> = _uiState.asStateFlow()

    private var allApps: List<InstalledApp> = emptyList()
    private var distractingAppPackages: Set<String> = emptySet()

    init {
        loadApps()
        observeDistractingApps()
        observeSettings()
    }

    private fun loadApps() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            allApps = appRepository.getInstalledApps()
            filterApps(_uiState.value.searchQuery)

            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private fun observeDistractingApps() {
        viewModelScope.launch {
            appRepository.getDistractingApps().collect { distractingApps ->
                distractingAppPackages = distractingApps.map { it.packageName }.toSet()
                filterApps(_uiState.value.searchQuery)
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.getSettings().collect { settings ->
                val isFocusModeEnabled = settings?.isFocusModeEnabled ?: false
                _uiState.value = _uiState.value.copy(isFocusModeEnabled = isFocusModeEnabled)
                filterApps(_uiState.value.searchQuery)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        filterApps(query)
    }

    private fun filterApps(query: String) {
        val isFocusModeEnabled = _uiState.value.isFocusModeEnabled

        val filteredApps = allApps.filter { app ->
            val matchesSearch = if (query.isBlank()) {
                true
            } else {
                app.appName.contains(query, ignoreCase = true)
            }

            val isVisible = if (isFocusModeEnabled) {
                !distractingAppPackages.contains(app.packageName)
            } else {
                true
            }

            matchesSearch && isVisible
        }

        _uiState.value = _uiState.value.copy(filteredApps = filteredApps)
    }

    fun launchApp(packageName: String) {
        viewModelScope.launch {
            if (_uiState.value.isFocusModeEnabled && distractingAppPackages.contains(packageName)) {
                _uiState.value = _uiState.value.copy(
                    blockedApp = packageName,
                    showFrictionDialog = true
                )
            } else {
                appRepository.launchApp(packageName)
            }
        }
    }

    fun dismissFrictionDialog() {
        _uiState.value = _uiState.value.copy(
            blockedApp = null,
            showFrictionDialog = false,
            frictionReason = ""
        )
    }

    fun updateFrictionReason(reason: String) {
        _uiState.value = _uiState.value.copy(frictionReason = reason)
    }

    fun proceedWithBlockedApp() {
        viewModelScope.launch {
            _uiState.value.blockedApp?.let { packageName ->
                appRepository.launchApp(packageName)
                dismissFrictionDialog()
            }
        }
    }

    fun clearSearch() {
        updateSearchQuery("")
    }
}

data class AppDrawerUiState(
    val filteredApps: List<InstalledApp> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isFocusModeEnabled: Boolean = false,
    val showFrictionDialog: Boolean = false,
    val blockedApp: String? = null,
    val frictionReason: String = ""
)