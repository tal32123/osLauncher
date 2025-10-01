package com.talauncher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.colorResource
import com.talauncher.R
import com.talauncher.data.model.ColorPaletteOption
import com.talauncher.data.model.ThemeModeOption

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
    ColorPaletteOption.BLACK_AND_WHITE to PaletteDefinition(
        light = PaletteVariant(
            primary = MinimalNeutral900,
            secondary = MinimalNeutral700,
            tertiary = MinimalNeutral600,
            background = Color.White,
            surface = MinimalNeutral50,
            surfaceVariant = MinimalNeutral100,
            onSurface = MinimalNeutral900,
            onSurfaceVariant = MinimalNeutral700,
            outline = MinimalNeutral400
        ),
        dark = PaletteVariant(
            primary = MinimalNeutral100,
            secondary = MinimalNeutral300,
            tertiary = MinimalNeutral400,
            background = Color(0xFF0A0A0A),
            surface = MinimalNeutral900,
            surfaceVariant = MinimalNeutral800,
            onSurface = MinimalNeutral100,
            onSurfaceVariant = MinimalNeutral300,
            outline = MinimalNeutral600
        )
    ),
    // CUSTOM palette will be generated dynamically based on user selection
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
    ),
    ColorPaletteOption.OCEANIC to PaletteDefinition(
        light = PaletteVariant(
            primary = Color(0xFF0891B2),
            secondary = Color(0xFF0284C7),
            tertiary = Color(0xFF164E63),
            background = Color(0xFFF0F9FF),
            surface = Color(0xFFECFEFF),
            surfaceVariant = Color(0xFFBFDBFE),
            onSurface = Color(0xFF0C4A6E),
            onSurfaceVariant = Color(0xFF0369A1),
            outline = Color(0xFF0891B2)
        ),
        dark = PaletteVariant(
            primary = Color(0xFF06B6D4),
            secondary = Color(0xFF38BDF8),
            tertiary = Color(0xFF67E8F9),
            background = Color(0xFF083344),
            surface = Color(0xFF0F172A),
            surfaceVariant = Color(0xFF1E293B),
            onSurface = Color(0xFFE0F7FA),
            onSurfaceVariant = Color(0xFFB3E5FC),
            outline = Color(0xFF0891B2)
        )
    ),
    ColorPaletteOption.SUNSET to PaletteDefinition(
        light = PaletteVariant(
            primary = Color(0xFFEA580C),
            secondary = Color(0xFFDC2626),
            tertiary = Color(0xFFA21CAF),
            background = Color(0xFFFEF2F2),
            surface = Color(0xFFFFF7ED),
            surfaceVariant = Color(0xFFFED7AA),
            onSurface = Color(0xFF9A3412),
            onSurfaceVariant = Color(0xFFB45309),
            outline = Color(0xFFEA580C)
        ),
        dark = PaletteVariant(
            primary = Color(0xFFFB923C),
            secondary = Color(0xFFEF4444),
            tertiary = Color(0xFFC084FC),
            background = Color(0xFF1F1917),
            surface = Color(0xFF292524),
            surfaceVariant = Color(0xFF44403C),
            onSurface = Color(0xFFFED7AA),
            onSurfaceVariant = Color(0xFFFFBF69),
            outline = Color(0xFFFB923C)
        )
    ),
    ColorPaletteOption.LAVENDER to PaletteDefinition(
        light = PaletteVariant(
            primary = Color(0xFF7C3AED),
            secondary = Color(0xFF8B5CF6),
            tertiary = Color(0xFF6D28D9),
            background = Color(0xFFFAF5FF),
            surface = Color(0xFFF3E8FF),
            surfaceVariant = Color(0xFFE9D5FF),
            onSurface = Color(0xFF5B21B6),
            onSurfaceVariant = Color(0xFF6D28D9),
            outline = Color(0xFF8B5CF6)
        ),
        dark = PaletteVariant(
            primary = Color(0xFFA78BFA),
            secondary = Color(0xFFC084FC),
            tertiary = Color(0xFFDDD6FE),
            background = Color(0xFF1E1B3A),
            surface = Color(0xFF2E1065),
            surfaceVariant = Color(0xFF3730A3),
            onSurface = Color(0xFFE9D5FF),
            onSurfaceVariant = Color(0xFFC4B5FD),
            outline = Color(0xFFA78BFA)
        )
    ),
    ColorPaletteOption.CHERRY to PaletteDefinition(
        light = PaletteVariant(
            primary = Color(0xFFE11D48),
            secondary = Color(0xFFDB2777),
            tertiary = Color(0xFF9F1239),
            background = Color(0xFFFEF2F2),
            surface = Color(0xFFFEF7F7),
            surfaceVariant = Color(0xFFFECDD3),
            onSurface = Color(0xFF881337),
            onSurfaceVariant = Color(0xFFA11043),
            outline = Color(0xFFE11D48)
        ),
        dark = PaletteVariant(
            primary = Color(0xFFF87171),
            secondary = Color(0xFFFBBF24),
            tertiary = Color(0xFFFED7AA),
            background = Color(0xFF2D1B1B),
            surface = Color(0xFF3F1F1F),
            surfaceVariant = Color(0xFF4C1D1D),
            onSurface = Color(0xFFFECDD3),
            onSurfaceVariant = Color(0xFFFCA5A5),
            outline = Color(0xFFF87171)
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
    themeMode: ThemeModeOption = ThemeModeOption.SYSTEM,
    colorPalette: ColorPaletteOption = ColorPaletteOption.DEFAULT,
    customColorOption: String? = null,
    customPrimaryColor: String? = null,
    customSecondaryColor: String? = null,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeModeOption.SYSTEM -> isSystemInDarkTheme()
        ThemeModeOption.LIGHT -> false
        ThemeModeOption.DARK -> true
    }

    val baseColorScheme = when {
        darkTheme -> MinimalDarkColorScheme
        else -> MinimalLightColorScheme
    }

    val paletteDefinition = when {
        colorPalette == ColorPaletteOption.CUSTOM && (customColorOption != null || customPrimaryColor != null) -> {
            createCustomPaletteDefinition(customColorOption, customPrimaryColor, customSecondaryColor)
        }
        else -> PaletteCatalog[colorPalette] ?: PaletteCatalog.getValue(ColorPaletteOption.DEFAULT)
    }
    val colorScheme = baseColorScheme.applyPalette(paletteDefinition, darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PrimerTypography,
        content = content
    )
}

