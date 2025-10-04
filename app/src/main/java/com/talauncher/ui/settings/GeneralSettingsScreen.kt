package com.talauncher.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.talauncher.R
import com.talauncher.data.model.WeatherDisplayOption
import com.talauncher.data.model.WeatherTemperatureUnit
import com.talauncher.data.model.NewsCategory
import com.talauncher.data.model.NewsRefreshInterval
import com.talauncher.ui.components.CollapsibleSection
import com.talauncher.ui.components.CollapsibleSectionContainer
import com.talauncher.ui.components.SettingItem
import com.talauncher.ui.components.SettingsLazyColumn
import com.talauncher.ui.components.SliderSetting
import com.talauncher.ui.components.CheckboxItem
import com.talauncher.utils.PermissionsHelper
import kotlin.math.roundToInt

@Composable
fun GeneralSettingsScreen(
    enableTimeLimitPrompt: Boolean,
    onToggleTimeLimitPrompt: () -> Unit,
    weatherDisplay: WeatherDisplayOption,
    onUpdateWeatherDisplay: (WeatherDisplayOption) -> Unit,
    weatherTemperatureUnit: WeatherTemperatureUnit,
    onUpdateWeatherTemperatureUnit: (WeatherTemperatureUnit) -> Unit,
    showNewsWidget: Boolean,
    onToggleNewsWidget: () -> Unit,
    newsRefreshInterval: NewsRefreshInterval,
    onUpdateNewsRefreshInterval: (NewsRefreshInterval) -> Unit,
    newsSelectedCategories: Set<NewsCategory>,
    onToggleNewsCategory: (NewsCategory) -> Unit,
    permissionsHelper: PermissionsHelper,
    buildCommitHash: String?,
    buildBranch: String?,
    buildTime: String?
) {
    val sections = buildList {
        add(
            CollapsibleSection(
                id = "focus_productivity",
                title = stringResource(R.string.settings_focus_productivity)
            ) {
                FocusProductivityContent(
                    enableTimeLimitPrompt = enableTimeLimitPrompt,
                    onToggleTimeLimitPrompt = onToggleTimeLimitPrompt,
                    permissionsHelper = permissionsHelper
                )
            }
        )
        add(
            CollapsibleSection(
                id = "weather",
                title = stringResource(R.string.settings_weather)
            ) {
                WeatherContent(
                    weatherDisplay = weatherDisplay,
                    onUpdateWeatherDisplay = onUpdateWeatherDisplay,
                    weatherTemperatureUnit = weatherTemperatureUnit,
                    onUpdateWeatherTemperatureUnit = onUpdateWeatherTemperatureUnit,
                    permissionsHelper = permissionsHelper
                )
            }
        )
        add(
            CollapsibleSection(
                id = "news",
                title = "News"
            ) {
                NewsContent(
                    showNewsWidget = showNewsWidget,
                    onToggleNewsWidget = onToggleNewsWidget,
                    refreshInterval = newsRefreshInterval,
                    onUpdateRefreshInterval = onUpdateNewsRefreshInterval,
                    selectedCategories = newsSelectedCategories,
                    onToggleCategory = onToggleNewsCategory
                )
            }
        )
        if (buildCommitHash != null || buildBranch != null || buildTime != null) {
            add(
                CollapsibleSection(
                    id = "build_info",
                    title = stringResource(R.string.settings_build_information)
                ) {
                    BuildInformationContent(
                        buildCommitHash = buildCommitHash,
                        buildBranch = buildBranch,
                        buildTime = buildTime
                    )
                }
            )
        }
    }

    SettingsLazyColumn {
        item {
            CollapsibleSectionContainer(
                sections = sections,
                initialExpandedId = "focus_productivity"
            )
        }
    }
}

