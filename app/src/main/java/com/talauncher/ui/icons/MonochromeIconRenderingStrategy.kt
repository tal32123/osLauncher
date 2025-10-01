package com.talauncher.ui.icons

import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color

/**
 * Rendering strategy for native monochrome icons (API 33+).
 *
 * Android 13 (API 33) introduced native monochrome icon support where apps can
 * provide a dedicated monochrome layer. This strategy uses that layer when available,
 * providing the best quality monochrome icons.
 *
 * Falls back to adaptive icon strategy if monochrome layer is not available.
 *
 * Follows the Strategy pattern and Single Responsibility Principle.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class MonochromeIconRenderingStrategy(
    private val colorApplier: IconColorApplier,
    private val adaptiveIconFallback: AdaptiveIconRenderingStrategy
) : IconRenderingStrategy {

    override fun canHandle(iconDrawable: Drawable): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return false
        }

        if (iconDrawable !is AdaptiveIconDrawable) {
            return false
        }

        return try {
            iconDrawable.monochrome != null
        } catch (e: Exception) {
            false
        }
    }

    override fun renderIcon(
        iconDrawable: Drawable,
        themeColor: Color,
        iconSize: Int,
        isDarkTheme: Boolean
    ): Drawable? {
        if (!canHandle(iconDrawable)) {
            // Fall back to adaptive icon strategy
            return adaptiveIconFallback.renderIcon(iconDrawable, themeColor, iconSize, isDarkTheme)
        }

        return try {
            val adaptiveIcon = iconDrawable as AdaptiveIconDrawable
            renderMonochromeIcon(adaptiveIcon, themeColor, iconSize, isDarkTheme)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to render monochrome icon, falling back", e)
            // Fall back to adaptive icon strategy
            adaptiveIconFallback.renderIcon(iconDrawable, themeColor, iconSize, isDarkTheme)
        }
    }

    /**
     * Renders a monochrome icon using the native monochrome layer.
     */
    private fun renderMonochromeIcon(
        adaptiveIcon: AdaptiveIconDrawable,
        themeColor: Color,
        iconSize: Int,
        isDarkTheme: Boolean
    ): Drawable {
        // Get the monochrome layer
        val monochromeLayer = adaptiveIcon.monochrome?.mutate()

        // Apply theme color to the monochrome layer
        val colorFilter = colorApplier.createMonochromeFilter(themeColor, isDarkTheme)
        monochromeLayer?.colorFilter = colorFilter

        // Create a new adaptive icon using the monochrome layer as foreground
        // Use a simple background (can be customized)
        val background = adaptiveIcon.background?.mutate()
        background?.colorFilter = colorFilter

        return AdaptiveIconDrawable(background, monochromeLayer)
    }

    companion object {
        private const val TAG = "MonochromeIconStrategy"
    }
}
