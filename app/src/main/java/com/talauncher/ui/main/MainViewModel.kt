package com.talauncher.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talauncher.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        checkOnboardingStatus()
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

    fun onOnboardingCompleted() {
        _uiState.value = _uiState.value.copy(isOnboardingCompleted = true)
    }
}

data class MainUiState(
    val isOnboardingCompleted: Boolean = false,
    val isLoading: Boolean = true
)