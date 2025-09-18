package com.talauncher.ui.onboarding

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talauncher.data.repository.SettingsRepository
import com.talauncher.utils.PermissionsHelper
import com.talauncher.utils.UsageStatsHelper
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
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
            checkPermissions()

            while (!uiState.value.allPermissionsGranted && isActive) {
                delay(2000) // Check every 2 seconds
                checkPermissions()
            }

            if (uiState.value.allPermissionsGranted) {
                cancel()
                return@launch
            }
        }
    }

    private fun checkPermissions() {
        val hasUsageStats = permissionsHelper.hasUsageStatsPermission()
        val isDefaultLauncher = usageStatsHelper.isDefaultLauncher()
        val hasSystemAlertWindow = permissionsHelper.hasSystemAlertWindowPermission()
        val hasNotifications = permissionsHelper.hasNotificationPermission()

        _uiState.value = _uiState.value.copy(
            hasUsageStatsPermission = hasUsageStats,
            isDefaultLauncher = isDefaultLauncher,
            hasSystemAlertWindowPermission = hasSystemAlertWindow,
            hasNotificationPermission = hasNotifications,
            allPermissionsGranted = hasUsageStats &&
                isDefaultLauncher &&
                hasSystemAlertWindow &&
                hasNotifications
        )
    }

    fun requestSystemAlertWindowPermission() {
        permissionsHelper.requestSystemAlertWindowPermission()
        checkPermissions()
    }

    fun requestNotificationPermission(activity: Activity?) {
        permissionsHelper.requestNotificationPermission(activity)
        checkPermissions()
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
    val hasSystemAlertWindowPermission: Boolean = false,
    val hasNotificationPermission: Boolean = false,
    val allPermissionsGranted: Boolean = false,
    val isOnboardingCompleted: Boolean = false
)
