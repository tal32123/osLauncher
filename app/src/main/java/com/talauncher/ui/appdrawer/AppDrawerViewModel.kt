package com.talauncher.ui.appdrawer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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

class AppDrawerViewModel(
    private val appRepository: AppRepository,
    private val settingsRepository: SettingsRepository,
    private val onLaunchApp: ((String, Int?) -> Unit)? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppDrawerUiState())
    val uiState: StateFlow<AppDrawerUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                appRepository.getAllVisibleApps(),
                appRepository.getHiddenApps(),
                settingsRepository.getSettings()
            ) { visibleApps, hiddenApps, settings ->
                _uiState.value = _uiState.value.copy(
                    allApps = visibleApps,
                    hiddenApps = hiddenApps,
                    isFocusModeEnabled = settings?.isFocusModeEnabled ?: false
                )
            }.collect { /* Data collection handled in the combine block */ }
        }
    }

    fun launchApp(packageName: String) {
        viewModelScope.launch {
            // Check if we should show time limit prompt first
            if (appRepository.shouldShowTimeLimitPrompt(packageName)) {
                _uiState.value = _uiState.value.copy(
                    showTimeLimitDialog = true,
                    selectedAppForTimeLimit = packageName
                )
                return@launch
            }

            // Try to launch the app through repository (handles friction checks)
            val launched = appRepository.launchApp(packageName)
            if (launched) {
                // App was launched successfully, use callback if available
                onLaunchApp?.invoke(packageName, null)
            } else {
                // Show friction barrier for distracting app
                _uiState.value = _uiState.value.copy(
                    showFrictionDialog = true,
                    selectedAppForFriction = packageName
                )
            }
        }
    }

    fun dismissFrictionDialog() {
        _uiState.value = _uiState.value.copy(
            showFrictionDialog = false,
            selectedAppForFriction = null
        )
    }

    fun launchAppWithReason(packageName: String, reason: String) {
        viewModelScope.launch {
            // Log the reason for analytics/insights if needed
            appRepository.launchApp(packageName, bypassFriction = true)
            onLaunchApp?.invoke(packageName, null)
            dismissFrictionDialog()
        }
    }

    fun dismissTimeLimitDialog() {
        _uiState.value = _uiState.value.copy(
            showTimeLimitDialog = false,
            selectedAppForTimeLimit = null
        )
    }

    fun launchAppWithTimeLimit(packageName: String, durationMinutes: Int) {
        viewModelScope.launch {
            appRepository.launchApp(packageName, plannedDuration = durationMinutes)
            onLaunchApp?.invoke(packageName, durationMinutes)
            dismissTimeLimitDialog()
        }
    }

    fun dismissMathChallengeDialog() {
        _uiState.value = _uiState.value.copy(
            showMathChallengeDialog = false,
            selectedAppForMathChallenge = null
        )
    }

    fun showMathChallengeForApp(packageName: String) {
        viewModelScope.launch {
            if (appRepository.shouldShowMathChallenge(packageName)) {
                _uiState.value = _uiState.value.copy(
                    showMathChallengeDialog = true,
                    selectedAppForMathChallenge = packageName
                )
            }
        }
    }

    fun onMathChallengeCompleted(packageName: String) {
        viewModelScope.launch {
            appRepository.endSessionForApp(packageName)
            dismissMathChallengeDialog()
        }
    }

    fun pinApp(packageName: String) {
        viewModelScope.launch {
            appRepository.pinApp(packageName)
        }
    }

    fun unpinApp(packageName: String) {
        viewModelScope.launch {
            appRepository.unpinApp(packageName)
        }
    }

    fun hideApp(packageName: String) {
        viewModelScope.launch {
            appRepository.hideApp(packageName)
        }
    }

    fun unhideApp(packageName: String) {
        viewModelScope.launch {
            appRepository.unhideApp(packageName)
        }
    }

    fun showAppActionDialog(app: AppInfo) {
        _uiState.value = _uiState.value.copy(selectedAppForAction = app)
    }

    fun dismissAppActionDialog() {
        _uiState.value = _uiState.value.copy(selectedAppForAction = null)
    }

    fun openAppInfo(context: Context, packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            // Handle error - could show a toast or log
        }
    }

    fun uninstallApp(context: Context, packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE)
            intent.data = Uri.parse("package:$packageName")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            // Handle error - could show a toast or log
        }
    }
}

data class AppDrawerUiState(
    val allApps: List<AppInfo> = emptyList(),
    val hiddenApps: List<AppInfo> = emptyList(),
    val isFocusModeEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val selectedAppForAction: AppInfo? = null,
    val showFrictionDialog: Boolean = false,
    val selectedAppForFriction: String? = null,
    val showTimeLimitDialog: Boolean = false,
    val selectedAppForTimeLimit: String? = null,
    val showMathChallengeDialog: Boolean = false,
    val selectedAppForMathChallenge: String? = null
)