package com.talauncher.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talauncher.data.model.ColorPaletteOption
import com.talauncher.data.model.ThemeModeOption
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

                // Only update isOnboardingCompleted from database if current value is false
                // This prevents database flow from overwriting the immediate navigation trigger
                val currentCompleted = _uiState.value.isOnboardingCompleted
                val newCompleted = if (currentCompleted) currentCompleted else isOnboardingCompleted

                android.util.Log.d("MainViewModel", "Database flow update: isOnboardingCompleted=$isOnboardingCompleted, current=$currentCompleted, using=$newCompleted")

                _uiState.value = _uiState.value.copy(
                    isOnboardingCompleted = newCompleted,
                    colorPalette = uiSettings.colorPalette,
                    customColorOption = settings?.customColorOption,
                    themeMode = uiSettings.themeMode,
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
        android.util.Log.d("MainViewModel", "===== onOnboardingCompleted() CALLED =====")
        android.util.Log.d("MainViewModel", "Current isOnboardingCompleted: ${_uiState.value.isOnboardingCompleted}")

        // Update state with timestamp to force emission even if other values are the same
        // This ensures MainActivity receives the update and navigates away from onboarding
        _uiState.value = _uiState.value.copy(
            isOnboardingCompleted = true,
            navigationTimestamp = System.currentTimeMillis()
        )

        android.util.Log.d("MainViewModel", "New state: isOnboardingCompleted=true, timestamp=${_uiState.value.navigationTimestamp}")
        android.util.Log.d("MainViewModel", "===== onOnboardingCompleted() END =====")
    }
}

data class MainUiState(
    val isOnboardingCompleted: Boolean = false,
    val isLoading: Boolean = true,
    val colorPalette: ColorPaletteOption = ColorPaletteOption.DEFAULT,
    val customColorOption: String? = null,
    val themeMode: ThemeModeOption = ThemeModeOption.SYSTEM,
    val uiSettings: UiSettings = UiSettings(),
    // Timestamp to force state emission when onboarding completes
    val navigationTimestamp: Long = 0L
)