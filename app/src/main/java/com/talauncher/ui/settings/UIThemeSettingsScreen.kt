package com.talauncher.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.talauncher.R
import com.talauncher.data.model.AppDisplayStyleOption
import com.talauncher.data.model.AppSectionLayoutOption
import com.talauncher.data.model.ColorPaletteOption
import com.talauncher.data.model.IconColorOption
import com.talauncher.data.model.ThemeModeOption
import com.talauncher.data.model.UiDensityOption
import com.talauncher.ui.components.*
import com.talauncher.ui.components.CollapsibleSection
import com.talauncher.ui.components.CollapsibleSectionContainer
import kotlin.math.roundToInt

@Composable
fun UIThemeSettingsScreen(
    backgroundColor: String,
    onUpdateBackgroundColor: (String) -> Unit,
    showWallpaper: Boolean,
    onToggleShowWallpaper: (Boolean) -> Unit,
    colorPalette: ColorPaletteOption,
    onUpdateColorPalette: (ColorPaletteOption) -> Unit,
    customColorOption: String?,
    onUpdateCustomColorOption: (String) -> Unit,
    customPrimaryColor: String?,
    onUpdateCustomPrimaryColor: (String) -> Unit,
    customSecondaryColor: String?,
    onUpdateCustomSecondaryColor: (String) -> Unit,
    themeMode: ThemeModeOption,
    onUpdateThemeMode: (ThemeModeOption) -> Unit,
    wallpaperBlurAmount: Float,
    onUpdateWallpaperBlur: (Float) -> Unit,
    backgroundOpacity: Float,
    onUpdateBackgroundOpacity: (Float) -> Unit,
    customWallpaperPath: String?,
    onPickCustomWallpaper: () -> Unit,
    enableGlassmorphism: Boolean,
    onToggleGlassmorphism: (Boolean) -> Unit,
    uiDensity: UiDensityOption,
    onUpdateUiDensity: (UiDensityOption) -> Unit,
    enableAnimations: Boolean,
    onToggleAnimations: (Boolean) -> Unit,
    // App Sections settings - Pinned Apps
    pinnedAppsLayout: AppSectionLayoutOption,
    onUpdatePinnedAppsLayout: (AppSectionLayoutOption) -> Unit,
    pinnedAppsDisplayStyle: AppDisplayStyleOption,
    onUpdatePinnedAppsDisplayStyle: (AppDisplayStyleOption) -> Unit,
    pinnedAppsIconColor: IconColorOption,
    onUpdatePinnedAppsIconColor: (IconColorOption) -> Unit,
    // App Sections settings - Recent Apps
    recentAppsLayout: AppSectionLayoutOption,
    onUpdateRecentAppsLayout: (AppSectionLayoutOption) -> Unit,
    recentAppsDisplayStyle: AppDisplayStyleOption,
    onUpdateRecentAppsDisplayStyle: (AppDisplayStyleOption) -> Unit,
    recentAppsIconColor: IconColorOption,
    onUpdateRecentAppsIconColor: (IconColorOption) -> Unit,
    recentAppsLimit: Int,
    onUpdateRecentAppsLimit: (Int) -> Unit,
    // App Sections settings - All Apps
    allAppsLayout: AppSectionLayoutOption,
    onUpdateAllAppsLayout: (AppSectionLayoutOption) -> Unit,
    allAppsDisplayStyle: AppDisplayStyleOption,
    onUpdateAllAppsDisplayStyle: (AppDisplayStyleOption) -> Unit,
    allAppsIconColor: IconColorOption,
    onUpdateAllAppsIconColor: (IconColorOption) -> Unit
) {
    val sections = listOf(
        CollapsibleSection(
            id = "theme_mode",
            title = stringResource(R.string.theme_mode_title)
        ) {
            ThemeModeContent(
                selectedMode = themeMode,
                onModeSelected = onUpdateThemeMode
            )
        },
        CollapsibleSection(
            id = "color_palette",
            title = stringResource(R.string.color_palette_title)
        ) {
            ColorPaletteSelector(
                selectedPalette = colorPalette,
                onPaletteSelected = onUpdateColorPalette,
                currentCustomColor = customColorOption,
                onCustomColorSelected = onUpdateCustomColorOption,
                currentCustomPrimaryColor = customPrimaryColor,
                currentCustomSecondaryColor = customSecondaryColor,
                onCustomPrimaryColorSelected = onUpdateCustomPrimaryColor,
                onCustomSecondaryColorSelected = onUpdateCustomSecondaryColor
            )
        },
        CollapsibleSection(
            id = "wallpaper",
            title = stringResource(R.string.wallpaper_settings_title)
        ) {
            WallpaperBackgroundContent(
                showWallpaper = showWallpaper,
                onToggleShowWallpaper = onToggleShowWallpaper,
                backgroundColor = backgroundColor,
                onUpdateBackgroundColor = onUpdateBackgroundColor,
                wallpaperBlurAmount = wallpaperBlurAmount,
                onUpdateWallpaperBlur = onUpdateWallpaperBlur,
                backgroundOpacity = backgroundOpacity,
                onUpdateBackgroundOpacity = onUpdateBackgroundOpacity,
                customWallpaperPath = customWallpaperPath,
                onPickCustomWallpaper = onPickCustomWallpaper
            )
        },
        CollapsibleSection(
            id = "visual_effects",
            title = stringResource(R.string.visual_effects_title)
        ) {
            VisualEffectsContent(
                enableGlassmorphism = enableGlassmorphism,
                onToggleGlassmorphism = onToggleGlassmorphism,
                enableAnimations = enableAnimations,
                onToggleAnimations = onToggleAnimations
            )
        },
        CollapsibleSection(
            id = "layout_density",
            title = stringResource(R.string.layout_density_title)
        ) {
            LayoutDensityContent(
                uiDensity = uiDensity,
                onUpdateUiDensity = onUpdateUiDensity
            )
        },
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
                initialExpandedId = "theme_mode"
            )
        }
    }
}

