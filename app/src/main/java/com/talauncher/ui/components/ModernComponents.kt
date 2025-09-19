package com.talauncher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp
import com.talauncher.ui.theme.*

/**
 * Modern 2025 minimalist card component with glassmorphism effects
 */
@Composable
fun ModernGlassCard(
    modifier: Modifier = Modifier,
    enableGlassmorphism: Boolean = false,
    cornerRadius: Int = 12,
    elevation: Int = 2,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardColors = if (enableGlassmorphism) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                .compositeOver(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        )
    } else {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    }

    val cardModifier = if (enableGlassmorphism) {
        modifier.blur(0.5.dp)
    } else {
        modifier
    }

    Card(
        modifier = cardModifier,
        elevation = CardDefaults.cardElevation(defaultElevation = elevation.dp),
        colors = cardColors,
        shape = RoundedCornerShape(cornerRadius.dp),
        border = if (enableGlassmorphism) {
            BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        },
        content = { Column(content = content) }
    )
}

/**
 * Modern search field with updated styling
 */
@Composable
fun ModernSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "Search...",
    modifier: Modifier = Modifier,
    enableGlassmorphism: Boolean = false
) {
    val fieldColors = if (enableGlassmorphism) {
        OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    } else {
        OutlinedTextFieldDefaults.colors()
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = { Text(placeholder) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = fieldColors
    )
}

/**
 * Modern button with updated styling
 */
@Composable
fun ModernButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: ButtonVariant = ButtonVariant.Primary,
    content: @Composable RowScope.() -> Unit
) {
    when (variant) {
        ButtonVariant.Primary -> {
            Button(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                content = content
            )
        }
        ButtonVariant.Secondary -> {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                content = content
            )
        }
        ButtonVariant.Ghost -> {
            TextButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(8.dp),
                content = content
            )
        }
    }
}

enum class ButtonVariant {
    Primary,
    Secondary,
    Ghost
}

/**
 * Modern app item with updated styling
 */
@Composable
fun ModernAppItem(
    appName: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    enableGlassmorphism: Boolean = false,
    cornerRadius: Int = 12,
    uiDensity: UiDensity = UiDensity.Comfortable
) {
    val padding = when (uiDensity) {
        UiDensity.Compact -> 12.dp
        UiDensity.Comfortable -> 16.dp
        UiDensity.Spacious -> 20.dp
    }

    val minHeight = when (uiDensity) {
        UiDensity.Compact -> 48.dp
        UiDensity.Comfortable -> 56.dp
        UiDensity.Spacious -> 64.dp
    }

    ModernGlassCard(
        modifier = modifier,
        enableGlassmorphism = enableGlassmorphism,
        cornerRadius = cornerRadius
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .heightIn(min = minHeight),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = appName,
                style = when (uiDensity) {
                    UiDensity.Compact -> MaterialTheme.typography.bodyMedium
                    UiDensity.Comfortable -> MaterialTheme.typography.bodyLarge
                    UiDensity.Spacious -> MaterialTheme.typography.titleMedium
                },
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

enum class UiDensity {
    Compact,
    Comfortable,
    Spacious
}

/**
 * Modern backdrop with blur effect for wallpaper
 */
@Composable
fun ModernBackdrop(
    showWallpaper: Boolean,
    blurAmount: Float = 0f,
    backgroundColor: String = "system",
    opacity: Float = 1f,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val backgroundModifier = if (showWallpaper) {
        // Transparent background to show wallpaper
        modifier
            .fillMaxSize()
            .then(
                if (blurAmount > 0f) {
                    Modifier.blur((blurAmount * 20).dp) // Convert 0-1 range to 0-20dp blur
                } else {
                    Modifier
                }
            )
    } else {
        // Solid background color
        val bgColor = when (backgroundColor) {
            "black" -> Color.Black
            "white" -> Color.White
            "system" -> MaterialTheme.colorScheme.background
            else -> {
                try {
                    Color(android.graphics.Color.parseColor(backgroundColor))
                } catch (e: Exception) {
                    MaterialTheme.colorScheme.background
                }
            }
        }

        modifier
            .fillMaxSize()
            .background(bgColor.copy(alpha = opacity))
    }

    Box(modifier = backgroundModifier) {
        content()
    }
}