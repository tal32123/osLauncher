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
import com.talauncher.ui.components.CollapsibleSection
import com.talauncher.ui.components.CollapsibleSectionContainer
import com.talauncher.ui.components.SettingItem
import com.talauncher.ui.components.SettingsLazyColumn
import com.talauncher.ui.components.SliderSetting
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
                    onToggleTimeLimitPrompt = onToggleTimeLimitPrompt
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
    onToggleTimeLimitPrompt: () -> Unit
) {
    SettingItem(
        title = stringResource(R.string.settings_time_limit_dialog_title),
        subtitle = stringResource(R.string.settings_time_limit_dialog_subtitle),
        checked = enableTimeLimitPrompt,
        onCheckedChange = { onToggleTimeLimitPrompt() }
    )
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
