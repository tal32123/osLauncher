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
import com.talauncher.data.model.AppIconStyleOption
import com.talauncher.data.model.ColorPaletteOption
import com.talauncher.data.model.ThemeModeOption
import com.talauncher.data.model.UiDensityOption
import com.talauncher.ui.components.*

@Composable
fun UIThemeSettingsScreen(
    backgroundColor: String,
    onUpdateBackgroundColor: (String) -> Unit,
    showWallpaper: Boolean,
    onToggleShowWallpaper: (Boolean) -> Unit,
    colorPalette: ColorPaletteOption,
    onUpdateColorPalette: (ColorPaletteOption) -> Unit,
    appIconStyle: AppIconStyleOption,
    onUpdateAppIconStyle: (AppIconStyleOption) -> Unit,
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
    onToggleAnimations: (Boolean) -> Unit
) {
    SettingsLazyColumn {
        item {
            ThemeModeSelector(
                selectedMode = themeMode,
                onModeSelected = onUpdateThemeMode
            )
        }

        item {
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
        }

        item {
            AppIconStyleSection(
                appIconStyle = appIconStyle,
                onUpdateAppIconStyle = onUpdateAppIconStyle
            )
        }

        item {
            WallpaperBackgroundSection(
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
        }

        item {
            VisualEffectsSection(
                enableGlassmorphism = enableGlassmorphism,
                onToggleGlassmorphism = onToggleGlassmorphism,
                enableAnimations = enableAnimations,
                onToggleAnimations = onToggleAnimations
            )
        }

        item {
            LayoutDensitySection(
                uiDensity = uiDensity,
                onUpdateUiDensity = onUpdateUiDensity
            )
        }
    }
}

@Composable
private fun AppIconStyleSection(
    appIconStyle: AppIconStyleOption,
    onUpdateAppIconStyle: (AppIconStyleOption) -> Unit
) {
    SettingsSectionCard(title = stringResource(R.string.settings_app_icons)) {
        Text(
            text = stringResource(R.string.settings_app_icons_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppIconStyleOption.entries.forEach { option ->
                FilterChip(
                    selected = appIconStyle == option,
                    onClick = { onUpdateAppIconStyle(option) },
                    label = {
                        Text(
                            text = option.label,
                            maxLines = 1
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun WallpaperBackgroundSection(
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

    SettingsSectionCard(title = stringResource(R.string.wallpaper_settings_title)) {
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
}

@Composable
private fun VisualEffectsSection(
    enableGlassmorphism: Boolean,
    onToggleGlassmorphism: (Boolean) -> Unit,
    enableAnimations: Boolean,
    onToggleAnimations: (Boolean) -> Unit
) {
    SettingsSectionCard(title = stringResource(R.string.visual_effects_title)) {
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
}

@Composable
private fun LayoutDensitySection(
    uiDensity: UiDensityOption,
    onUpdateUiDensity: (UiDensityOption) -> Unit
) {
    SettingsSectionCard(title = stringResource(R.string.layout_density_title)) {
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
}
