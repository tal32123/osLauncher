package com.talauncher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.talauncher.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    val shape = RoundedCornerShape(cornerRadius.dp)
    val backgroundBrush = Brush.linearGradient(
        colors = if (enableGlassmorphism) {
            listOf(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.68f),
                MaterialTheme.colorScheme.surface.copy(alpha = 0.28f)
            )
        } else {
            listOf(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.surfaceContainer)
        }
    )
    val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)

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
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        },
        border = if (enableGlassmorphism) {
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
                                    Color.White.copy(alpha = 0.16f),
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
 * Modern search field with updated styling
 */
@Composable
fun ModernSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "Search...",
    modifier: Modifier = Modifier,
    enableGlassmorphism: Boolean = false,
    onSearch: ((String) -> Unit)? = null
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    val fieldColors = if (enableGlassmorphism) {
        OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedLeadingIconColor = MaterialTheme.colorScheme.onSurface,
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        OutlinedTextFieldDefaults.colors()
    }

    val decoratedModifier = if (enableGlassmorphism) {
        modifier
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.32f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                shape = RoundedCornerShape(16.dp)
            )
    } else {
        modifier
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = decoratedModifier,
        placeholder = { Text(placeholder) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = fieldColors,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                onSearch?.invoke(value.trim())
                keyboardController?.hide()
            }
        )
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
@OptIn(ExperimentalFoundationApi::class)
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
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
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
    customWallpaperPath: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val wallpaperBitmap by produceState<ImageBitmap?>(initialValue = null, showWallpaper, customWallpaperPath) {
        value = if (showWallpaper) {
            withContext(Dispatchers.IO) {
                loadWallpaperBitmap(context, customWallpaperPath)
            }
        } else {
            null
        }
    }

    val wallpaperPainter = remember(wallpaperBitmap) {
        wallpaperBitmap?.let { BitmapPainter(it) }
    }

    val backgroundColorValue = when (backgroundColor) {
        "black" -> Color.Black
        "white" -> Color.White
        "system" -> MaterialTheme.colorScheme.background
        else -> runCatching { Color(android.graphics.Color.parseColor(backgroundColor)) }
            .getOrElse { MaterialTheme.colorScheme.background }
    }

    val blurModifier = if (blurAmount > 0f) {
        Modifier.blur((blurAmount * 25f).dp)
    } else {
        Modifier
    }

    val wallpaperAlpha = if (showWallpaper) opacity.coerceIn(0f, 1f) else 1f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColorValue)
    ) {
        if (showWallpaper && wallpaperPainter != null) {
            Image(
                painter = wallpaperPainter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(blurModifier)
                    .graphicsLayer { this.alpha = wallpaperAlpha }
            )
        }

        content()
    }
}

private suspend fun loadWallpaperBitmap(context: Context, customWallpaperPath: String?): ImageBitmap? {
    return runCatching {
        if (customWallpaperPath != null) {
            val uri = Uri.parse(customWallpaperPath)
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)?.asImageBitmap()
            }
        } else {
            val drawable = WallpaperManager.getInstance(context).drawable ?: return@runCatching null
            drawable.toBitmap().asImageBitmap()
        }
    }.getOrNull()
}
