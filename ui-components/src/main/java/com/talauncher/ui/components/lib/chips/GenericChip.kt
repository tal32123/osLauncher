package com.talauncher.ui.components.lib.chips

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.talauncher.ui.components.lib.foundation.ComponentDefaults
import com.talauncher.ui.components.lib.foundation.ComponentDensity

/**
 * Generic, reusable chip component with selection support
 * Replaces various chip implementations across the app
 */
@Composable
fun <T> GenericChip(
    item: T,
    isSelected: Boolean,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector? = null,
    density: ComponentDensity = ComponentDensity.Comfortable,
    testTag: String? = null,
    contentDescription: String? = null
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(300, easing = EaseInOutCubic),
        label = "chipContainerColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(300, easing = EaseInOutCubic),
        label = "chipContentColor"
    )

    val padding = when (density) {
        ComponentDensity.Compact -> ComponentDefaults.Spacing.sm
        ComponentDensity.Comfortable -> ComponentDefaults.Spacing.md
        ComponentDensity.Spacious -> ComponentDefaults.Spacing.lg
    }

    val iconSize = when (density) {
        ComponentDensity.Compact -> 16.dp
        ComponentDensity.Comfortable -> 18.dp
        ComponentDensity.Spacious -> 20.dp
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onSelected(item) }
            .then(
                if (testTag != null) Modifier.testTag(testTag) else Modifier
            )
            .semantics {
                role = Role.RadioButton
                this.contentDescription = contentDescription ?: label
            },
        color = containerColor,
        border = ComponentDefaults.borderStroke(
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
            }
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = padding,
                vertical = ComponentDefaults.Spacing.sm
            ),
            horizontalArrangement = Arrangement.spacedBy(ComponentDefaults.Spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(iconSize)
                )
            }
            Text(
                text = label,
                color = contentColor,
                style = when (density) {
                    ComponentDensity.Compact -> MaterialTheme.typography.bodySmall
                    ComponentDensity.Comfortable -> MaterialTheme.typography.bodyMedium
                    ComponentDensity.Spacious -> MaterialTheme.typography.bodyLarge
                },
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

/**
 * Standard Material3 filter chip wrapper for consistency
 */
@Composable
fun <T> SimpleFilterChip(
    item: T,
    isSelected: Boolean,
    onSelected: (T) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    testTag: String? = null
) {
    FilterChip(
        selected = isSelected,
        onClick = { onSelected(item) },
        label = { Text(label) },
        modifier = modifier.then(
            if (testTag != null) Modifier.testTag(testTag) else Modifier
        ),
        leadingIcon = if (leadingIcon != null) {
            {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

/**
 * Action chip for triggering actions without selection state
 */
@Composable
fun ActionChip(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    variant: ChipVariant = ChipVariant.Default,
    density: ComponentDensity = ComponentDensity.Comfortable,
    testTag: String? = null
) {
    val (containerColor, contentColor) = when (variant) {
        ChipVariant.Default -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        ChipVariant.Primary -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        ChipVariant.Success -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        ChipVariant.Warning -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        ChipVariant.Error -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }

    val padding = when (density) {
        ComponentDensity.Compact -> ComponentDefaults.Spacing.sm
        ComponentDensity.Comfortable -> ComponentDefaults.Spacing.md
        ComponentDensity.Spacious -> ComponentDefaults.Spacing.lg
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .then(
                if (testTag != null) Modifier.testTag(testTag) else Modifier
            ),
        color = containerColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = padding,
                vertical = ComponentDefaults.Spacing.sm
            ),
            horizontalArrangement = Arrangement.spacedBy(ComponentDefaults.Spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = label,
                color = contentColor,
                style = when (density) {
                    ComponentDensity.Compact -> MaterialTheme.typography.bodySmall
                    ComponentDensity.Comfortable -> MaterialTheme.typography.bodyMedium
                    ComponentDensity.Spacious -> MaterialTheme.typography.bodyLarge
                }
            )
        }
    }
}

enum class ChipVariant {
    Default,
    Primary,
    Success,
    Warning,
    Error
}