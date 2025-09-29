package com.talauncher.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

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
                            "Deep Purple" to Color(0xFF7c3aed),
                            "Pink Rose" to Color(0xFFec4899),
                            "Forest Green" to Color(0xFF10b981),
                            "Sunset Orange" to Color(0xFFf97316),
                            "Cherry Red" to Color(0xFFef4444),
                            "Ocean Teal" to Color(0xFF14b8a6),
                            "Sky Blue" to Color(0xFF0ea5e9),
                            "Royal Indigo" to Color(0xFF6366f1),
                            "Emerald" to Color(0xFF10b981),
                            "Crimson" to Color(0xFFef4444),
                            "Amber" to Color(0xFFf59e0b),
                            "Cyan" to Color(0xFF06b6d4)
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
                    "Deep Purple" to Color(0xFF7c3aed),
                    "Pink Rose" to Color(0xFFec4899),
                    "Forest Green" to Color(0xFF10b981),
                    "Sunset Orange" to Color(0xFFf97316),
                    "Cherry Red" to Color(0xFFef4444),
                    "Ocean Teal" to Color(0xFF14b8a6),
                    "Sky Blue" to Color(0xFF0ea5e9),
                    "Royal Indigo" to Color(0xFF6366f1),
                    "Emerald" to Color(0xFF10b981),
                    "Crimson" to Color(0xFFef4444),
                    "Amber" to Color(0xFFf59e0b),
                    "Cyan" to Color(0xFF06b6d4)
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
                        text = { Text("Color Wheel") }
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
                        AdvancedColorWheelPicker(
                            customHexColor = customHexColor,
                            onHexColorChange = { customHexColor = it },
                            onColorSelected = onColorSelected
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Done")
            }
        },
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
        "Deep Purple" to Color(0xFF7c3aed),
        "Pink Rose" to Color(0xFFec4899),
        "Forest Green" to Color(0xFF10b981),
        "Sunset Orange" to Color(0xFFf97316),
        "Cherry Red" to Color(0xFFef4444),
        "Ocean Teal" to Color(0xFF14b8a6),
        "Sky Blue" to Color(0xFF0ea5e9),
        "Royal Indigo" to Color(0xFF6366f1),
        "Emerald" to Color(0xFF10b981),
        "Crimson" to Color(0xFFef4444),
        "Amber" to Color(0xFFf59e0b),
        "Cyan" to Color(0xFF06b6d4)
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

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            color = parseHexColorSafe(selectedPrimaryColor),
                                            shape = CircleShape
                                        )
                                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                )
                                Text(
                                    text = selectedPrimaryColor,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                        }
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

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            color = parseHexColorSafe(selectedSecondaryColor),
                                            shape = CircleShape
                                        )
                                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                )
                                Text(
                                    text = selectedSecondaryColor,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                        }
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