@Composable
private fun ThemeModeContent(
    selectedMode: ThemeModeOption,
    onModeSelected: (ThemeModeOption) -> Unit
) {
    Text(
        text = stringResource(R.string.theme_mode_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ThemeModeOption.entries.forEach { mode ->
            FilterChip(
                selected = selectedMode == mode,
                onClick = { onModeSelected(mode) },
                label = { Text(mode.label) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun WallpaperBackgroundContent(
    showWallpaper: Boolean,
    onToggleShowWallpaper: (Boolean) -> Unit,
    backgroundColor: String,
    onUpdateBackgroundColor: (String) -> Unit,
    wallpaperBlurAmount: Float,
    onUpdateWallpaperBlur: (Float) -> Unit,
    backgroundOpacity: Float,
    onUpdateBackgroundOpacity: (Float) -> Unit,
    customWallpaperPath: String?,
    onPickCustomWallpaper: () -> Unit
) {
    var blurValue by remember(wallpaperBlurAmount) {
        mutableStateOf(wallpaperBlurAmount)
    }
    var opacityValue by remember(backgroundOpacity) {
        mutableStateOf(backgroundOpacity)
    }

    SettingItem(
        title = stringResource(R.string.settings_show_wallpaper_title),
        subtitle = stringResource(R.string.settings_show_wallpaper_subtitle),
        checked = showWallpaper,
        onCheckedChange = onToggleShowWallpaper,
        modifier = Modifier.testTag("show_wallpaper_switch")
    )

    if (showWallpaper) {
        SliderSetting(
            label = stringResource(R.string.wallpaper_blur_title),
            value = blurValue,
            onValueChange = { blurValue = it },
            valueRange = 0f..1f,
            onValueChangeFinished = {
                onUpdateWallpaperBlur(blurValue)
            },
            valueLabel = "${(blurValue * 100).toInt()}% blur",
            modifier = Modifier.testTag("wallpaper_blur_slider")
        )

        SliderSetting(
            label = stringResource(R.string.wallpaper_opacity_title),
            value = opacityValue,
            onValueChange = { opacityValue = it },
            valueRange = 0f..1f,
            onValueChangeFinished = {
                onUpdateBackgroundOpacity(opacityValue)
            },
            valueLabel = "${(opacityValue * 100).toInt()}% opacity"
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.custom_wallpaper_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModernButton(
                    onClick = onPickCustomWallpaper,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.custom_wallpaper_choose))
                }
            }
            if (customWallpaperPath != null) {
                Text(
                    text = stringResource(R.string.custom_wallpaper_using),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = stringResource(R.string.custom_wallpaper_system),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        Text(
            text = stringResource(R.string.settings_background_color),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        ChipGrid(
            items = listOf(
                "system" to stringResource(R.string.settings_background_system),
                "black" to stringResource(R.string.settings_background_black),
                "white" to stringResource(R.string.settings_background_white)
            ),
            modifier = Modifier.fillMaxWidth()
        ) { (value, label) ->
            FilterChip(
                selected = backgroundColor == value,
                onClick = { onUpdateBackgroundColor(value) },
                label = {
                    Text(
                        text = label,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            )
        }
    }
}

@Composable
private fun VisualEffectsContent(
    enableGlassmorphism: Boolean,
    onToggleGlassmorphism: (Boolean) -> Unit,
    enableAnimations: Boolean,
    onToggleAnimations: (Boolean) -> Unit
) {
    SettingItem(
        title = stringResource(R.string.glassmorphism_title),
        subtitle = stringResource(R.string.glassmorphism_subtitle),
        checked = enableGlassmorphism,
        onCheckedChange = onToggleGlassmorphism
    )

    SettingItem(
        title = stringResource(R.string.animations_title),
        subtitle = stringResource(R.string.animations_subtitle),
        checked = enableAnimations,
        onCheckedChange = onToggleAnimations
    )
}

@Composable
private fun LayoutDensityContent(
    uiDensity: UiDensityOption,
    onUpdateUiDensity: (UiDensityOption) -> Unit
) {
    Text(
        text = stringResource(R.string.ui_density_title),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
    )

    ChipGrid(
        items = UiDensityOption.entries,
        modifier = Modifier.fillMaxWidth()
    ) { option ->
        FilterChip(
            selected = uiDensity == option,
            onClick = { onUpdateUiDensity(option) },
            label = {
                Text(
                    text = option.label,
                    maxLines = 1,
                    softWrap = false
                )
            }
        )
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
