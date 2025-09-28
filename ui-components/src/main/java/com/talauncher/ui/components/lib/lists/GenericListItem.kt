package com.talauncher.ui.components.lib.lists

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.talauncher.ui.components.lib.cards.GenericCard
import com.talauncher.ui.components.lib.foundation.ComponentDefaults
import com.talauncher.ui.components.lib.foundation.ComponentDensity

/**
 * Generic, reusable list item component
 * Replaces ModernAppItem, ContactItem patterns with a unified approach
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GenericListItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    enableGlassmorphism: Boolean = false,
    density: ComponentDensity = ComponentDensity.Comfortable,
    testTag: String? = null
) {
    val padding = when (density) {
        ComponentDensity.Compact -> ComponentDefaults.Spacing.md
        ComponentDensity.Comfortable -> ComponentDefaults.Spacing.lg
        ComponentDensity.Spacious -> ComponentDefaults.Spacing.xl
    }

    val minHeight = when (density) {
        ComponentDensity.Compact -> 48.dp
        ComponentDensity.Comfortable -> 56.dp
        ComponentDensity.Spacious -> 64.dp
    }

    GenericCard(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .then(
                if (testTag != null) Modifier.testTag(testTag) else Modifier
            ),
        enableGlassmorphism = enableGlassmorphism
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .heightIn(min = minHeight),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ComponentDefaults.Spacing.md)
        ) {
            // Leading content (icon, avatar, etc.)
            leadingContent?.invoke()

            // Title and subtitle
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(ComponentDefaults.Spacing.xs)
            ) {
                Text(
                    text = title,
                    style = when (density) {
                        ComponentDensity.Compact -> MaterialTheme.typography.bodyMedium
                        ComponentDensity.Comfortable -> MaterialTheme.typography.bodyLarge
                        ComponentDensity.Spacious -> MaterialTheme.typography.titleMedium
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )

                subtitle?.let { sub ->
                    Text(
                        text = sub,
                        style = when (density) {
                            ComponentDensity.Compact -> MaterialTheme.typography.bodySmall
                            ComponentDensity.Comfortable -> MaterialTheme.typography.bodySmall
                            ComponentDensity.Spacious -> MaterialTheme.typography.bodyMedium
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Trailing content (buttons, icons, etc.)
            trailingContent?.invoke()
        }
    }
}

/**
 * List item with icon avatar
 */
@Composable
fun IconListItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    iconBackgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    trailingContent: (@Composable () -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    enableGlassmorphism: Boolean = false,
    density: ComponentDensity = ComponentDensity.Comfortable,
    testTag: String? = null
) {
    GenericListItem(
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        enableGlassmorphism = enableGlassmorphism,
        density = density,
        testTag = testTag,
        leadingContent = {
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                color = iconBackgroundColor,
                border = ComponentDefaults.borderStroke(
                    color = iconColor.copy(alpha = 0.3f)
                )
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        trailingContent = trailingContent
    )
}

/**
 * List item with text avatar (initials)
 */
@Composable
fun TextAvatarListItem(
    title: String,
    avatarText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    avatarColor: Color = MaterialTheme.colorScheme.primary,
    avatarBackgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    trailingContent: (@Composable () -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    enableGlassmorphism: Boolean = false,
    density: ComponentDensity = ComponentDensity.Comfortable,
    testTag: String? = null
) {
    GenericListItem(
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        enableGlassmorphism = enableGlassmorphism,
        density = density,
        testTag = testTag,
        leadingContent = {
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                color = avatarBackgroundColor,
                border = ComponentDefaults.borderStroke(
                    color = avatarColor.copy(alpha = 0.3f)
                )
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = avatarText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = avatarColor
                    )
                }
            }
        },
        trailingContent = trailingContent
    )
}

/**
 * Action item for list item trailing content
 */
@Composable
fun ListItemAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint
        )
    }
}