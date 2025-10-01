package com.talauncher.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.talauncher.R
import com.talauncher.data.model.ColorPaletteOption
import com.talauncher.data.model.ThemeModeOption
import com.talauncher.ui.theme.ColorPalettes

/**
 * Beautiful theme mode toggle component following Material Design 3 principles
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeModeSelector(
    selectedMode: ThemeModeOption,
    onModeSelected: (ThemeModeOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.theme_mode_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = stringResource(R.string.theme_mode_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(ThemeModeOption.entries) { mode ->
                    ThemeModeChip(
                        mode = mode,
                        isSelected = selectedMode == mode,
                        onSelected = { onModeSelected(mode) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeModeChip(
    mode: ThemeModeOption,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
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

    val icon = when (mode) {
        ThemeModeOption.SYSTEM -> Icons.Filled.Settings
        ThemeModeOption.LIGHT -> Icons.Filled.Star
        ThemeModeOption.DARK -> Icons.Filled.Settings
    }

    val label = when (mode) {
        ThemeModeOption.SYSTEM -> stringResource(R.string.theme_mode_system)
        ThemeModeOption.LIGHT -> stringResource(R.string.theme_mode_light)
        ThemeModeOption.DARK -> stringResource(R.string.theme_mode_dark)
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onSelected() }
            .testTag("theme_mode_${mode.name}")
            .semantics {
                role = Role.RadioButton
                contentDescription = "Select $label theme mode"
            },
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(20.dp),
        border = if (!isSelected) {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        } else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
            )
        }
    }
}

/**
 * Enhanced color palette picker with beautiful preview cards
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPaletteSelector(
    selectedPalette: ColorPaletteOption,
    onPaletteSelected: (ColorPaletteOption) -> Unit,
    modifier: Modifier = Modifier,
    currentCustomColor: String? = null,
    onCustomColorSelected: (String) -> Unit = {},
    currentCustomPrimaryColor: String? = null,
    currentCustomSecondaryColor: String? = null,
    onCustomPrimaryColorSelected: (String) -> Unit = {},
    onCustomSecondaryColorSelected: (String) -> Unit = {}
) {
    var showCustomColorPicker by remember { mutableStateOf(false) }
    var showAdvancedCustomPicker by remember { mutableStateOf(false) }
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.color_palette_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = stringResource(R.string.color_palette_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Group palettes in rows of 2
            val paletteRows = ColorPaletteOption.entries.chunked(2)

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                paletteRows.forEach { rowPalettes ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowPalettes.forEach { palette ->
                            ColorPaletteCard(
                                palette = palette,
                                isSelected = selectedPalette == palette,
                                currentCustomColor = if (palette == ColorPaletteOption.CUSTOM) currentCustomColor else null,
                                currentCustomPrimaryColor = if (palette == ColorPaletteOption.CUSTOM) currentCustomPrimaryColor else null,
                                currentCustomSecondaryColor = if (palette == ColorPaletteOption.CUSTOM) currentCustomSecondaryColor else null,
                                onSelected = {
                                    if (palette == ColorPaletteOption.CUSTOM) {
                                        showCustomColorPicker = true
                                    } else {
                                        onPaletteSelected(palette)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Add spacer if row has only one item
                        if (rowPalettes.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    // Custom color picker dialog
    CustomColorPickerDialog(
        isVisible = showCustomColorPicker,
        currentCustomColor = currentCustomColor,
        onColorSelected = { colorName ->
            onCustomColorSelected(colorName)
            showCustomColorPicker = false
        },
        onDismiss = { showCustomColorPicker = false }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorPaletteCard(
    palette: ColorPaletteOption,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
    currentCustomColor: String? = null,
    currentCustomPrimaryColor: String? = null,
    currentCustomSecondaryColor: String? = null
) {
    val colors = remember(
        palette,
        currentCustomColor,
        currentCustomPrimaryColor,
        currentCustomSecondaryColor
    ) {
        getPalettePreviewColors(
            palette = palette,
            customColorOption = currentCustomColor,
            customPrimaryColor = currentCustomPrimaryColor,
            customSecondaryColor = currentCustomSecondaryColor
        )
    }
    val hasActiveCustomColors = palette == ColorPaletteOption.CUSTOM &&
        (!currentCustomColor.isNullOrBlank() ||
            !currentCustomPrimaryColor.isNullOrBlank() ||
            !currentCustomSecondaryColor.isNullOrBlank())

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        },
        animationSpec = tween(300, easing = EaseInOutCubic),
        label = "borderColor"
    )

    Card(
        modifier = modifier
            .aspectRatio(1.8f)
            .clickable { onSelected() }
            .testTag("color_palette_${palette.name}")
            .semantics {
                role = Role.RadioButton
            },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = borderColor
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = palette.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Show current custom color selection for CUSTOM palette
                    if (palette == ColorPaletteOption.CUSTOM && currentCustomColor != null) {
                        Text(
                            text = currentCustomColor,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasActiveCustomColors) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            contentColor = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.color_palette_custom_badge),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Color preview
            ColorPreview(
                primaryColor = colors.primary,
                secondaryColor = colors.secondary,
                backgroundColor = colors.background,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            )
        }
    }
}

@Composable
private fun ColorPreview(
    primaryColor: Color,
    secondaryColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.clip(RoundedCornerShape(8.dp))) {
        val width = size.width
        val height = size.height

        // Background
        drawRect(backgroundColor)

        // Primary color section (left half)
        drawRect(
            color = primaryColor,
            size = androidx.compose.ui.geometry.Size(width * 0.6f, height)
        )

        // Secondary color section (right portion)
        drawRect(
            color = secondaryColor,
            topLeft = androidx.compose.ui.geometry.Offset(width * 0.6f, 0f),
            size = androidx.compose.ui.geometry.Size(width * 0.4f, height)
        )
    }
}

/**
 * Get preview colors for each palette
 */
