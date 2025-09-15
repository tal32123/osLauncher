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
    private val settingsRepository: SettingsRepository
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
                appRepository.getEssentialApps(),
                settingsRepository.getSettings()
            ) { essentialApps, settings ->
                _uiState.value = _uiState.value.copy(
                    essentialApps = essentialApps,
                    isFocusModeEnabled = settings?.isFocusModeEnabled ?: false,
                    showTime = settings?.showTimeOnHomeScreen ?: true,
                    showDate = settings?.showDateOnHomeScreen ?: true
                )
            }
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
            appRepository.launchApp(packageName)
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
}

data class HomeUiState(
    val essentialApps: List<AppInfo> = emptyList(),
    val currentTime: String = "",
    val currentDate: String = "",
    val isFocusModeEnabled: Boolean = false,
    val showTime: Boolean = true,
    val showDate: Boolean = true,
    val isLoading: Boolean = false
)