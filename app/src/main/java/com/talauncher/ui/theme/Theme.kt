package com.talauncher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
    colorPalette: String = "default",
    content: @Composable () -> Unit
) {
    val baseColorScheme = when {
        darkTheme -> MinimalDarkColorScheme
        else -> MinimalLightColorScheme
    }

    val colorScheme = when (colorPalette) {
        "warm" -> baseColorScheme.copy(
            primary = if (darkTheme) Color(0xFFFFB74D) else Color(0xFFE65100),
            secondary = if (darkTheme) Color(0xFFFF8A65) else Color(0xFFBF360C),
            tertiary = if (darkTheme) Color(0xFFFFF3E0) else Color(0xFF6D4C41)
        )
        "cool" -> baseColorScheme.copy(
            primary = if (darkTheme) Color(0xFF64B5F6) else Color(0xFF1976D2),
            secondary = if (darkTheme) Color(0xFF81C784) else Color(0xFF388E3C),
            tertiary = if (darkTheme) Color(0xFFE1F5FE) else Color(0xFF455A64)
        )
        "monochrome" -> baseColorScheme.copy(
            primary = if (darkTheme) Color(0xFFBDBDBD) else Color(0xFF424242),
            secondary = if (darkTheme) Color(0xFF9E9E9E) else Color(0xFF616161),
            tertiary = if (darkTheme) Color(0xFF757575) else Color(0xFF212121)
        )
        "nature" -> baseColorScheme.copy(
            primary = if (darkTheme) Color(0xFF81C784) else Color(0xFF2E7D32),
            secondary = if (darkTheme) Color(0xFFA5D6A7) else Color(0xFF1B5E20),
            tertiary = if (darkTheme) Color(0xFFDCEDC8) else Color(0xFF33691E)
        )
        else -> baseColorScheme // "default"
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PrimerTypography,
        content = content
    )
}