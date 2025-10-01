package com.talauncher.ui.icons

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color

/**
 * Rendering strategy for Adaptive Icons (API 26+).
 *
 * Adaptive icons have foreground and background layers that can be independently styled.
 * This strategy applies monochrome color to both layers while preserving their separation.
 *
 * Follows the Strategy pattern and Single Responsibility Principle.
 */
@RequiresApi(Build.VERSION_CODES.O)
class AdaptiveIconRenderingStrategy(
    private val colorApplier: IconColorApplier
) : IconRenderingStrategy {

    override fun canHandle(iconDrawable: Drawable): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            iconDrawable is AdaptiveIconDrawable
    }

    override fun renderIcon(
        iconDrawable: Drawable,
        themeColor: Color,
        iconSize: Int,
        isDarkTheme: Boolean
    ): Drawable? {
        if (!canHandle(iconDrawable)) {
            return null
        }

        return try {
            val adaptiveIcon = iconDrawable as AdaptiveIconDrawable
            renderAdaptiveIcon(adaptiveIcon, themeColor, iconSize, isDarkTheme)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to render adaptive icon", e)
            null
        }
    }

    /**
     * Renders an adaptive icon by applying monochrome color to both foreground and background layers.
     */
    private fun renderAdaptiveIcon(
        adaptiveIcon: AdaptiveIconDrawable,
        themeColor: Color,
        iconSize: Int,
        isDarkTheme: Boolean
    ): Drawable {
        // Create a new AdaptiveIconDrawable with colored layers
        val foreground = adaptiveIcon.foreground?.mutate()
        val background = adaptiveIcon.background?.mutate()

        // Apply color filter to both layers
        val colorFilter = colorApplier.createMonochromeFilter(themeColor, isDarkTheme)

        foreground?.colorFilter = colorFilter
        background?.colorFilter = colorFilter

        // Create new adaptive icon with colored layers
        return AdaptiveIconDrawable(background, foreground)
    }

    companion object {
        private const val TAG = "AdaptiveIconStrategy"
    }
}