@Composable
private fun AdvancedColorWheelPicker(
    customHexColor: String,
    onHexColorChange: (String) -> Unit,
    onColorSelected: (String) -> Unit
) {
    var hueValue by remember { mutableFloatStateOf(180f) }
    var saturationValue by remember { mutableFloatStateOf(1f) }
    var brightnessValue by remember { mutableFloatStateOf(1f) }

    // Convert current hex to HSV for initialization
    LaunchedEffect(customHexColor) {
        runCatching {
            val color = parseHexColorSafe(customHexColor)
            val hsv = color.toHSV()
            hueValue = hsv.hue
            saturationValue = hsv.saturation
            brightnessValue = hsv.value
        }
    }

    // Update hex when HSV changes with animated color transitions
    val currentColor by animateColorAsState(
        targetValue = HSVColor(hueValue, saturationValue, brightnessValue).toColor(),
        animationSpec = tween(durationMillis = 150),
        label = "ColorTransition"
    )

    LaunchedEffect(hueValue, saturationValue, brightnessValue) {
        val baseColor = HSVColor(hueValue, saturationValue, brightnessValue).toColor()
        val argb = baseColor.toArgb()
        val hex = String.format("#%06X", 0xFFFFFF and argb)
        onHexColorChange(hex)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Use the color wheel to select any color:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Large color preview
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            colors = CardDefaults.cardColors(
                containerColor = currentColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = customHexColor,
                    color = if (brightnessValue > 0.5f) Color.Black else Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Hue spectrum bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Hue Spectrum",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                HueSpectrumBar(
                    hue = hueValue,
                    onHueChange = { hueValue = it }
                )

                Text(
                    text = "Hue: ${hueValue.roundToInt()}°",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2D Saturation/Brightness picker
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Saturation & Brightness",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                SaturationBrightnessSquare(
                    hue = hueValue,
                    saturation = saturationValue,
                    brightness = brightnessValue,
                    onColorChange = { sat, bright ->
                        saturationValue = sat
                        brightnessValue = bright
                    }
                )
            }
        }

        // Saturation slider
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Saturation",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${(saturationValue * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Slider(
                    value = saturationValue,
                    onValueChange = { saturationValue = it },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = currentColor,
                        activeTrackColor = currentColor.copy(alpha = 0.8f)
                    )
                )
            }
        }

        // Brightness slider
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Brightness",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${(brightnessValue * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Slider(
                    value = brightnessValue,
                    onValueChange = { brightnessValue = it },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = currentColor,
                        activeTrackColor = currentColor.copy(alpha = 0.8f)
                    )
                )
            }
        }

        // Color code display (read-only)
        Card(
            modifier = Modifier.fillMaxWidth(0.6f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(currentColor, CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
                Text(
                    text = customHexColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Apply button
        Button(
            onClick = {
                onColorSelected(customHexColor)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = currentColor
            )
        ) {
            Text(
                text = "Use This Color",
                color = if (brightnessValue > 0.5f) Color.Black else Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun HueSpectrumBar(
    hue: Float,
    onHueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val newHue = (offset.x / size.width) * 360f
                            onHueChange(newHue.coerceIn(0f, 360f))
                        },
                        onDragEnd = {
                            // Optional: Add haptic feedback here if needed
                        }
                    ) { change, _ ->
                        val newHue = (change.position.x / size.width) * 360f
                        onHueChange(newHue.coerceIn(0f, 360f))
                    }
                }
        ) {
            // Draw hue spectrum
            val width = size.width
            val height = size.height

            for (x in 0 until width.toInt()) {
                val hueAtX = (x / width) * 360f
                val color = HSVColor(hueAtX, 1f, 1f).toColor()
                drawRect(
                    color = color,
                    topLeft = Offset(x.toFloat(), 0f),
                    size = androidx.compose.ui.geometry.Size(1f, height)
                )
            }

            // Draw hue indicator
            val indicatorX = (hue / 360f) * width
            drawCircle(
                color = Color.White,
                radius = height * 0.4f,
                center = Offset(indicatorX, height / 2f)
            )
            drawCircle(
                color = Color.Black,
                radius = height * 0.3f,
                center = Offset(indicatorX, height / 2f)
            )
        }
    }
}

@Composable
private fun SaturationBrightnessSquare(
    hue: Float,
    saturation: Float,
    brightness: Float,
    onColorChange: (saturation: Float, brightness: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val newSaturation = (offset.x / size.width).coerceIn(0f, 1f)
                            val newBrightness = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                            onColorChange(newSaturation, newBrightness)
                        },
                        onDragEnd = {
                            // Optional: Add haptic feedback here if needed
                        }
                    ) { change, _ ->
                        val newSaturation = (change.position.x / size.width).coerceIn(0f, 1f)
                        val newBrightness = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                        onColorChange(newSaturation, newBrightness)
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val stepSize = 4f // Adjust for performance vs quality

            // Draw the saturation/brightness square
            for (x in 0 until (width / stepSize).toInt()) {
                for (y in 0 until (height / stepSize).toInt()) {
                    val sat = (x * stepSize) / width
                    val bright = 1f - ((y * stepSize) / height)
                    val color = HSVColor(hue, sat, bright).toColor()

                    drawRect(
                        color = color,
                        topLeft = Offset(x * stepSize, y * stepSize),
                        size = androidx.compose.ui.geometry.Size(stepSize, stepSize)
                    )
                }
            }

            // Draw selection indicator
            val indicatorX = saturation * width
            val indicatorY = (1f - brightness) * height
            val indicatorRadius = 8.dp.toPx()

            // White outer ring
            drawCircle(
                color = Color.White,
                radius = indicatorRadius + 2.dp.toPx(),
                center = Offset(indicatorX, indicatorY)
            )

            // Black inner ring
            drawCircle(
                color = Color.Black,
                radius = indicatorRadius,
                center = Offset(indicatorX, indicatorY)
            )

            // Current color dot
            val currentColor = HSVColor(hue, saturation, brightness).toColor()
            drawCircle(
                color = currentColor,
                radius = indicatorRadius - 2.dp.toPx(),
                center = Offset(indicatorX, indicatorY)
            )
        }
    }
}

data class HSVColor(
    val hue: Float,        // 0-360
    val saturation: Float, // 0-1
    val value: Float       // 0-1
)

private fun Color.toHSV(): HSVColor {
    val r = red
    val g = green
    val b = blue

    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min

    val hue = when {
        delta == 0f -> 0f
        max == r -> 60f * (((g - b) / delta) % 6f)
        max == g -> 60f * ((b - r) / delta + 2f)
        else -> 60f * ((r - g) / delta + 4f)
    }.let { if (it < 0) it + 360f else it }

    val saturation = if (max == 0f) 0f else delta / max
    val value = max

    return HSVColor(hue, saturation, value)
}

private fun HSVColor.toColor(): Color {
    val c = value * saturation
    val x = c * (1f - kotlin.math.abs((hue / 60f) % 2f - 1f))
    val m = value - c

    val (r, g, b) = when ((hue / 60f).toInt()) {
        0 -> Triple(c, x, 0f)
        1 -> Triple(x, c, 0f)
        2 -> Triple(0f, c, x)
        3 -> Triple(0f, x, c)
        4 -> Triple(x, 0f, c)
        5 -> Triple(c, 0f, x)
        else -> Triple(0f, 0f, 0f)
    }

    return Color(r + m, g + m, b + m)
}