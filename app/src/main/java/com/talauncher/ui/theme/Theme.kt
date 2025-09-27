package com.talauncher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.talauncher.data.model.ColorPaletteOption

private data class PaletteVariant(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color
)

private data class PaletteDefinition(
    val light: PaletteVariant,
    val dark: PaletteVariant
)

private fun readableContentColor(background: Color): Color {
    val luminance = background.luminance()
    return if (luminance >= 0.6f) Color.Black else Color.White
}

private fun ColorScheme.applyPalette(
    palette: PaletteDefinition,
    darkTheme: Boolean
): ColorScheme {
    val variant = if (darkTheme) palette.dark else palette.light
    val primary = variant.primary
    val secondary = variant.secondary
    val tertiary = variant.tertiary
    val background = variant.background
    val surface = variant.surface
    val surfaceVariant = variant.surfaceVariant
    val onSurface = variant.onSurface
    val onSurfaceVariant = variant.onSurfaceVariant
    val outline = variant.outline

    return this.copy(
        primary = primary,
        onPrimary = readableContentColor(primary),
        primaryContainer = primary.copy(alpha = 0.85f),
        onPrimaryContainer = readableContentColor(primary.copy(alpha = 0.85f)),
        secondary = secondary,
        onSecondary = readableContentColor(secondary),
        secondaryContainer = secondary.copy(alpha = 0.9f),
        onSecondaryContainer = readableContentColor(secondary.copy(alpha = 0.9f)),
        tertiary = tertiary,
        onTertiary = readableContentColor(tertiary),
        tertiaryContainer = tertiary.copy(alpha = 0.9f),
        onTertiaryContainer = readableContentColor(tertiary.copy(alpha = 0.9f)),
        background = background,
        onBackground = readableContentColor(background),
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceContainer = surface.copy(alpha = 0.8f),
        outline = outline,
        outlineVariant = outline.copy(alpha = 0.7f)
    )
}

private val PaletteCatalog = mapOf(
    ColorPaletteOption.DEFAULT to PaletteDefinition(
        light = PaletteVariant(
            primary = MinimalPrimaryLight,
            secondary = MinimalAccent,
            tertiary = MinimalNeutral600,
            background = MinimalBackgroundLight,
            surface = MinimalSurfaceLight,
            surfaceVariant = MinimalNeutral50,
            onSurface = MinimalOnSurfaceLight,
            onSurfaceVariant = MinimalNeutral700,
            outline = MinimalNeutral300
        ),
        dark = PaletteVariant(
            primary = MinimalPrimaryDark,
            secondary = MinimalAccent,
            tertiary = MinimalNeutral400,
            background = MinimalBackgroundDark,
            surface = MinimalSurfaceDark,
            surfaceVariant = MinimalNeutral800,
            onSurface = MinimalOnSurfaceDark,
            onSurfaceVariant = MinimalNeutral300,
            outline = MinimalNeutral600
        )
    ),
    ColorPaletteOption.WARM to PaletteDefinition(
        light = PaletteVariant(
            primary = Color(0xFFB94517),
            secondary = Color(0xFF9C4421),
            tertiary = Color(0xFF825632),
            background = Color(0xFFFCF1E6),
            surface = Color(0xFFFFF7F0),
            surfaceVariant = Color(0xFFF2DED1),
            onSurface = Color(0xFF2B160C),
            onSurfaceVariant = Color(0xFF6F4A3A),
            outline = Color(0xFFA37968)
        ),
        dark = PaletteVariant(
            primary = Color(0xFFFFB68D),
            secondary = Color(0xFFE8A572),
            tertiary = Color(0xFFDDB889),
            background = Color(0xFF2B160C),
            surface = Color(0xFF361E12),
            surfaceVariant = Color(0xFF4B2F20),
            onSurface = Color(0xFFF8DFD2),
            onSurfaceVariant = Color(0xFFEBC1A7),
            outline = Color(0xFFCFA189)
        )
    ),
    ColorPaletteOption.COOL to PaletteDefinition(
        light = PaletteVariant(
            primary = Color(0xFF0EA5E9),
            secondary = Color(0xFF6366F1),
            tertiary = Color(0xFF1F2937),
            background = Color(0xFFE0F2FE),
            surface = Color(0xFFF0F9FF),
            surfaceVariant = Color(0xFFBFDBFE),
            onSurface = Color(0xFF0F172A),
            onSurfaceVariant = Color(0xFF1E3A8A),
            outline = Color(0xFF60A5FA)
        ),
        dark = PaletteVariant(
            primary = Color(0xFF38BDF8),
            secondary = Color(0xFF8B5CF6),
            tertiary = Color(0xFF93C5FD),
            background = Color(0xFF0B1120),
            surface = Color(0xFF0F172A),
            surfaceVariant = Color(0xFF1E293B),
            onSurface = Color(0xFFE2E8F0),
            onSurfaceVariant = Color(0xFFBFDBFE),
            outline = Color(0xFF60A5FA)
        )
    ),
    ColorPaletteOption.MONOCHROME to PaletteDefinition(
        light = PaletteVariant(
            primary = Color(0xFF1F2937),
            secondary = Color(0xFF4B5563),
            tertiary = Color(0xFF9CA3AF),
            background = Color(0xFFF5F5F5),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFE5E7EB),
            onSurface = Color(0xFF111827),
            onSurfaceVariant = Color(0xFF374151),
            outline = Color(0xFF94A3B8)
        ),
        dark = PaletteVariant(
            primary = Color(0xFF9CA3AF),
            secondary = Color(0xFF6B7280),
            tertiary = Color(0xFF4B5563),
            background = Color(0xFF090A0B),
            surface = Color(0xFF111827),
            surfaceVariant = Color(0xFF1F2937),
            onSurface = Color(0xFFF9FAFB),
            onSurfaceVariant = Color(0xFFE5E7EB),
            outline = Color(0xFF6B7280)
        )
    ),
    ColorPaletteOption.NATURE to PaletteDefinition(
        light = PaletteVariant(
            primary = Color(0xFF256B37),
            secondary = Color(0xFF3A7D44),
            tertiary = Color(0xFF4F8F65),
            background = Color(0xFFE8F5EB),
            surface = Color(0xFFF4FBF6),
            surfaceVariant = Color(0xFFD0E8D8),
            onSurface = Color(0xFF0F2F1C),
            onSurfaceVariant = Color(0xFF285239),
            outline = Color(0xFF7AA98C)
        ),
        dark = PaletteVariant(
            primary = Color(0xFF6DD9A3),
            secondary = Color(0xFF5FBB84),
            tertiary = Color(0xFF7CC7A4),
            background = Color(0xFF071B11),
            surface = Color(0xFF0C2618),
            surfaceVariant = Color(0xFF133323),
            onSurface = Color(0xFFDCEFE1),
            onSurfaceVariant = Color(0xFFA9D5B8),
            outline = Color(0xFF6FB995)
        )
    )
)

