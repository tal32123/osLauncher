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
            primary = Color(0xFFE65100),
            secondary = Color(0xFFB45309),
            tertiary = Color(0xFF92400E),
            background = Color(0xFFFEF3C7),
            surface = Color(0xFFFFFBEB),
            surfaceVariant = Color(0xFFFDE68A),
            onSurface = Color(0xFF422006),
            onSurfaceVariant = Color(0xFF713F12),
            outline = Color(0xFFB45309)
        ),
        dark = PaletteVariant(
            primary = Color(0xFFFFB74D),
            secondary = Color(0xFFEAA46B),
            tertiary = Color(0xFFFFD7BA),
            background = Color(0xFF1F1405),
            surface = Color(0xFF2C1A07),
            surfaceVariant = Color(0xFF3B2510),
            onSurface = Color(0xFFFDE7C7),
            onSurfaceVariant = Color(0xFFE9B08C),
            outline = Color(0xFFB86429)
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
            primary = Color(0xFF2E7D32),
            secondary = Color(0xFF388E3C),
            tertiary = Color(0xFF81C784),
            background = Color(0xFFDCFCE7),
            surface = Color(0xFFF0FDF4),
            surfaceVariant = Color(0xFFD1FAE5),
            onSurface = Color(0xFF064E3B),
            onSurfaceVariant = Color(0xFF047857),
            outline = Color(0xFF34D399)
        ),
        dark = PaletteVariant(
            primary = Color(0xFF81C784),
            secondary = Color(0xFF4ADE80),
            tertiary = Color(0xFFA7F3D0),
            background = Color(0xFF03211A),
            surface = Color(0xFF052E21),
            surfaceVariant = Color(0xFF0A3B29),
            onSurface = Color(0xFFDCFCE7),
            onSurfaceVariant = Color(0xFF6EE7B7),
            outline = Color(0xFF34D399)
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
