package com.talauncher.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.talauncher.data.repository.AppRepository
import com.talauncher.data.repository.SessionRepository
import com.talauncher.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val settingsRepository: SettingsRepository,
    private val appRepository: AppRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        checkOnboardingStatus()
        refreshInstalledApps()
    }

    private fun checkOnboardingStatus() {
        viewModelScope.launch {
            val isOnboardingCompleted = settingsRepository.isOnboardingCompleted()
            _uiState.value = _uiState.value.copy(
                isOnboardingCompleted = isOnboardingCompleted,
                isLoading = false
            )
        }
    }

    fun refreshInstalledApps() {
        viewModelScope.launch {
            appRepository.syncInstalledApps()
        }
    }

    fun emitExpiredSessions() {
        viewModelScope.launch {
            sessionRepository.emitExpiredSessions()
        }
    }

    fun onOnboardingCompleted() {
        _uiState.value = _uiState.value.copy(isOnboardingCompleted = true)
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val appRepository: AppRepository,
        private val sessionRepository: SessionRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(settingsRepository, appRepository, sessionRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

data class MainUiState(
    val isOnboardingCompleted: Boolean = false,
    val isLoading: Boolean = true
)