package com.talauncher.ui.components.lib.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.talauncher.ui.components.lib.foundation.ComponentDefaults

/**
 * Generic, reusable card component with glassmorphism support
 * Can be used across the app for consistent card styling
 */
@Composable
fun GenericCard(
    modifier: Modifier = Modifier,
    enableGlassmorphism: Boolean = false,
    cornerRadius: Int = 12,
    elevation: Int = 2,
    colors: CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface
    ),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius.dp)
    val backgroundBrush = Brush.linearGradient(
        colors = if (enableGlassmorphism) {
            listOf(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.68f),
                MaterialTheme.colorScheme.surface.copy(alpha = 0.28f)
            )
        } else {
            listOf(colors.containerColor, colors.containerColor)
        }
    )
    val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    val surfaceColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)

    Card(
        modifier = modifier,
        shape = shape,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (enableGlassmorphism) 0.dp else elevation.dp
        ),
        colors = if (enableGlassmorphism) {
            CardDefaults.cardColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        } else {
            colors
        },
        border = border ?: if (enableGlassmorphism) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        }
    ) {
        Box(
            modifier = Modifier
                .background(backgroundBrush)
                .drawBehind {
                    if (enableGlassmorphism) {
                        val radius = cornerRadius.dp.toPx()
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    surfaceColor,
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = size.height * 0.6f
                            ),
                            size = Size(size.width, size.height),
                            cornerRadius = CornerRadius(radius, radius)
                        )
                        drawRoundRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    highlightColor,
                                    Color.Transparent
                                ),
                                center = Offset(size.width / 2f, size.height),
                                radius = size.width,
                                tileMode = TileMode.Clamp
                            ),
                            size = Size(size.width, size.height),
                            cornerRadius = CornerRadius(radius, radius)
                        )
                    }
                }
        ) {
            Column(content = content)
        }
    }
}

/**
 * Simplified card for basic use cases
 */
@Composable
fun SimpleCard(
    modifier: Modifier = Modifier,
    variant: CardVariant = CardVariant.Surface,
    content: @Composable ColumnScope.() -> Unit
) {
    val (colors, border) = when (variant) {
        CardVariant.Surface -> CardDefaults.cardColors() to ComponentDefaults.borderStroke()
        CardVariant.Primary -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) to ComponentDefaults.borderStroke(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        CardVariant.Secondary -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) to ComponentDefaults.borderStroke(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
        CardVariant.Success -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ) to ComponentDefaults.borderStroke(color = Color(0xFF22C55E).copy(alpha = 0.2f))
        CardVariant.Warning -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ) to ComponentDefaults.borderStroke(color = Color(0xFFF59E0B).copy(alpha = 0.2f))
        CardVariant.Error -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ) to ComponentDefaults.borderStroke(color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
    }

    Card(
        modifier = modifier,
        shape = ComponentDefaults.Shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = ComponentDefaults.Elevation.none),
        colors = colors,
        border = border,
        content = content
    )
}

enum class CardVariant {
    Surface,
    Primary,
    Secondary,
    Success,
    Warning,
    Error
}