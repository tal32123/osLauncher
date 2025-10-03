package com.talauncher.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.talauncher.R
import com.talauncher.ui.insights.InsightsScreen
import com.talauncher.ui.insights.InsightsViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.settings_tab_general),
        stringResource(R.string.settings_tab_app_sections),
        stringResource(R.string.settings_tab_ui_theme),
        stringResource(R.string.settings_tab_distracting_apps),
        stringResource(R.string.settings_tab_usage_insights)
    )
    var editingApp by remember { mutableStateOf<com.talauncher.data.model.InstalledApp?>(null) }
    var editingTimeLimit by remember { mutableStateOf(uiState.defaultTimeLimitMinutes) }
    var editingUsesDefault by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val currentWallpaperPath by rememberUpdatedState(uiState.customWallpaperPath)

    val pickWallpaperLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            runCatching {
                currentWallpaperPath?.let { existing ->
                    context.contentResolver.releasePersistableUriPermission(
                        Uri.parse(existing),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.updateCustomWallpaper(it.toString())
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab != 3) {
            viewModel.clearSearchQuery()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.settings),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(bottom = 16.dp)
                .testTag("settings_title")
        )

        ScrollableTabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    modifier = Modifier.testTag("settings_tab_$title"),
                    text = {
                        Text(
                            text = title,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            0 -> GeneralSettingsScreen(
                enableTimeLimitPrompt = uiState.enableTimeLimitPrompt,
                onToggleTimeLimitPrompt = viewModel::toggleTimeLimitPrompt,
                showPhoneAction = uiState.showPhoneAction,
                onToggleShowPhoneAction = viewModel::toggleShowPhoneAction,
                showMessageAction = uiState.showMessageAction,
                onToggleShowMessageAction = viewModel::toggleShowMessageAction,
                showWhatsAppAction = uiState.showWhatsAppAction,
                onToggleShowWhatsAppAction = viewModel::toggleShowWhatsAppAction,
                weatherDisplay = uiState.weatherDisplay,
                onUpdateWeatherDisplay = viewModel::updateWeatherDisplay,
                weatherTemperatureUnit = uiState.weatherTemperatureUnit,
                onUpdateWeatherTemperatureUnit = viewModel::updateWeatherTemperatureUnit,
                permissionsHelper = viewModel.permissionsHelper,
                buildCommitHash = uiState.buildCommitHash,
                buildBranch = uiState.buildBranch,
                buildTime = uiState.buildTime
            )
            1 -> AppSectionsSettingsScreen(
                pinnedAppsLayout = uiState.pinnedAppsLayout,
                onUpdatePinnedAppsLayout = viewModel::updatePinnedAppsLayout,
                pinnedAppsDisplayStyle = uiState.pinnedAppsDisplayStyle,
                onUpdatePinnedAppsDisplayStyle = viewModel::updatePinnedAppsDisplayStyle,
                pinnedAppsIconColor = uiState.pinnedAppsIconColor,
                onUpdatePinnedAppsIconColor = viewModel::updatePinnedAppsIconColor,
                recentAppsLayout = uiState.recentAppsLayout,
                onUpdateRecentAppsLayout = viewModel::updateRecentAppsLayout,
                recentAppsDisplayStyle = uiState.recentAppsDisplayStyle,
                onUpdateRecentAppsDisplayStyle = viewModel::updateRecentAppsDisplayStyle,
                recentAppsIconColor = uiState.recentAppsIconColor,
                onUpdateRecentAppsIconColor = viewModel::updateRecentAppsIconColor,
                recentAppsLimit = uiState.recentAppsLimit,
                onUpdateRecentAppsLimit = viewModel::updateRecentAppsLimit,
                allAppsLayout = uiState.allAppsLayout,
                onUpdateAllAppsLayout = viewModel::updateAllAppsLayout,
                allAppsDisplayStyle = uiState.allAppsDisplayStyle,
                onUpdateAllAppsDisplayStyle = viewModel::updateAllAppsDisplayStyle,
                allAppsIconColor = uiState.allAppsIconColor,
                onUpdateAllAppsIconColor = viewModel::updateAllAppsIconColor
            )
            2 -> UIThemeSettingsScreen(
                backgroundColor = uiState.backgroundColor,
                onUpdateBackgroundColor = viewModel::updateBackgroundColor,
                showWallpaper = uiState.showWallpaper,
                onToggleShowWallpaper = viewModel::updateShowWallpaper,
                colorPalette = uiState.colorPalette,
                onUpdateColorPalette = viewModel::updateColorPalette,
                appIconStyle = uiState.appIconStyle,
                onUpdateAppIconStyle = viewModel::updateAppIconStyle,
                customColorOption = uiState.customColorOption,
                onUpdateCustomColorOption = viewModel::updateCustomColorOption,
                customPrimaryColor = uiState.customPrimaryColor,
                onUpdateCustomPrimaryColor = viewModel::updateCustomPrimaryColor,
                customSecondaryColor = uiState.customSecondaryColor,
                onUpdateCustomSecondaryColor = viewModel::updateCustomSecondaryColor,
                themeMode = uiState.themeMode,
                onUpdateThemeMode = viewModel::updateThemeMode,
                wallpaperBlurAmount = uiState.wallpaperBlurAmount,
                onUpdateWallpaperBlur = viewModel::updateWallpaperBlur,
                backgroundOpacity = uiState.backgroundOpacity,
                onUpdateBackgroundOpacity = viewModel::updateBackgroundOpacity,
                customWallpaperPath = uiState.customWallpaperPath,
                onPickCustomWallpaper = {
                    pickWallpaperLauncher.launch(arrayOf("image/*"))
                },
                enableGlassmorphism = uiState.enableGlassmorphism,
                onToggleGlassmorphism = viewModel::updateGlassmorphism,
                uiDensity = uiState.uiDensity,
                onUpdateUiDensity = viewModel::updateUiDensity,
                enableAnimations = uiState.enableAnimations,
                onToggleAnimations = viewModel::updateAnimationsEnabled
            )
            3 -> DistractingAppsSettingsScreen(
                uiState = uiState,
                viewModel = viewModel,
                onEditApp = { app, timeLimit, usesDefault ->
                    editingApp = app
                    editingTimeLimit = timeLimit
                    editingUsesDefault = usesDefault
                }
            )
            4 -> {
                val insightsViewModel: InsightsViewModel = viewModel {
                    InsightsViewModel(viewModel.usageStatsHelper, viewModel.permissionsHelper)
                }
                InsightsScreen(
                    onNavigateBack = onNavigateBack,
                    viewModel = insightsViewModel
                )
            }
        }

        editingApp?.let { app ->
            EditTimeLimitDialog(
                appName = app.appName,
                defaultMinutes = uiState.defaultTimeLimitMinutes,
                initialMinutes = editingTimeLimit,
                isUsingDefault = editingUsesDefault,
                onUseDefault = {
                    viewModel.updateAppTimeLimit(app.packageName, null)
                    editingApp = null
                },
                onConfirm = { minutes ->
                    viewModel.updateAppTimeLimit(app.packageName, minutes)
                    editingApp = null
                },
                onDismiss = { editingApp = null }
            )
        }
    }
}
