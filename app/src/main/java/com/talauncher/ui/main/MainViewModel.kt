package com.talauncher.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talauncher.data.model.ColorPaletteOption
import com.talauncher.data.repository.AppRepository
import com.talauncher.data.repository.SettingsRepository
import com.talauncher.ui.theme.UiSettings
import com.talauncher.ui.theme.toUiSettingsOrDefault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.talauncher.utils.IdlingResourceHelper

class MainViewModel(
    private val settingsRepository: SettingsRepository,
    private val appRepository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        observeSettingsAndOnboarding()
        syncApps()
    }

    private fun observeSettingsAndOnboarding() {
        viewModelScope.launch {
            settingsRepository.getSettings().collect { settings ->
                val uiSettings = settings.toUiSettingsOrDefault()
                val isOnboardingCompleted = settings?.isOnboardingCompleted ?: false
                _uiState.value = _uiState.value.copy(
                    isOnboardingCompleted = isOnboardingCompleted,
                    colorPalette = uiSettings.colorPalette,
                    uiSettings = uiSettings,
                    isLoading = false
                )
            }
        }
    }

    private fun syncApps() {
        viewModelScope.launch(Dispatchers.IO) {
            IdlingResourceHelper.increment()
            try {
                // Add delay to allow UI to initialize first for tests
                kotlinx.coroutines.delay(200)
                appRepository.syncInstalledApps()
            } finally {
                IdlingResourceHelper.decrement()
            }
        }
    }

    fun onOnboardingCompleted() {
        _uiState.value = _uiState.value.copy(isOnboardingCompleted = true)
    }
}

data class MainUiState(
    val isOnboardingCompleted: Boolean = false,
    val isLoading: Boolean = true,
    val colorPalette: ColorPaletteOption = ColorPaletteOption.DEFAULT,
    val uiSettings: UiSettings = UiSettings()
)