package com.talauncher.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talauncher.data.model.AppIconStyleOption
import com.talauncher.data.model.ThemeModeOption
import com.talauncher.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun goToNextStep() {
        _uiState.value = _uiState.value.copy(currentStepIndex = _uiState.value.currentStepIndex + 1)
    }

    fun goToPreviousStep() {
        _uiState.value = _uiState.value.copy(currentStepIndex = (_uiState.value.currentStepIndex - 1).coerceAtLeast(0))
    }

    fun setThemeMode(mode: ThemeModeOption) {
        viewModelScope.launch {
            settingsRepository.updateThemeMode(mode)
            _uiState.value = _uiState.value.copy(selectedThemeMode = mode)
        }
    }

    fun setAppIconStyle(style: AppIconStyleOption) {
        viewModelScope.launch {
            settingsRepository.updateAppIconStyle(style)
            _uiState.value = _uiState.value.copy(selectedAppIconStyle = style)
        }
    }

    fun setShowWallpaper(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateShowWallpaper(show)
            _uiState.value = _uiState.value.copy(showWallpaper = show)
        }
    }

    fun setWallpaperBlur(amount: Float) {
        viewModelScope.launch {
            settingsRepository.updateWallpaperBlurAmount(amount)
            _uiState.value = _uiState.value.copy(wallpaperBlurAmount = amount)
        }
    }

    fun setBackgroundOpacity(value: Float) {
        viewModelScope.launch {
            settingsRepository.updateBackgroundOpacity(value)
            _uiState.value = _uiState.value.copy(backgroundOpacity = value)
        }
    }

    fun setBackgroundColor(value: String) {
        viewModelScope.launch {
            settingsRepository.updateBackgroundColor(value)
            _uiState.value = _uiState.value.copy(backgroundColor = value)
        }
    }

    fun setCustomWallpaper(path: String?) {
        viewModelScope.launch {
            settingsRepository.updateCustomWallpaper(path)
            _uiState.value = _uiState.value.copy(customWallpaperPath = path)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsRepository.completeOnboarding()
            _uiState.value = _uiState.value.copy(isOnboardingCompleted = true)
        }
    }
}

data class OnboardingUiState(
    val currentStepIndex: Int = 0,
    val isOnboardingCompleted: Boolean = false,
    val selectedThemeMode: ThemeModeOption = ThemeModeOption.SYSTEM,
    val selectedAppIconStyle: AppIconStyleOption = AppIconStyleOption.ORIGINAL,
    val showWallpaper: Boolean = true,
    val wallpaperBlurAmount: Float = 0f,
    val backgroundOpacity: Float = 1f,
    val backgroundColor: String = "system",
    val customWallpaperPath: String? = null
)
