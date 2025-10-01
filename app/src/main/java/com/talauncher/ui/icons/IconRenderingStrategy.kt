package com.talauncher.ui.icons

import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color

/**
 * Strategy interface for rendering different types of app icons.
 * Follows the Strategy pattern to handle different icon types (Adaptive, Legacy, Monochrome)
 * across various Android API levels.
 */
interface IconRenderingStrategy {
    /**
     * Renders an icon with the specified theme color.
     *
     * @param iconDrawable The original icon drawable from PackageManager
     * @param themeColor The theme color to apply
     * @param iconSize The desired icon size in pixels
     * @param isDarkTheme Whether the current theme is dark
     * @return A rendered Drawable with theme color applied, or null if rendering fails
     */
    fun renderIcon(
        iconDrawable: Drawable,
        themeColor: Color,
        iconSize: Int,
        isDarkTheme: Boolean
    ): Drawable?

    /**
     * Checks if this strategy can handle the given drawable type.
     *
     * @param iconDrawable The icon drawable to check
     * @return true if this strategy can handle the drawable, false otherwise
     */
    fun canHandle(iconDrawable: Drawable): Boolean
}
