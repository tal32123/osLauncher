package com.talauncher.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.talauncher.R
import com.talauncher.data.model.AppSectionLayoutOption
import com.talauncher.data.model.AppDisplayStyleOption
import com.talauncher.data.model.IconColorOption
import com.talauncher.ui.components.CollapsibleSection
import com.talauncher.ui.components.CollapsibleSectionContainer
import com.talauncher.ui.components.SettingsLazyColumn
import com.talauncher.ui.components.SliderSetting
import kotlin.math.roundToInt

@Composable
fun AppSectionsSettingsScreen(
    // Pinned Apps settings
    pinnedAppsLayout: AppSectionLayoutOption,
    onUpdatePinnedAppsLayout: (AppSectionLayoutOption) -> Unit,
    pinnedAppsDisplayStyle: AppDisplayStyleOption,
    onUpdatePinnedAppsDisplayStyle: (AppDisplayStyleOption) -> Unit,
    pinnedAppsIconColor: IconColorOption,
    onUpdatePinnedAppsIconColor: (IconColorOption) -> Unit,

    // Recent Apps settings
    recentAppsLayout: AppSectionLayoutOption,
    onUpdateRecentAppsLayout: (AppSectionLayoutOption) -> Unit,
    recentAppsDisplayStyle: AppDisplayStyleOption,
    onUpdateRecentAppsDisplayStyle: (AppDisplayStyleOption) -> Unit,
    recentAppsIconColor: IconColorOption,
    onUpdateRecentAppsIconColor: (IconColorOption) -> Unit,
    recentAppsLimit: Int,
    onUpdateRecentAppsLimit: (Int) -> Unit,

    // All Apps settings
    allAppsLayout: AppSectionLayoutOption,
    onUpdateAllAppsLayout: (AppSectionLayoutOption) -> Unit,
    allAppsDisplayStyle: AppDisplayStyleOption,
    onUpdateAllAppsDisplayStyle: (AppDisplayStyleOption) -> Unit,
    allAppsIconColor: IconColorOption,
    onUpdateAllAppsIconColor: (IconColorOption) -> Unit
) {
    val sections = listOf(
        CollapsibleSection(
            id = "pinned_apps",
            title = stringResource(R.string.settings_pinned_apps)
        ) {
            AppSectionContent(
                layout = pinnedAppsLayout,
                onUpdateLayout = onUpdatePinnedAppsLayout,
                displayStyle = pinnedAppsDisplayStyle,
                onUpdateDisplayStyle = onUpdatePinnedAppsDisplayStyle,
                iconColor = pinnedAppsIconColor,
                onUpdateIconColor = onUpdatePinnedAppsIconColor
            )
        },
        CollapsibleSection(
            id = "recent_apps",
            title = stringResource(R.string.settings_recent_apps)
        ) {
            AppSectionContent(
                layout = recentAppsLayout,
                onUpdateLayout = onUpdateRecentAppsLayout,
                displayStyle = recentAppsDisplayStyle,
                onUpdateDisplayStyle = onUpdateRecentAppsDisplayStyle,
                iconColor = recentAppsIconColor,
                onUpdateIconColor = onUpdateRecentAppsIconColor,
                showRecentAppsLimit = true,
                recentAppsLimit = recentAppsLimit,
                onUpdateRecentAppsLimit = onUpdateRecentAppsLimit
            )
        },
        CollapsibleSection(
            id = "all_apps",
            title = stringResource(R.string.settings_all_apps)
        ) {
            AppSectionContent(
                layout = allAppsLayout,
                onUpdateLayout = onUpdateAllAppsLayout,
                displayStyle = allAppsDisplayStyle,
                onUpdateDisplayStyle = onUpdateAllAppsDisplayStyle,
                iconColor = allAppsIconColor,
                onUpdateIconColor = onUpdateAllAppsIconColor
            )
        }
    )

    SettingsLazyColumn {
        item {
            CollapsibleSectionContainer(
                sections = sections,
                initialExpandedId = "pinned_apps"
            )
        }
    }
}

@Composable
private fun AppSectionContent(
    layout: AppSectionLayoutOption,
    onUpdateLayout: (AppSectionLayoutOption) -> Unit,
    displayStyle: AppDisplayStyleOption,
    onUpdateDisplayStyle: (AppDisplayStyleOption) -> Unit,
    iconColor: IconColorOption,
    onUpdateIconColor: (IconColorOption) -> Unit,
    showRecentAppsLimit: Boolean = false,
    recentAppsLimit: Int = 0,
    onUpdateRecentAppsLimit: ((Int) -> Unit)? = null
) {
    // Layout Section
    Text(
        text = stringResource(R.string.settings_section_layout),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 4.dp)
    )
    Text(
        text = stringResource(R.string.settings_section_layout_subtitle),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppSectionLayoutOption.entries.forEach { option ->
            FilterChip(
                selected = layout == option,
                onClick = { onUpdateLayout(option) },
                label = { Text(option.label) },
                modifier = Modifier.weight(1f)
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Display Style Section
    Text(
        text = stringResource(R.string.settings_display_style),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 4.dp)
    )
    Text(
        text = stringResource(R.string.settings_display_style_subtitle),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppDisplayStyleOption.entries.forEach { option ->
            FilterChip(
                selected = displayStyle == option,
                onClick = { onUpdateDisplayStyle(option) },
                label = { Text(option.label) },
                modifier = Modifier.weight(1f)
            )
        }
    }

    // Icon Color Section (only show if display style includes icons)
    if (displayStyle != AppDisplayStyleOption.TEXT_ONLY) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.settings_icon_color),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = stringResource(R.string.settings_icon_color_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconColorOption.entries.forEach { option ->
                FilterChip(
                    selected = iconColor == option,
                    onClick = { onUpdateIconColor(option) },
                    label = { Text(option.label) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // Recent Apps Limit Slider (only for recent apps section)
    if (showRecentAppsLimit && onUpdateRecentAppsLimit != null) {
        Spacer(modifier = Modifier.height(16.dp))

        var recentAppsValue by remember(recentAppsLimit) {
            mutableStateOf(recentAppsLimit.toFloat())
        }

        val recentCount = recentAppsValue.roundToInt()
        val recentSummary = when {
            recentCount == 0 -> stringResource(R.string.settings_recent_apps_limit_hidden)
            recentCount == 1 -> stringResource(R.string.settings_recent_apps_limit_summary_single)
            else -> stringResource(R.string.settings_recent_apps_limit_summary_plural, recentCount)
        }

        SliderSetting(
            label = stringResource(R.string.settings_recent_apps_limit_title),
            value = recentAppsValue,
            onValueChange = { recentAppsValue = it },
            valueRange = 0f..10f,
            steps = 9,
            onValueChangeFinished = {
                onUpdateRecentAppsLimit(recentAppsValue.roundToInt())
            },
            valueLabel = recentSummary
        )
    }
}
