package com.talauncher.ui.onboarding

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talauncher.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class OnboardingViewModel(
    private val context: Context,
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
        val hasUsageStats = hasUsageStatsPermission()
        val isDefaultLauncher = isDefaultLauncher()

        _uiState.value = _uiState.value.copy(
            hasUsageStatsPermission = hasUsageStats,
            isDefaultLauncher = isDefaultLauncher,
            allPermissionsGranted = hasUsageStats
        )
    }

    private fun hasUsageStatsPermission(): Boolean {
        return try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val time = System.currentTimeMillis()
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                time - 1000 * 60 * 60 * 24, // 24 hours ago
                time
            )
            stats != null && stats.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    private fun isDefaultLauncher(): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val resolveInfo: ResolveInfo? = context.packageManager.resolveActivity(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            resolveInfo?.activityInfo?.packageName == context.packageName
        } catch (e: Exception) {
            false
        }
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