package com.talauncher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ZenDarkColorScheme = darkColorScheme(
    primary = ZenAccentDark,
    secondary = ZenSecondaryDark,
    tertiary = ZenPrimaryDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    primaryContainer = ZenPrimaryDark,
    onPrimaryContainer = ZenAccentDark,
)

private val ZenLightColorScheme = lightColorScheme(
    primary = ZenPrimary,
    secondary = ZenSecondary,
    tertiary = ZenAccent,
    background = Background,
    surface = Surface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    primaryContainer = FocusModeBackground,
    onPrimaryContainer = ZenPrimary,
)

@Composable
fun TALauncherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> ZenDarkColorScheme
        else -> ZenLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}