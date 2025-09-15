package com.talauncher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talauncher.data.model.AppInfo
import com.talauncher.data.model.LauncherSettings
import com.talauncher.data.repository.AppRepository
import com.talauncher.data.repository.SettingsRepository
import com.talauncher.data.repository.SessionRepository
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
    private val onLaunchApp: ((String, Int?) -> Unit)? = null,
    private val sessionRepository: SessionRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())

    init {
        observeData()
        updateTime()
        checkExpiredSessions()
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

    private fun checkExpiredSessions() {
        if (sessionRepository == null) return

        viewModelScope.launch {
            sessionRepository.getActiveSessions().collect { sessions ->
                // Check for expired sessions that require math challenges
                sessions.forEach { session ->
                    if (sessionRepository.isSessionExpired(session)) {
                        val shouldShowMathChallenge = appRepository.shouldShowMathChallenge(session.packageName)
                        if (shouldShowMathChallenge) {
                            // Show math challenge for the first expired session
                            // (only show one at a time to avoid overwhelming user)
                            if (!_uiState.value.showMathChallengeDialog) {
                                _uiState.value = _uiState.value.copy(
                                    showMathChallengeDialog = true,
                                    selectedAppForMathChallenge = session.packageName
                                )
                                return@forEach // Exit after showing first challenge
                            }
                        } else {
                            // Auto-end session if no math challenge required
                            sessionRepository.endSessionForApp(session.packageName)
                        }
                    }
                }
            }
        }
    }

    fun checkExpiredSessionsManually() {
        checkExpiredSessions()
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
    val showTimeLimitDialog: Boolean = false,
    val selectedAppForTimeLimit: String? = null,
    val showMathChallengeDialog: Boolean = false,
    val selectedAppForMathChallenge: String? = null,
    val isLoading: Boolean = false
)