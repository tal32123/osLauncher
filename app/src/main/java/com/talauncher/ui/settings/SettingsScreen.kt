package com.talauncher.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.talauncher.R
import com.talauncher.ui.insights.InsightsScreen
import com.talauncher.ui.insights.InsightsViewModel
import com.talauncher.utils.UsageStatsHelper
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("General", "Essential Apps", "Distracting Apps", "Usage Insights")

    // Clear search when navigating away from app selection tabs
    LaunchedEffect(selectedTab) {
        if (selectedTab != 1 && selectedTab != 2) {
            viewModel.clearSearchQuery()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        ScrollableTabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
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
            0 -> GeneralSettings(
                enableTimeLimitPrompt = uiState.enableTimeLimitPrompt,
                onToggleTimeLimitPrompt = viewModel::toggleTimeLimitPrompt,
                enableMathChallenge = uiState.enableMathChallenge,
                onToggleMathChallenge = viewModel::toggleMathChallenge,
                mathDifficulty = uiState.mathDifficulty,
                onUpdateMathDifficulty = viewModel::updateMathDifficulty,
                recentAppsLimit = uiState.recentAppsLimit,
                onUpdateRecentAppsLimit = viewModel::updateRecentAppsLimit,
                sessionExpiryCountdownSeconds = uiState.sessionExpiryCountdownSeconds,
                onUpdateSessionExpiryCountdown = viewModel::updateSessionExpiryCountdown,
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
                permissionsHelper = viewModel.permissionsHelper
            )
            1 -> AppSelectionTab(
                title = "Essential Apps",
                subtitle = "Apps that will appear on your home screen",
                apps = viewModel.getFilteredApps(),
                selectedApps = uiState.pinnedApps.map { it.packageName }.toSet(),
                onToggleApp = viewModel::toggleEssentialApp,
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = viewModel::updateSearchQuery,
                isLoading = uiState.isLoading
            )
            2 -> AppSelectionTab(
                title = "Distracting Apps",
                subtitle = "Apps that require friction barriers and prompts",
                apps = viewModel.getFilteredApps(),
                selectedApps = uiState.distractingApps.map { it.packageName }.toSet(),
                onToggleApp = viewModel::toggleDistractingApp,
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = viewModel::updateSearchQuery,
                isLoading = uiState.isLoading
            )
            3 -> {
                val insightsViewModel: InsightsViewModel = viewModel {
                    InsightsViewModel(viewModel.usageStatsHelper, viewModel.permissionsHelper)
                }
                InsightsScreen(
                    onNavigateBack = onNavigateBack,
                    viewModel = insightsViewModel
                )
            }
        }
    }
}

