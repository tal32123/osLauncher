package com.talauncher.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talauncher.data.repository.SettingsRepository
import com.talauncher.utils.PermissionsHelper
import com.talauncher.utils.UsageStatsHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val permissionsHelper: PermissionsHelper,
    private val usageStatsHelper: UsageStatsHelper,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        startPermissionChecking()
    }

    private fun startPermissionChecking() {
        viewModelScope.launch {
            while (true) {
                checkPermissions()
                delay(2000) // Check every 2 seconds
            }
        }
    }

    private fun checkPermissions() {
        val hasUsageStats = permissionsHelper.hasUsageStatsPermission()
        val isDefaultLauncher = usageStatsHelper.isDefaultLauncher()

        _uiState.value = _uiState.value.copy(
            hasUsageStatsPermission = hasUsageStats,
            isDefaultLauncher = isDefaultLauncher,
            allPermissionsGranted = hasUsageStats && isDefaultLauncher
        )
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsRepository.completeOnboarding()
        }
    }
}

data class OnboardingUiState(
    val hasUsageStatsPermission: Boolean = false,
    val isDefaultLauncher: Boolean = false,
    val allPermissionsGranted: Boolean = false,
    val isOnboardingCompleted: Boolean = false
)