private val palettePreviewColorsMap = mapOf(
    ColorPaletteOption.DEFAULT to PalettePreviewColors(
        primary = Color(0xFF2196F3),
        secondary = Color(0xFFFF9800),
        background = Color(0xFFFAFAFA)
    ),
    ColorPaletteOption.WARM to PalettePreviewColors(
        primary = Color(0xFFB94517),
        secondary = Color(0xFF9C4421),
        background = Color(0xFFFCF1E6)
    ),
    ColorPaletteOption.COOL to PalettePreviewColors(
        primary = Color(0xFF0EA5E9),
        secondary = Color(0xFF6366F1),
        background = Color(0xFFE0F2FE)
    ),
    ColorPaletteOption.NATURE to PalettePreviewColors(
        primary = Color(0xFF256B37),
        secondary = Color(0xFF3A7D44),
        background = Color(0xFFE8F5EB)
    ),
    ColorPaletteOption.OCEANIC to PalettePreviewColors(
        primary = Color(0xFF0891B2),
        secondary = Color(0xFF0284C7),
        background = Color(0xFFF0F9FF)
    ),
    ColorPaletteOption.SUNSET to PalettePreviewColors(
        primary = Color(0xFFEA580C),
        secondary = Color(0xFFDC2626),
        background = Color(0xFFFEF2F2)
    ),
    ColorPaletteOption.LAVENDER to PalettePreviewColors(
        primary = Color(0xFF7C3AED),
        secondary = Color(0xFF8B5CF6),
        background = Color(0xFFFAF5FF)
    ),
    ColorPaletteOption.CHERRY to PalettePreviewColors(
        primary = Color(0xFFE11D48),
        secondary = Color(0xFFDB2777),
        background = Color(0xFFFEF2F2)
    ),
    ColorPaletteOption.CUSTOM to PalettePreviewColors(
        primary = Color(0xFF2196F3),
        secondary = Color(0xFFFF9800),
        background = Color(0xFFF8F9FA)
    )
)

private fun getPalettePreviewColors(
    palette: ColorPaletteOption,
    customColorOption: String? = null,
    customPrimaryColor: String? = null,
    customSecondaryColor: String? = null
): PalettePreviewColors {
    if (palette == ColorPaletteOption.CUSTOM) {
        val fallback = palettePreviewColorsMap.getValue(ColorPaletteOption.CUSTOM)
        val namedColors = customColorOption
            ?.takeIf { it.isNotBlank() }
            ?.let { option -> ColorPalettes.CustomColorOptions[option] }

        val primary = when {
            !customPrimaryColor.isNullOrBlank() -> parseHexColorSafe(customPrimaryColor)
            namedColors?.get("primary") != null -> namedColors.getValue("primary")
            else -> fallback.primary
        }

        val secondary = when {
            !customSecondaryColor.isNullOrBlank() -> parseHexColorSafe(customSecondaryColor)
            !customPrimaryColor.isNullOrBlank() || namedColors?.get("primary") != null -> primary.copy(alpha = 0.8f)
            else -> fallback.secondary
        }

        val background = namedColors?.get("background") ?: fallback.background

        return PalettePreviewColors(
            primary = primary,
            secondary = secondary,
            background = background
        )
    }

    return palettePreviewColorsMap[palette] ?: palettePreviewColorsMap.getValue(ColorPaletteOption.DEFAULT)
}