@Composable
private fun FocusProductivityContent(
    enableTimeLimitPrompt: Boolean,
    onToggleTimeLimitPrompt: () -> Unit,
    permissionsHelper: PermissionsHelper
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val permissionState by permissionsHelper.permissionState.collectAsState()

    SettingItem(
        title = stringResource(R.string.settings_time_limit_dialog_title),
        subtitle = stringResource(R.string.settings_time_limit_dialog_subtitle),
        checked = enableTimeLimitPrompt,
        onCheckedChange = { onToggleTimeLimitPrompt() }
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Music Widget Notification Listener Permission
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Music Widget",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (permissionState.hasNotificationListener) {
                "Notification listener enabled - music widget will display when audio is playing"
            } else {
                "Requires notification listener permission to show music playback"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!permissionState.hasNotificationListener) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (context is androidx.activity.ComponentActivity) {
                        permissionsHelper.requestPermission(
                            context,
                            com.talauncher.utils.PermissionType.NOTIFICATION_LISTENER
                        )
                    }
                }
            ) {
                Text("Enable Notification Listener")
            }
        }
    }
}

@Composable
private fun WeatherContent(
    weatherDisplay: WeatherDisplayOption,
    onUpdateWeatherDisplay: (WeatherDisplayOption) -> Unit,
    weatherTemperatureUnit: WeatherTemperatureUnit,
    onUpdateWeatherTemperatureUnit: (WeatherTemperatureUnit) -> Unit,
    permissionsHelper: PermissionsHelper
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val permissionState by permissionsHelper.permissionState.collectAsState()

    Text(
        text = stringResource(R.string.settings_weather_display_options),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        WeatherDisplayOption.entries.forEach { option ->
            FilterChip(
                selected = weatherDisplay == option,
                onClick = {
                    if (option != WeatherDisplayOption.OFF && !permissionState.hasLocation) {
                        if (context is androidx.activity.ComponentActivity) {
                            permissionsHelper.requestPermission(
                                context,
                                com.talauncher.utils.PermissionType.LOCATION
                            )
                        }
                    }
                    onUpdateWeatherDisplay(option)
                },
                label = { Text(option.label) },
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (weatherDisplay != WeatherDisplayOption.OFF && !permissionState.hasLocation) {
        Text(
            text = stringResource(R.string.settings_weather_location_required),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = stringResource(R.string.settings_weather_temperature_unit),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        WeatherTemperatureUnit.entries.forEach { option ->
            FilterChip(
                selected = weatherTemperatureUnit == option,
                onClick = { onUpdateWeatherTemperatureUnit(option) },
                label = { Text("°${option.symbol}") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NewsContent(
    showNewsWidget: Boolean,
    onToggleNewsWidget: () -> Unit,
    refreshInterval: NewsRefreshInterval,
    onUpdateRefreshInterval: (NewsRefreshInterval) -> Unit,
    selectedCategories: Set<NewsCategory>,
    onToggleCategory: (NewsCategory) -> Unit
) {
    SettingItem(
        title = "Show News Widget",
        subtitle = "Display news articles on the home screen when music is not playing",
        checked = showNewsWidget,
        onCheckedChange = { onToggleNewsWidget() }
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Refresh Frequency",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FilterChip(
            selected = refreshInterval == NewsRefreshInterval.DAILY,
            onClick = { onUpdateRefreshInterval(NewsRefreshInterval.DAILY) },
            label = { Text("Daily") }
        )
        FilterChip(
            selected = refreshInterval == NewsRefreshInterval.HOURLY,
            onClick = { onUpdateRefreshInterval(NewsRefreshInterval.HOURLY) },
            label = { Text("Hourly") }
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Categories",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(8.dp))

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        NewsCategory.entries.forEach { category ->
            CheckboxItem(
                label = category.label,
                checked = selectedCategories.contains(category),
                onCheckedChange = { onToggleCategory(category) }
            )
        }
        if (selectedCategories.isEmpty()) {
            Text(
                text = "Select at least one category to enable news.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun BuildInformationContent(
    buildCommitHash: String?,
    buildBranch: String?,
    buildTime: String?
) {
    buildCommitHash?.let { hash ->
        Text(
            text = stringResource(R.string.settings_build_commit, hash.take(8)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
    }

    buildBranch?.let { branch ->
        Text(
            text = stringResource(R.string.settings_build_branch, branch),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    buildTime?.let { time ->
        Text(
            text = stringResource(R.string.settings_build_time, time),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
