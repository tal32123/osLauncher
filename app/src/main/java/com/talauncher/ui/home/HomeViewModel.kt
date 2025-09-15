package com.talauncher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talauncher.data.model.AppInfo
import com.talauncher.data.model.LauncherSettings
import com.talauncher.data.repository.AppRepository
import com.talauncher.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HomeViewModel(
    private val appRepository: AppRepository,
    private val settingsRepository: SettingsRepository,
    private val onLaunchApp: ((String) -> Unit)? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())

    init {
        observeData()
        updateTime()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                appRepository.getPinnedApps(),
                settingsRepository.getSettings()
            ) { pinnedApps, settings ->
                _uiState.value = _uiState.value.copy(
                    pinnedApps = pinnedApps,
                    isFocusModeEnabled = settings?.isFocusModeEnabled ?: false,
                    showTime = settings?.showTimeOnHomeScreen ?: true,
                    showDate = settings?.showDateOnHomeScreen ?: true,
                    showWallpaper = settings?.showWallpaper ?: true,
                    backgroundColor = settings?.backgroundColor ?: "system"
                )
            }.collect { /* Data collection handled in the combine block */ }
        }
    }

    private fun updateTime() {
        viewModelScope.launch {
            val now = Date()
            _uiState.value = _uiState.value.copy(
                currentTime = timeFormat.format(now),
                currentDate = dateFormat.format(now)
            )
        }
    }

    fun launchApp(packageName: String) {
        if (onLaunchApp != null) {
            onLaunchApp(packageName)
        } else {
            viewModelScope.launch {
                val launched = appRepository.launchApp(packageName)
                if (!launched) {
                    // Show friction barrier for distracting app
                    _uiState.value = _uiState.value.copy(
                        showFrictionDialog = true,
                        selectedAppForFriction = packageName
                    )
                }
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
            dismissFrictionDialog()
        }
    }

    fun toggleFocusMode() {
        viewModelScope.launch {
            val newState = !_uiState.value.isFocusModeEnabled
            settingsRepository.updateFocusMode(newState)
        }
    }

    fun refreshTime() {
        updateTime()
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
}

data class HomeUiState(
    val pinnedApps: List<AppInfo> = emptyList(),
    val currentTime: String = "",
    val currentDate: String = "",
    val isFocusModeEnabled: Boolean = false,
    val showTime: Boolean = true,
    val showDate: Boolean = true,
    val showWallpaper: Boolean = true,
    val backgroundColor: String = "system",
    val showFrictionDialog: Boolean = false,
    val selectedAppForFriction: String? = null,
    val isLoading: Boolean = false
)