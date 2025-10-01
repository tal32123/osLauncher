package com.talauncher.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.talauncher.data.model.AppInfo
import com.talauncher.data.model.AppIconStyleOption
import com.talauncher.ui.components.AppIcon
import com.talauncher.ui.theme.PrimerCard
import com.talauncher.ui.theme.PrimerListItemDefaults
import com.talauncher.ui.theme.PrimerShapes
import com.talauncher.ui.theme.PrimerSpacing

/**
 * List item components for the Home screen.
 *
 * Architecture:
 * - Follows Component pattern for reusable UI elements
 * - Single Responsibility: Each component renders one type of list item
 * - Open/Closed Principle: Easy to extend with new item types
 * - Consistent styling through theme system
 */

/**
 * List item for recently used apps.
 * Displays app with special "Recent" indicator.
 *
 * Design Pattern: Component pattern with composition
 * SOLID: Single Responsibility - renders recent app item only
 *
 * @param appInfo App information to display
 * @param onClick Callback when item is clicked
 * @param onLongClick Callback when item is long-pressed
 * @param iconStyle Style for the app icon
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentAppItem(
    appInfo: AppInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    iconStyle: AppIconStyleOption
) {
    PrimerCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PrimerSpacing.md)
                .heightIn(min = PrimerListItemDefaults.minHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconStyle != AppIconStyleOption.HIDDEN) {
                AppIcon(
                    packageName = appInfo.packageName,
                    appName = appInfo.appName,
                    iconStyle = iconStyle
                )
                Spacer(modifier = Modifier.width(PrimerSpacing.md))
            }

            Text(
                text = appInfo.appName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Recent indicator badge
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = PrimerShapes.small,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "Recent",
                    modifier = Modifier.padding(
                        horizontal = PrimerSpacing.xs,
                        vertical = 2.dp
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * List item for search results.
 * Simple card showing app name.
 *
 * @param appInfo App information to display
 * @param onClick Callback when item is clicked
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchResultItem(
    appInfo: AppInfo,
    onClick: () -> Unit
) {
    PrimerCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PrimerSpacing.md)
                .heightIn(min = PrimerListItemDefaults.minHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = appInfo.appName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * List item for pinned apps.
 * Similar to search result but with long-click support.
 *
 * @param appInfo App information to display
 * @param onClick Callback when item is clicked
 * @param onLongClick Callback when item is long-pressed
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PinnedAppItem(
    appInfo: AppInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    PrimerCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PrimerSpacing.md)
                .heightIn(min = PrimerListItemDefaults.minHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = appInfo.appName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
