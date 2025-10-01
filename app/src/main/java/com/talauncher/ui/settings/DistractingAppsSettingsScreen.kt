package com.talauncher.ui.settings

import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.talauncher.R
import com.talauncher.data.model.InstalledApp
import kotlin.math.roundToInt

@Composable
fun DistractingAppsSettingsScreen(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onEditApp: (app: InstalledApp, timeLimit: Int, usesDefault: Boolean) -> Unit
) {
    val distractingAppMap = remember(uiState.distractingApps) {
        uiState.distractingApps.associateBy { it.packageName }
    }

    var defaultLimitSlider by remember(uiState.defaultTimeLimitMinutes) {
        mutableStateOf(uiState.defaultTimeLimitMinutes.toFloat())
    }

    AppSelectionTab(
        title = stringResource(R.string.settings_distracting_apps_title),
        subtitle = stringResource(R.string.settings_distracting_apps_subtitle),
        apps = viewModel.getFilteredApps(),
        selectedApps = uiState.distractingApps.map { it.packageName }.toSet(),
        onToggleApp = viewModel::toggleDistractingApp,
        searchQuery = uiState.searchQuery,
        onSearchQueryChange = viewModel::updateSearchQuery,
        isLoading = uiState.isLoading,
        headerContent = {
            DefaultTimeLimitCard(
                sliderValue = defaultLimitSlider,
                displayedMinutes = defaultLimitSlider.roundToInt(),
                onSliderValueChange = { defaultLimitSlider = it },
                onSliderChangeFinished = {
                    viewModel.updateDefaultTimeLimit(defaultLimitSlider.roundToInt())
                }
            )
        },
        timeLimitInfoProvider = { app ->
            val info = distractingAppMap[app.packageName]
            val override = info?.timeLimitMinutes
            val usesDefault = override == null
            (override ?: uiState.defaultTimeLimitMinutes) to usesDefault
        },
        onEditTimeLimit = { app ->
            val info = distractingAppMap[app.packageName]
            val override = info?.timeLimitMinutes
            val timeLimit = override ?: uiState.defaultTimeLimitMinutes
            val usesDefault = override == null
            onEditApp(app, timeLimit, usesDefault)
        }
    )
}
