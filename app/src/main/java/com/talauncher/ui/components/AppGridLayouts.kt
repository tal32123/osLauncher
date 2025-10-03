package com.talauncher.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.talauncher.data.model.AppInfo
import com.talauncher.data.model.AppSectionLayoutOption
import com.talauncher.data.model.AppDisplayStyleOption
import com.talauncher.data.model.IconColorOption
import com.talauncher.data.model.AppIconStyleOption

/**
 * Renders a list of apps using the specified section layout settings.
 * This function encapsulates all layout logic (list vs grid) in one place.
 */
fun LazyListScope.appSectionItems(
    apps: List<AppInfo>,
    layout: AppSectionLayoutOption,
    displayStyle: AppDisplayStyleOption,
    iconColor: IconColorOption,
    enableGlassmorphism: Boolean,
    uiDensity: UiDensity,
    onClick: (AppInfo) -> Unit,
    onLongClick: ((AppInfo) -> Unit)? = null,
    keyPrefix: String = ""
) {
    when (layout) {
        AppSectionLayoutOption.LIST -> {
            // Standard list layout - one app per row
            items(apps, key = { "${keyPrefix}_${it.packageName}" }) { app ->
                AppListItem(
                    app = app,
                    displayStyle = displayStyle,
                    iconColor = iconColor,
                    enableGlassmorphism = enableGlassmorphism,
                    uiDensity = uiDensity,
                    onClick = { onClick(app) },
                    onLongClick = onLongClick?.let { { it(app) } }
                )
            }
        }
        AppSectionLayoutOption.GRID_3,
        AppSectionLayoutOption.GRID_4 -> {
            // Grid layout - multiple apps per row
            val columns = layout.columns
            val chunked = apps.chunked(columns)

            items(chunked.size, key = { rowIndex -> "${keyPrefix}_row_$rowIndex" }) { rowIndex ->
                val rowApps = chunked[rowIndex]
                AppGridRow(
                    apps = rowApps,
                    columns = columns,
                    displayStyle = displayStyle,
                    iconColor = iconColor,
                    onClick = onClick,
                    onLongClick = onLongClick
                )
            }
        }
    }
}

/**
 * Renders a single app in list mode.
 */
@Composable
private fun AppListItem(
    app: AppInfo,
    displayStyle: AppDisplayStyleOption,
    iconColor: IconColorOption,
    enableGlassmorphism: Boolean,
    uiDensity: UiDensity,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?
) {
    val iconStyle = when {
        displayStyle == AppDisplayStyleOption.TEXT_ONLY -> AppIconStyleOption.HIDDEN
        iconColor == IconColorOption.MONOCHROME -> AppIconStyleOption.BLACK_AND_WHITE
        else -> AppIconStyleOption.ORIGINAL
    }

    ModernAppItem(
        appName = app.appName,
        packageName = app.packageName,
        onClick = onClick,
        onLongClick = onLongClick,
        appIconStyle = iconStyle,
        enableGlassmorphism = enableGlassmorphism,
        uiDensity = uiDensity
    )
}

/**
 * Renders a row of apps in grid mode.
 */
@Composable
private fun AppGridRow(
    apps: List<AppInfo>,
    columns: Int,
    displayStyle: AppDisplayStyleOption,
    iconColor: IconColorOption,
    onClick: (AppInfo) -> Unit,
    onLongClick: ((AppInfo) -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        apps.forEach { app ->
            Box(modifier = Modifier.weight(1f)) {
                AppGridItem(
                    app = app,
                    displayStyle = displayStyle,
                    iconColor = iconColor,
                    onClick = { onClick(app) },
                    onLongClick = onLongClick?.let { { it(app) } }
                )
            }
        }

        // Fill remaining cells with empty space
        repeat(columns - apps.size) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

/**
 * Renders a single app in grid mode.
 */
@Composable
private fun AppGridItem(
    app: AppInfo,
    displayStyle: AppDisplayStyleOption,
    iconColor: IconColorOption,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?
) {
    val iconStyle = when {
        displayStyle == AppDisplayStyleOption.TEXT_ONLY -> AppIconStyleOption.HIDDEN
        iconColor == IconColorOption.MONOCHROME -> AppIconStyleOption.BLACK_AND_WHITE
        else -> AppIconStyleOption.ORIGINAL
    }

    when (displayStyle) {
        AppDisplayStyleOption.ICON_ONLY -> {
            AppGridItemIconOnly(
                app = app,
                iconStyle = iconStyle,
                onClick = onClick,
                onLongClick = onLongClick
            )
        }
        AppDisplayStyleOption.ICON_AND_TEXT -> {
            AppGridItemWithText(
                app = app,
                iconStyle = iconStyle,
                onClick = onClick,
                onLongClick = onLongClick
            )
        }
        AppDisplayStyleOption.TEXT_ONLY -> {
            AppGridItemTextOnly(
                app = app,
                onClick = onClick,
                onLongClick = onLongClick
            )
        }
    }
}
