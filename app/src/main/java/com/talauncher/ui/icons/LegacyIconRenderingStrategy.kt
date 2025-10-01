package com.talauncher.ui.icons

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import androidx.compose.ui.graphics.Color

/**
 * Rendering strategy for legacy icons (API < 26) and non-adaptive icons.
 *
 * Handles bitmap icons, vector drawables, and XML drawables by converting them
 * to monochrome bitmaps with the theme color applied.
 *
 * Follows the Strategy pattern and Single Responsibility Principle.
 */
class LegacyIconRenderingStrategy(
    private val colorApplier: IconColorApplier
) : IconRenderingStrategy {

    override fun canHandle(iconDrawable: Drawable): Boolean {
        // Handle legacy icons on older API levels
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return true
        }

        // On newer API levels, handle non-adaptive icons
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            iconDrawable !is android.graphics.drawable.AdaptiveIconDrawable
    }

    override fun renderIcon(
        iconDrawable: Drawable,
        themeColor: Color,
        iconSize: Int,
        isDarkTheme: Boolean
    ): Drawable? {
        return try {
            val monochromeBitmap = colorApplier.createMonochromeBitmap(
                iconDrawable,
                themeColor,
                iconSize,
                isDarkTheme
            )

            BitmapDrawable(null, monochromeBitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to render legacy icon", e)
            null
        }
    }

    companion object {
        private const val TAG = "LegacyIconStrategy"
    }
}
