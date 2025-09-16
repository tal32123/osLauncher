package com.talauncher.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talauncher.data.model.AppUsage
import com.talauncher.utils.PermissionsHelper
import com.talauncher.utils.UsageStatsHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class InsightsViewModel(
    private val usageStatsHelper: UsageStatsHelper,
    private val permissionsHelper: PermissionsHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        loadUsageStats()
    }

    fun loadUsageStats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            if (!permissionsHelper.hasUsageStatsPermission()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasPermission = false
                )
                return@launch
            }

            val usageStats = usageStatsHelper.getTodayUsageStats()

            val appUsageWithNames = usageStats.mapNotNull { usage ->
                val appName = usageStatsHelper.getAppName(usage.packageName)
                if (appName != null) {
                    AppUsageDisplay(
                        packageName = usage.packageName,
                        appName = appName,
                        timeInForeground = usage.timeInForeground,
                        formattedTime = formatTime(usage.timeInForeground)
                    )
                } else null
            }.filter { it.timeInForeground > 0 }
                .sortedByDescending { it.timeInForeground }

            val totalTime = appUsageWithNames.sumOf { it.timeInForeground }

            _uiState.value = _uiState.value.copy(
                appUsageList = appUsageWithNames,
                totalScreenTime = formatTime(totalTime),
                isLoading = false,
                hasPermission = true
            )
        }
    }

    private fun formatTime(milliseconds: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "< 1m"
        }
    }

    fun refreshData() {
        loadUsageStats()
    }
}

data class InsightsUiState(
    val appUsageList: List<AppUsageDisplay> = emptyList(),
    val totalScreenTime: String = "0m",
    val isLoading: Boolean = false,
    val hasPermission: Boolean = true
)

data class AppUsageDisplay(
    val packageName: String,
    val appName: String,
    val timeInForeground: Long,
    val formattedTime: String
)