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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Circle
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
        ThemeModeOption.DARK -> Icons.Filled.Circle
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
                                onSelected = { onPaletteSelected(palette) },
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorPaletteCard(
    palette: ColorPaletteOption,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
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
                Text(
                    text = palette.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
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
    ColorPaletteOption.FOREST to PalettePreviewColors(
        primary = Color(0xFF166534),
        secondary = Color(0xFF15803D),
        background = Color(0xFFF0FDF4)
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