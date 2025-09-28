package com.talauncher.ui.components.lib.buttons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.talauncher.ui.components.lib.foundation.ComponentDefaults
import com.talauncher.ui.components.lib.foundation.ComponentVariant
import com.talauncher.ui.components.lib.foundation.ComponentDensity

/**
 * Generic, reusable button component with variant support
 * Replaces ModernButton and provides consistent styling across the app
 */
@Composable
fun GenericButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: ComponentVariant = ComponentVariant.Primary,
    density: ComponentDensity = ComponentDensity.Comfortable,
    content: @Composable RowScope.() -> Unit
) {
    val contentPadding = when (density) {
        ComponentDensity.Compact -> PaddingValues(
            horizontal = ComponentDefaults.Spacing.md,
            vertical = ComponentDefaults.Spacing.sm
        )
        ComponentDensity.Comfortable -> PaddingValues(
            horizontal = ComponentDefaults.Spacing.lg,
            vertical = ComponentDefaults.Spacing.md
        )
        ComponentDensity.Spacious -> PaddingValues(
            horizontal = ComponentDefaults.Spacing.xl,
            vertical = ComponentDefaults.Spacing.lg
        )
    }

    when (variant) {
        ComponentVariant.Primary -> {
            Button(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = ComponentDefaults.Shapes.medium,
                contentPadding = contentPadding,
                content = content
            )
        }
        ComponentVariant.Secondary -> {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                border = BorderStroke(
                    ComponentDefaults.BorderWidth.thin,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                ),
                shape = ComponentDefaults.Shapes.medium,
                contentPadding = contentPadding,
                content = content
            )
        }
        ComponentVariant.Tertiary -> {
            Button(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = ComponentDefaults.Shapes.medium,
                contentPadding = contentPadding,
                content = content
            )
        }
        ComponentVariant.Ghost -> {
            TextButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                shape = ComponentDefaults.Shapes.small,
                contentPadding = contentPadding,
                content = content
            )
        }
    }
}

/**
 * Action button with semantic colors
 */
@Composable
fun ActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    action: ButtonAction = ButtonAction.Default,
    density: ComponentDensity = ComponentDensity.Comfortable,
    content: @Composable RowScope.() -> Unit
) {
    val colors = when (action) {
        ButtonAction.Default -> ButtonDefaults.buttonColors()
        ButtonAction.Destructive -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        )
        ButtonAction.Success -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
        ButtonAction.Warning -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary
        )
    }

    val contentPadding = when (density) {
        ComponentDensity.Compact -> PaddingValues(
            horizontal = ComponentDefaults.Spacing.md,
            vertical = ComponentDefaults.Spacing.sm
        )
        ComponentDensity.Comfortable -> PaddingValues(
            horizontal = ComponentDefaults.Spacing.lg,
            vertical = ComponentDefaults.Spacing.md
        )
        ComponentDensity.Spacious -> PaddingValues(
            horizontal = ComponentDefaults.Spacing.xl,
            vertical = ComponentDefaults.Spacing.lg
        )
    }

    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        shape = ComponentDefaults.Shapes.medium,
        contentPadding = contentPadding,
        content = content
    )
}

enum class ButtonAction {
    Default,
    Destructive,
    Success,
    Warning
}