private data class PalettePreviewColors(
    val primary: Color,
    val secondary: Color,
    val background: Color
)

/**
 * Custom color picker dialog for selecting custom palette colors
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomColorPickerDialog(
    isVisible: Boolean,
    currentCustomColor: String?,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onAdvancedRequested: () -> Unit = {}
) {
    if (!isVisible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Choose Custom Color",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close"
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Select from these beautiful color options:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Custom color options grid
                val customColors = listOf(
                    "Purple", "Pink", "Green", "Orange", "Red", "Teal"
                )

                val colorRows = customColors.chunked(3)
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    colorRows.forEach { rowColors ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowColors.forEach { colorName ->
                                CustomColorOption(
                                    colorName = colorName,
                                    isSelected = currentCustomColor == colorName,
                                    onSelected = { onColorSelected(colorName) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Add spacers for incomplete rows
                            repeat(3 - rowColors.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        modifier = modifier
    )
}

@Composable
private fun CustomColorOption(
    colorName: String,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorMap = mapOf(
        "Purple" to Color(0xFF7c3aed),
        "Pink" to Color(0xFFec4899),
        "Green" to Color(0xFF10b981),
        "Orange" to Color(0xFFf97316),
        "Red" to Color(0xFFef4444),
        "Teal" to Color(0xFF14b8a6)
    )

    val color = colorMap[colorName] ?: MaterialTheme.colorScheme.outline

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        },
        animationSpec = tween(300, easing = EaseInOutCubic),
        label = "customColorBorder"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Color circle
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = borderColor,
                    shape = CircleShape
                )
                .clickable { onSelected() }
                .semantics {
                    role = Role.RadioButton
                    contentDescription = "Select $colorName color"
                },
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Color name
        Text(
            text = colorName,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

/**
 * Advanced custom color picker dialog for selecting primary and secondary colors
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedCustomColorPickerDialog(
    isVisible: Boolean,
    currentPrimaryColor: String?,
    currentSecondaryColor: String?,
    onColorsSelected: (String, String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    var selectedPrimaryColor by remember { mutableStateOf(currentPrimaryColor ?: "#2196F3") }
    var selectedSecondaryColor by remember { mutableStateOf(currentSecondaryColor ?: "#FF9800") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Advanced Color Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close"
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "Customize your primary and secondary colors for a unique theme experience.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Primary Color Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Primary Color",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )

                        OutlinedTextField(
                            value = selectedPrimaryColor,
                            onValueChange = { selectedPrimaryColor = it },
                            label = { Text("Hex Color") },
                            placeholder = { Text("#2196F3") },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            color = parseHexColorSafe(selectedPrimaryColor),
                                            shape = CircleShape
                                        )
                                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Secondary Color Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Secondary Color",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )

                        OutlinedTextField(
                            value = selectedSecondaryColor,
                            onValueChange = { selectedSecondaryColor = it },
                            label = { Text("Hex Color") },
                            placeholder = { Text("#FF9800") },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            color = parseHexColorSafe(selectedSecondaryColor),
                                            shape = CircleShape
                                        )
                                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Preview Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Preview",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )

                        ColorPreview(
                            primaryColor = parseHexColorSafe(selectedPrimaryColor),
                            secondaryColor = parseHexColorSafe(selectedSecondaryColor),
                            backgroundColor = MaterialTheme.colorScheme.background,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        onColorsSelected(selectedPrimaryColor, selectedSecondaryColor)
                    }
                ) {
                    Text("Apply")
                }
            }
        },
        modifier = modifier
    )
}

private fun parseHexColorSafe(hexColor: String): Color {
    return try {
        val cleanHex = hexColor.removePrefix("#")
        when (cleanHex.length) {
            6 -> Color(cleanHex.toLong(16) or 0xFF000000)
            8 -> Color(cleanHex.toLong(16))
            else -> Color(0xFF2196F3) // Default fallback
        }
    } catch (e: NumberFormatException) {
        Color(0xFF2196F3) // Default fallback
    }
}