@Composable
fun GeneralSettings(
    enableTimeLimitPrompt: Boolean,
    onToggleTimeLimitPrompt: () -> Unit,
    enableMathChallenge: Boolean,
    onToggleMathChallenge: () -> Unit,
    mathDifficulty: String,
    onUpdateMathDifficulty: (String) -> Unit,
    recentAppsLimit: Int,
    onUpdateRecentAppsLimit: (Int) -> Unit,
    sessionExpiryCountdownSeconds: Int,
    onUpdateSessionExpiryCountdown: (Int) -> Unit,
    showPhoneAction: Boolean,
    onToggleShowPhoneAction: () -> Unit,
    showMessageAction: Boolean,
    onToggleShowMessageAction: () -> Unit,
    showWhatsAppAction: Boolean,
    onToggleShowWhatsAppAction: () -> Unit,
    weatherDisplay: String,
    onUpdateWeatherDisplay: (String) -> Unit,
    weatherTemperatureUnit: String,
    onUpdateWeatherTemperatureUnit: (String) -> Unit,
    permissionsHelper: com.talauncher.utils.PermissionsHelper
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Focus & Productivity",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )


                    SettingItem(
                        title = "Time Limit Prompts",
                        subtitle = "Ask how long you'll use distracting apps",
                        checked = enableTimeLimitPrompt,
                        onCheckedChange = { onToggleTimeLimitPrompt() }
                    )

                    var sliderValue by remember(sessionExpiryCountdownSeconds) {
                        mutableStateOf(sessionExpiryCountdownSeconds.toFloat())
                    }
                    var recentAppsValue by remember(recentAppsLimit) {
                        mutableStateOf(recentAppsLimit.toFloat())
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Countdown after timer",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Slider(
                            value = sliderValue,
                            onValueChange = { sliderValue = it },
                            valueRange = 0f..15f,
                            steps = 14,
                            onValueChangeFinished = {
                                onUpdateSessionExpiryCountdown(sliderValue.roundToInt())
                            },
                            enabled = enableTimeLimitPrompt
                        )
                        Text(
                            text = "${sliderValue.roundToInt()} second${if (sliderValue.roundToInt() == 1) "" else "s"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_recent_apps_limit_title),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Slider(
                            value = recentAppsValue,
                            onValueChange = { recentAppsValue = it },
                            valueRange = 0f..10f,
                            steps = 9,
                            onValueChangeFinished = {
                                onUpdateRecentAppsLimit(recentAppsValue.roundToInt())
                            }
                        )
                        val recentCount = recentAppsValue.roundToInt()
                        val recentSummary = when {
                            recentCount == 0 -> stringResource(R.string.settings_recent_apps_limit_hidden)
                            recentCount == 1 -> stringResource(R.string.settings_recent_apps_limit_summary_single)
                            else -> stringResource(R.string.settings_recent_apps_limit_summary_plural, recentCount)
                        }
                        Text(
                            text = recentSummary,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    SettingItem(
                        title = "Math Challenge to Close",
                        subtitle = "Solve math problems to close distracting apps",
                        checked = enableMathChallenge,
                        onCheckedChange = { onToggleMathChallenge() }
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Contact Actions",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    SettingItem(
                        title = "Show Phone Action",
                        subtitle = "Show a button to call the contact",
                        checked = showPhoneAction,
                        onCheckedChange = { onToggleShowPhoneAction() }
                    )

                    SettingItem(
                        title = "Show Message Action",
                        subtitle = "Show a button to message the contact",
                        checked = showMessageAction,
                        onCheckedChange = { onToggleShowMessageAction() }
                    )

                    SettingItem(
                        title = "Show WhatsApp Action",
                        subtitle = "Show a button to message the contact on WhatsApp",
                        checked = showWhatsAppAction,
                        onCheckedChange = { onToggleShowWhatsAppAction() }
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Weather",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val context = androidx.compose.ui.platform.LocalContext.current
                    val permissionState by permissionsHelper.permissionState.collectAsState()

                    Text(
                        text = "Display Options",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("off", "daily", "hourly").forEach { option ->
                            FilterChip(
                                selected = weatherDisplay == option,
                                onClick = {
                                    if (option != "off" && !permissionState.hasLocation) {
                                        if (context is androidx.activity.ComponentActivity) {
                                            permissionsHelper.requestPermission(context, com.talauncher.utils.PermissionType.LOCATION)
                                        }
                                    }
                                    onUpdateWeatherDisplay(option)
                                },
                                label = {
                                    Text(when(option) {
                                        "off" -> "Off"
                                        "daily" -> "Daily"
                                        "hourly" -> "Hourly"
                                        else -> option
                                    })
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (weatherDisplay != "off" && !permissionState.hasLocation) {
                        Text(
                            text = "Location permission required for weather",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Temperature Unit",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("celsius" to "°C", "fahrenheit" to "°F").forEach { (option, label) ->
                            FilterChip(
                                selected = weatherTemperatureUnit == option,
                                onClick = { onUpdateWeatherTemperatureUnit(option) },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        if (enableMathChallenge) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Math Challenge Settings",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "Difficulty Level",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("easy", "medium", "hard").forEach { difficulty ->
                                FilterChip(
                                    selected = mathDifficulty == difficulty,
                                    onClick = { onUpdateMathDifficulty(difficulty) },
                                    label = {
                                        Text(difficulty.replaceFirstChar { it.uppercase() })
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Build Information",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    uiState.buildCommitHash?.let { hash ->
                        Text(
                            text = "Commit: ${hash.take(8)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }

                    uiState.buildBranch?.let { branch ->
                        Text(
                            text = "Branch: $branch",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    uiState.buildTime?.let { buildTime ->
                        Text(
                            text = "Built: $buildTime",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionTab(
    title: String,
    subtitle: String,
    apps: List<com.talauncher.data.model.InstalledApp>,
    selectedApps: Set<String>,
    onToggleApp: (String) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isLoading: Boolean
) {
    Column {
        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear"
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(apps) { app ->
                    AppSelectionItem(
                        app = app,
                        isSelected = selectedApps.contains(app.packageName),
                        onToggle = { onToggleApp(app.packageName) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionItem(
    app: com.talauncher.data.model.InstalledApp,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = app.appName,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() }
            )
        }
    }
}


