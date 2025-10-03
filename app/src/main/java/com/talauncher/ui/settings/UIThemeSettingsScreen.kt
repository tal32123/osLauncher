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
import com.talauncher.data.model.ColorPaletteOption
import com.talauncher.data.model.ThemeModeOption
import com.talauncher.data.model.UiDensityOption
import com.talauncher.ui.components.*
import com.talauncher.ui.components.CollapsibleSection
import com.talauncher.ui.components.CollapsibleSectionContainer

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
    // Sidebar (Alphabet index) customization
    sidebarActiveScale: Float,
    onUpdateSidebarActiveScale: (Float) -> Unit,
    sidebarPopOutDp: Int,
    onUpdateSidebarPopOutDp: (Int) -> Unit,
    sidebarWaveSpread: Float,
    onUpdateSidebarWaveSpread: (Float) -> Unit
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
            id = "sidebar",
            title = stringResource(R.string.settings_sidebar_title)
        ) {
            SidebarSettingsContent(
                activeScale = sidebarActiveScale,
                onActiveScaleChange = onUpdateSidebarActiveScale,
                popOutDp = sidebarPopOutDp,
                onPopOutDpChange = onUpdateSidebarPopOutDp,
                waveSpread = sidebarWaveSpread,
                onWaveSpreadChange = onUpdateSidebarWaveSpread
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
private fun SidebarSettingsContent(
    activeScale: Float,
    onActiveScaleChange: (Float) -> Unit,
    popOutDp: Int,
    onPopOutDpChange: (Int) -> Unit,
    waveSpread: Float,
    onWaveSpreadChange: (Float) -> Unit
) {
    SettingsSectionCard(title = stringResource(R.string.settings_sidebar_title)) {
        Text(
            text = stringResource(R.string.settings_sidebar_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        var scale by remember { mutableStateOf(activeScale) }
        SliderSetting(
            label = stringResource(R.string.settings_sidebar_active_scale),
            value = scale,
            onValueChange = { scale = it },
            valueRange = 1.0f..2.5f,
            onValueChangeFinished = { onActiveScaleChange(scale) },
            valueLabel = String.format("%.2fx", scale)
        )

        var popOut by remember { mutableStateOf(popOutDp.toFloat()) }
        SliderSetting(
            label = stringResource(R.string.settings_sidebar_popout),
            value = popOut,
            onValueChange = { popOut = it },
            valueRange = 0f..48f,
            onValueChangeFinished = { onPopOutDpChange(popOut.toInt()) },
            valueLabel = "${popOut.toInt()} dp"
        )

        var spread by remember { mutableStateOf(waveSpread) }
        SliderSetting(
            label = stringResource(R.string.settings_sidebar_wave_spread),
            value = spread,
            onValueChange = { spread = it },
            valueRange = 0.0f..4.0f,
            onValueChangeFinished = { onWaveSpreadChange(spread) },
            valueLabel = String.format("%.2f", spread)
        )
    }
}
