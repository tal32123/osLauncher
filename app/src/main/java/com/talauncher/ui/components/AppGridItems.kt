package com.talauncher.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.talauncher.data.model.AppInfo
import com.talauncher.data.model.AppIconStyleOption

/**
 * Grid item that shows only the app icon.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppGridItemIconOnly(
    app: AppInfo,
    iconStyle: AppIconStyleOption,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?
) {
    Surface(
        modifier = Modifier
            .aspectRatio(1f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            AppIcon(
                packageName = app.packageName,
                appName = app.appName,
                iconStyle = iconStyle,
                iconSize = 48.dp
            )
        }
    }
}

/**
 * Grid item that shows app icon and text below it.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppGridItemWithText(
    app: AppInfo,
    iconStyle: AppIconStyleOption,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?
) {
    Surface(
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AppIcon(
                packageName = app.packageName,
                appName = app.appName,
                iconStyle = iconStyle,
                iconSize = 40.dp
            )
            Text(
                text = app.appName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Grid item that shows only text (no icon).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppGridItemTextOnly(
    app: AppInfo,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = app.appName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