// Modern 2025 Minimalist Color Schemes
private val MinimalDarkColorScheme = darkColorScheme(
    primary = MinimalPrimaryDark,
    secondary = MinimalAccent,
    tertiary = MinimalNeutral400,
    background = MinimalBackgroundDark,
    surface = MinimalSurfaceDark,
    surfaceVariant = MinimalNeutral800,
    surfaceContainer = MinimalNeutral800.copy(alpha = 0.4f),

    onPrimary = MinimalNeutral50,
    onSecondary = MinimalNeutral900,
    onTertiary = MinimalNeutral100,
    onBackground = MinimalOnSurfaceDark,
    onSurface = MinimalOnSurfaceDark,
    onSurfaceVariant = MinimalNeutral300,

    primaryContainer = MinimalNeutral800,
    onPrimaryContainer = MinimalNeutral100,
    secondaryContainer = MinimalNeutral700,
    onSecondaryContainer = MinimalNeutral100,

    outline = MinimalNeutral600.copy(alpha = 0.3f),
    outlineVariant = MinimalNeutral700.copy(alpha = 0.2f),
    scrim = Color.Black.copy(alpha = 0.7f),

    // Status colors
    error = PrimerRedDark,
    onError = MinimalNeutral50,
    errorContainer = PrimerRedDark.copy(alpha = 0.15f),
    onErrorContainer = PrimerRedDark,
)

private val MinimalLightColorScheme = lightColorScheme(
    primary = MinimalPrimaryLight,
    secondary = MinimalAccent,
    tertiary = MinimalNeutral600,
    background = MinimalBackgroundLight,
    surface = MinimalSurfaceLight,
    surfaceVariant = MinimalNeutral50,
    surfaceContainer = MinimalNeutral100.copy(alpha = 0.6f),

    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = MinimalOnSurfaceLight,
    onSurface = MinimalOnSurfaceLight,
    onSurfaceVariant = MinimalNeutral700,

    primaryContainer = MinimalNeutral50,
    onPrimaryContainer = MinimalPrimaryLight,
    secondaryContainer = MinimalNeutral100,
    onSecondaryContainer = MinimalNeutral900,

    outline = MinimalNeutral300.copy(alpha = 0.4f),
    outlineVariant = MinimalNeutral200.copy(alpha = 0.6f),
    scrim = Color.Black.copy(alpha = 0.4f),

    // Status colors
    error = PrimerRed,
    onError = Color.White,
    errorContainer = PrimerRed.copy(alpha = 0.1f),
    onErrorContainer = PrimerRed,
)

// Legacy schemes for backward compatibility
private val PrimerDarkColorScheme = MinimalDarkColorScheme
private val PrimerLightColorScheme = MinimalLightColorScheme

// Legacy color schemes for backward compatibility
private val ZenDarkColorScheme = PrimerDarkColorScheme
private val ZenLightColorScheme = PrimerLightColorScheme

@Composable
fun TALauncherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorPalette: ColorPaletteOption = ColorPaletteOption.DEFAULT,
    content: @Composable () -> Unit
) {
    val baseColorScheme = when {
        darkTheme -> MinimalDarkColorScheme
        else -> MinimalLightColorScheme
    }

    val paletteDefinition = PaletteCatalog[colorPalette] ?: PaletteCatalog.getValue(ColorPaletteOption.DEFAULT)
    val colorScheme = baseColorScheme.applyPalette(paletteDefinition, darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PrimerTypography,
        content = content
    )
}