private fun createCustomPaletteDefinition(
    customColorOption: String? = null,
    customPrimaryColor: String? = null,
    customSecondaryColor: String? = null
): PaletteDefinition {
    // Priority: direct color values > named option > fallback
    val primary = when {
        customPrimaryColor != null -> parseHexColor(customPrimaryColor) ?: Color(0xFF2196F3)
        customColorOption != null -> {
            ColorPalettes.CustomColorOptions[customColorOption]?.get("primary")
                ?: ColorPalettes.CustomColorOptions["Purple"]!!["primary"]!!
        }
        else -> Color(0xFF2196F3) // Default blue
    }

    val secondary = when {
        customSecondaryColor != null -> parseHexColor(customSecondaryColor) ?: primary.copy(alpha = 0.8f)
        customColorOption != null -> {
            ColorPalettes.CustomColorOptions[customColorOption]?.get("primary")?.copy(alpha = 0.8f)
                ?: primary.copy(alpha = 0.8f)
        }
        else -> primary.copy(alpha = 0.8f)
    }

    // Use colors from named option if available, otherwise generate from primary
    val baseColors = if (customColorOption != null) {
        ColorPalettes.CustomColorOptions[customColorOption] ?: ColorPalettes.CustomColorOptions["Purple"]!!
    } else {
        mapOf(
            "surface" to Color(0xFFFFFFFF),
            "background" to Color(0xFFF8F9FA),
            "onSurface" to Color(0xFF1A1A1A)
        )
    }

    val surface = baseColors["surface"] ?: Color(0xFFFFFFFF)
    val background = baseColors["background"] ?: Color(0xFFF8F9FA)
    val onSurface = baseColors["onSurface"] ?: Color(0xFF1A1A1A)

    // Generate additional colors based on the primary color
    val tertiary = primary.copy(alpha = 0.6f)
    val surfaceVariant = surface.copy(alpha = 0.9f)
    val onSurfaceVariant = onSurface.copy(alpha = 0.7f)
    val outline = primary.copy(alpha = 0.4f)

    return PaletteDefinition(
        light = PaletteVariant(
            primary = primary,
            secondary = secondary,
            tertiary = tertiary,
            background = background,
            surface = surface,
            surfaceVariant = surfaceVariant,
            onSurface = onSurface,
            onSurfaceVariant = onSurfaceVariant,
            outline = outline
        ),
        dark = PaletteVariant(
            primary = primary.copy(alpha = 0.9f),
            secondary = secondary.copy(alpha = 0.8f),
            tertiary = tertiary.copy(alpha = 0.7f),
            background = Color(0xFF0A0A0A),
            surface = Color(0xFF141414),
            surfaceVariant = Color(0xFF1E1E1E),
            onSurface = Color(0xFFE5E5E5),
            onSurfaceVariant = Color(0xFFB0B0B0),
            outline = primary.copy(alpha = 0.5f)
        )
    )
}

private fun parseHexColor(hexColor: String): Color? {
    return try {
        val cleanHex = hexColor.removePrefix("#")
        val rgb = when (cleanHex.length) {
            6 -> cleanHex.toLong(16)
            8 -> cleanHex.toLong(16)
            else -> return null
        }
        Color(rgb.toULong())
    } catch (e: NumberFormatException) {
        null
    }
}
