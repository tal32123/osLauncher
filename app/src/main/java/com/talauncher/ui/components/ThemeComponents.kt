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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.talauncher.R
import com.talauncher.data.model.ColorPaletteOption
import com.talauncher.data.model.ThemeModeOption
import kotlin.math.roundToInt

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
            onPaletteSelected(ColorPaletteOption.CUSTOM)
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
    currentCustomColor: String? = null
) {
    val colors = remember(palette) { getPalettePreviewColors(palette) }

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

                    // Show visual preview of custom color instead of text
                    if (palette == ColorPaletteOption.CUSTOM && currentCustomColor != null) {
                        val customColorMap = mapOf(
                            "Deep Purple" to Color(0xFF673AB7),
                            "Pink Rose" to Color(0xFFE91E63),
                            "Forest Green" to Color(0xFF4CAF50),
                            "Sunset Orange" to Color(0xFFFF9800),
                            "Cherry Red" to Color(0xFFF44336),
                            "Ocean Teal" to Color(0xFF009688),
                            "Sky Blue" to Color(0xFF2196F3),
                            "Royal Indigo" to Color(0xFF3F51B5),
                            "Emerald" to Color(0xFF00C853),
                            "Crimson" to Color(0xFFD32F2F),
                            "Amber" to Color(0xFFFFC107),
                            "Cyan" to Color(0xFF00BCD4)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            val displayColor = if (currentCustomColor.startsWith("#")) {
                                parseHexColorSafe(currentCustomColor)
                            } else {
                                customColorMap[currentCustomColor] ?: MaterialTheme.colorScheme.primary
                            }

                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(displayColor)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                            )
                            Text(
                                text = if (currentCustomColor.startsWith("#")) "Custom" else currentCustomColor,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
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

            // Color preview - Show actual custom colors if selected
            val previewColors = if (palette == ColorPaletteOption.CUSTOM && currentCustomColor != null) {
                val customColorMap = mapOf(
                    "Deep Purple" to Color(0xFF673AB7),
                    "Pink Rose" to Color(0xFFE91E63),
                    "Forest Green" to Color(0xFF4CAF50),
                    "Sunset Orange" to Color(0xFFFF9800),
                    "Cherry Red" to Color(0xFFF44336),
                    "Ocean Teal" to Color(0xFF009688),
                    "Sky Blue" to Color(0xFF2196F3),
                    "Royal Indigo" to Color(0xFF3F51B5),
                    "Emerald" to Color(0xFF00C853),
                    "Crimson" to Color(0xFFD32F2F),
                    "Amber" to Color(0xFFFFC107),
                    "Cyan" to Color(0xFF00BCD4)
                )
                val selectedColor = if (currentCustomColor.startsWith("#")) {
                    parseHexColorSafe(currentCustomColor)
                } else {
                    customColorMap[currentCustomColor] ?: colors.primary
                }
                PalettePreviewColors(
                    primary = selectedColor,
                    secondary = selectedColor.copy(alpha = 0.8f),
                    background = selectedColor.copy(alpha = 0.12f)
                )
            } else {
                colors
            }

            ColorPreview(
                primaryColor = previewColors.primary,
                secondaryColor = previewColors.secondary,
                backgroundColor = previewColors.background,
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
        primary = Color(0xFF6366F1),
        secondary = Color(0xFF06B6D4),
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
    ColorPaletteOption.MONOCHROME to PalettePreviewColors(
        primary = Color(0xFF1F2937),
        secondary = Color(0xFF4B5563),
        background = Color(0xFFF5F5F5)
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
        primary = Color(0xFF6366F1),
        secondary = Color(0xFF8B5CF6),
        background = Color(0xFFF8F9FA)
    )
)

private fun getPalettePreviewColors(palette: ColorPaletteOption): PalettePreviewColors {
    return palettePreviewColorsMap[palette] ?: palettePreviewColorsMap[ColorPaletteOption.DEFAULT]!!
}

private data class PalettePreviewColors(
    val primary: Color,
    val secondary: Color,
    val background: Color
)

/**
 * Advanced custom color picker dialog with presets and true color selection
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

    var selectedTab by remember { mutableIntStateOf(0) }
    var customHexColor by remember { mutableStateOf("#6366F1") }
    var redValue by remember { mutableFloatStateOf(99f) }
    var greenValue by remember { mutableFloatStateOf(102f) }
    var blueValue by remember { mutableFloatStateOf(241f) }

    // Update sliders when hex changes
    LaunchedEffect(customHexColor) {
        runCatching {
            val color = parseHexColorSafe(customHexColor)
            val argb = color.toArgb()
            redValue = ((argb shr 16) and 0xFF).toFloat()
            greenValue = ((argb shr 8) and 0xFF).toFloat()
            blueValue = (argb and 0xFF).toFloat()
        }
    }

    // Update hex when sliders change
    val sliderColor = Color(
        red = redValue / 255f,
        green = greenValue / 255f,
        blue = blueValue / 255f
    )

    LaunchedEffect(redValue, greenValue, blueValue) {
        customHexColor = String.format("#%02X%02X%02X",
            redValue.roundToInt(),
            greenValue.roundToInt(),
            blueValue.roundToInt()
        )
    }

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
                // Tab selector
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Presets") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Custom") }
                    )
                }

                when (selectedTab) {
                    0 -> {
                        // Preset colors tab
                        Text(
                            text = "Select from these beautiful color options:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        val customColors = listOf(
                            "Deep Purple", "Pink Rose", "Forest Green",
                            "Sunset Orange", "Cherry Red", "Ocean Teal",
                            "Sky Blue", "Royal Indigo", "Emerald",
                            "Crimson", "Amber", "Cyan"
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
                                    repeat(3 - rowColors.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // Custom color creation tab
                        Text(
                            text = "Create your perfect color:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Color preview
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = sliderColor
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "◉",
                                    color = Color.White,
                                    style = MaterialTheme.typography.headlineLarge
                                )
                            }
                        }

                        // Hex input
                        OutlinedTextField(
                            value = customHexColor,
                            onValueChange = {
                                if (it.startsWith("#") && it.length <= 7) {
                                    customHexColor = it.uppercase()
                                }
                            },
                            label = { Text("Hex Color Code") },
                            placeholder = { Text("#6366F1") },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(sliderColor, CircleShape)
                                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Ascii
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // RGB Sliders
                        Text(
                            text = "Or adjust RGB values:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Red slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Red",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFEF4444)
                                )
                                Text(
                                    text = redValue.roundToInt().toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Slider(
                                value = redValue,
                                onValueChange = { redValue = it },
                                valueRange = 0f..255f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFEF4444),
                                    activeTrackColor = Color(0xFFEF4444)
                                )
                            )
                        }

                        // Green slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Green",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF10B981)
                                )
                                Text(
                                    text = greenValue.roundToInt().toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Slider(
                                value = greenValue,
                                onValueChange = { greenValue = it },
                                valueRange = 0f..255f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF10B981),
                                    activeTrackColor = Color(0xFF10B981)
                                )
                            )
                        }

                        // Blue slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Blue",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF3B82F6)
                                )
                                Text(
                                    text = blueValue.roundToInt().toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Slider(
                                value = blueValue,
                                onValueChange = { blueValue = it },
                                valueRange = 0f..255f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF3B82F6),
                                    activeTrackColor = Color(0xFF3B82F6)
                                )
                            )
                        }

                        // Apply custom color button
                        Button(
                            onClick = {
                                onColorSelected(customHexColor)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = sliderColor
                            )
                        ) {
                            Text(
                                text = "Use This Color",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
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
        "Deep Purple" to Color(0xFF673AB7),
        "Pink Rose" to Color(0xFFE91E63),
        "Forest Green" to Color(0xFF4CAF50),
        "Sunset Orange" to Color(0xFFFF9800),
        "Cherry Red" to Color(0xFFF44336),
        "Ocean Teal" to Color(0xFF009688),
        "Sky Blue" to Color(0xFF2196F3),
        "Royal Indigo" to Color(0xFF3F51B5),
        "Emerald" to Color(0xFF00C853),
        "Crimson" to Color(0xFFD32F2F),
        "Amber" to Color(0xFFFFC107),
        "Cyan" to Color(0xFF00BCD4)
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
        // Color circle - Much larger for easier tapping
        Box(
            modifier = Modifier
                .size(72.dp)  // Increased from 48dp to 72dp for much easier tapping
                .clip(CircleShape)
                .background(color)
                .border(
                    width = if (isSelected) 4.dp else 2.dp,  // Thicker borders
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
                    tint = Color.White,  // Always white for better contrast
                    modifier = Modifier.size(28.dp)  // Larger icon
                )
            }
        }

        // Color name
        Text(
            text = colorName,
            style = MaterialTheme.typography.labelMedium,  // Slightly larger text
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 2,  // Allow wrapping for longer names
            overflow = TextOverflow.Ellipsis
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

    var selectedPrimaryColor by remember { mutableStateOf(currentPrimaryColor ?: "#6366F1") }
    var selectedSecondaryColor by remember { mutableStateOf(currentSecondaryColor ?: "#8B5CF6") }

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
                            placeholder = { Text("#6366F1") },
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
                            placeholder = { Text("#8B5CF6") },
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
            else -> Color(0xFF6366F1) // Default fallback
        }
    } catch (e: NumberFormatException) {
        Color(0xFF6366F1) // Default fallback
    }
}