package com.talauncher.ui.icons

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import androidx.compose.ui.graphics.Color

/**
 * Main entry point for icon rendering.
 *
 * Coordinates icon extraction, strategy selection, rendering, and caching.
 * This class serves as the Facade for the icon rendering subsystem.
 *
 * Usage:
 * ```kotlin
 * val renderer = IconRenderer(context)
 * val themedIcon = renderer.renderThemedIcon(packageName, themeColor, iconSize, isDarkTheme)
 * val originalIcon = renderer.renderOriginalIcon(packageName)
 * ```
 *
 * Follows the Facade pattern to provide a simple interface to a complex subsystem.
 */
class IconRenderer(
    context: Context,
    private val cacheManager: IconCacheManager = IconCacheManager()
) {
    private val iconExtractor = IconExtractor(context)
    private val colorApplier = IconColorApplier()

    // Initialize rendering strategies
    private val strategies: List<IconRenderingStrategy> = buildList {
        // API 33+: Try monochrome icons first (best quality)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val adaptiveStrategy = AdaptiveIconRenderingStrategy(colorApplier)
            add(MonochromeIconRenderingStrategy(colorApplier, adaptiveStrategy))
        }

        // API 26+: Try adaptive icons
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            add(AdaptiveIconRenderingStrategy(colorApplier))
        }

        // Fallback: Legacy icon strategy (works on all API levels)
        add(LegacyIconRenderingStrategy(colorApplier))
    }

    /**
     * Renders an icon with theme color applied (monochrome).
     *
     * @param packageName The package to render icon for
     * @param themeColor The theme color to apply
     * @param iconSize The desired icon size in pixels
     * @param isDarkTheme Whether current theme is dark mode
     * @return A themed Drawable, or null if rendering fails
     */
    fun renderThemedIcon(
        packageName: String,
        themeColor: Color,
        iconSize: Int,
        isDarkTheme: Boolean
    ): Drawable? {
        // Check cache first
        val cached = cacheManager.get(packageName, themeColor, iconSize, isDarkTheme)
        if (cached != null) {
            return cached
        }

        // Extract original icon
        val originalIcon = iconExtractor.extractIcon(packageName) ?: run {
            Log.w(TAG, "Failed to extract icon for $packageName")
            return null
        }

        // Find appropriate strategy and render
        val rendered = renderWithStrategy(originalIcon, themeColor, iconSize, isDarkTheme)
            ?: run {
                Log.w(TAG, "Failed to render icon for $packageName")
                return null
            }

        // Cache the result
        cacheManager.put(packageName, themeColor, iconSize, isDarkTheme, rendered)

        return rendered
    }

    /**
     * Renders an icon in its original colors (no theming applied).
     *
     * @param packageName The package to render icon for
     * @return The original Drawable, or null if extraction fails
     */
    fun renderOriginalIcon(packageName: String): Drawable? {
        return iconExtractor.extractIcon(packageName)
    }

    /**
     * Finds the appropriate strategy and renders the icon.
     */
    private fun renderWithStrategy(
        iconDrawable: Drawable,
        themeColor: Color,
        iconSize: Int,
        isDarkTheme: Boolean
    ): Drawable? {
        for (strategy in strategies) {
            if (strategy.canHandle(iconDrawable)) {
                val rendered = strategy.renderIcon(iconDrawable, themeColor, iconSize, isDarkTheme)
                if (rendered != null) {
                    Log.d(TAG, "Rendered icon using ${strategy::class.simpleName}")
                    return rendered
                }
            }
        }

        Log.w(TAG, "No strategy could handle icon drawable: ${iconDrawable::class.simpleName}")
        return null
    }

    /**
     * Clears the icon cache.
     * Call this when theme changes or memory pressure is detected.
     */
    fun clearCache() {
        cacheManager.clear()
    }

    /**
     * Invalidates cached icons for a specific package.
     * Call this when an app is updated.
     *
     * @param packageName The package to invalidate
     */
    fun invalidatePackage(packageName: String) {
        cacheManager.invalidatePackage(packageName)
    }

    /**
     * Invalidates all cached icons for a specific color.
     * Call this when theme color changes.
     *
     * @param themeColor The color to invalidate
     */
    fun invalidateColor(themeColor: Color) {
        cacheManager.invalidateColor(themeColor)
    }

    /**
     * Returns cache statistics for monitoring performance.
     */
    fun getCacheStats(): IconCacheManager.CacheStats {
        return cacheManager.getStats()
    }

    companion object {
        private const val TAG = "IconRenderer"
    }
}
