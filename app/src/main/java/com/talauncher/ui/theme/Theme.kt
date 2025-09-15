package com.talauncher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// GitHub Primer-inspired color schemes
private val PrimerDarkColorScheme = darkColorScheme(
    primary = PrimerBlueDark,
    secondary = PrimerGray600Dark,
    tertiary = PrimerGray500Dark,
    background = PrimerGray50Dark,
    surface = PrimerGray100Dark,
    surfaceVariant = PrimerGray200Dark,
    surfaceContainer = PrimerGray200Dark,

    onPrimary = PrimerGray50Dark,
    onSecondary = PrimerGray900Dark,
    onTertiary = PrimerGray900Dark,
    onBackground = PrimerGray900Dark,
    onSurface = PrimerGray900Dark,
    onSurfaceVariant = PrimerGray700Dark,

    primaryContainer = FocusModeBackgroundDark,
    onPrimaryContainer = PrimerGray900Dark,
    secondaryContainer = PrimerGray300Dark,
    onSecondaryContainer = PrimerGray900Dark,

    outline = PrimerGray300Dark,
    outlineVariant = PrimerGray200Dark,
    scrim = Color.Black.copy(alpha = 0.6f),

    // Status colors
    error = PrimerRedDark,
    onError = PrimerGray50Dark,
    errorContainer = PrimerRedDark.copy(alpha = 0.1f),
    onErrorContainer = PrimerRedDark,
)

private val PrimerLightColorScheme = lightColorScheme(
    primary = PrimerBlue,
    secondary = PrimerGray700,
    tertiary = PrimerGray600,
    background = PrimerGray50,
    surface = Color.White,
    surfaceVariant = PrimerGray100,
    surfaceContainer = PrimerGray100,

    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = PrimerGray900,
    onSurface = PrimerGray900,
    onSurfaceVariant = PrimerGray700,

    primaryContainer = FocusModeBackground,
    onPrimaryContainer = PrimerBlue,
    secondaryContainer = PrimerGray100,
    onSecondaryContainer = PrimerGray900,

    outline = PrimerGray300,
    outlineVariant = PrimerGray200,
    scrim = Color.Black.copy(alpha = 0.6f),

    // Status colors
    error = PrimerRed,
    onError = Color.White,
    errorContainer = PrimerRed.copy(alpha = 0.1f),
    onErrorContainer = PrimerRed,
)

// Legacy color schemes for backward compatibility
private val ZenDarkColorScheme = PrimerDarkColorScheme
private val ZenLightColorScheme = PrimerLightColorScheme

@Composable
fun TALauncherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> PrimerDarkColorScheme
        else -> PrimerLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PrimerTypography,
        content = content
    )
}