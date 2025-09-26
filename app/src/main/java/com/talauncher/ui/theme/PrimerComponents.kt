package com.talauncher.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// GitHub Primer-inspired component styles

object PrimerShapes {
    val small = RoundedCornerShape(6.dp)    // GitHub's border radius
    val medium = RoundedCornerShape(8.dp)   // Cards, buttons
    val large = RoundedCornerShape(12.dp)   // Large cards
}

@Composable
fun PrimerCard(
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    colors: CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ),
    elevation: CardElevation = CardDefaults.cardElevation(
        defaultElevation = 0.dp
    ),
    border: BorderStroke = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        colors = colors,
        elevation = elevation,
        border = border,
        shape = PrimerShapes.medium,
        content = content
    )
}

@Composable
fun PrimerButton(
    onClick: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ),
    contentPadding: PaddingValues = PaddingValues(
        horizontal = PrimerSpacing.md,
        vertical = PrimerSpacing.sm
    ),
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        contentPadding = contentPadding,
        shape = PrimerShapes.small,
        content = content
    )
}

@Composable
fun PrimerSecondaryButton(
    onClick: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = PrimerShapes.small,
        contentPadding = PaddingValues(
            horizontal = PrimerSpacing.md,
            vertical = PrimerSpacing.sm
        ),
        content = content
    )
}

object PrimerListItemDefaults {
    val contentPadding = PaddingValues(
        horizontal = PrimerSpacing.md,
        vertical = PrimerSpacing.sm
    )

    val minHeight = 44.dp // GitHub's minimum touch target
}
