package com.talauncher.ui.icons

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import kotlin.math.max
import kotlin.math.min

/**
 * Applies theme colors to icon drawables while ensuring proper contrast and visibility.
 * Handles color transformation with alpha preservation and contrast enhancement
 * for both light and dark themes.
 *
 * Single Responsibility: Color application and contrast enhancement only.
 */
class IconColorApplier {

    /**
     * Applies a monochrome color filter to a drawable while preserving alpha channel.
     *
     * @param drawable The drawable to tint
     * @param color The theme color to apply
     * @param isDarkTheme Whether current theme is dark mode
     * @return A ColorFilter that applies the monochrome effect with proper contrast
     */
    fun createMonochromeFilter(
        color: Color,
        isDarkTheme: Boolean
    ): ColorFilter {
        val enhancedColor = enhanceContrastIfNeeded(color, isDarkTheme)

        // Use PorterDuff SRC_ATOP to preserve alpha while applying color
        return PorterDuffColorFilter(
            enhancedColor.toArgb(),
            PorterDuff.Mode.SRC_ATOP
        )
    }

    /**
     * Creates a desaturated (grayscale) version of a bitmap, then tints it.
     * This ensures all icons become monochrome before color is applied.
     *
     * @param drawable The source drawable
     * @param targetColor The color to apply after desaturation
     * @param iconSize The size to render the icon
     * @param isDarkTheme Whether current theme is dark mode
     * @return A new Bitmap with monochrome color applied
     */
    fun createMonochromeBitmap(
        drawable: Drawable,
        targetColor: Color,
        iconSize: Int,
        isDarkTheme: Boolean
    ): Bitmap {
        // Create bitmap from drawable
        val sourceBitmap = drawableToBitmap(drawable, iconSize)

        // Enhance color for better contrast
        val enhancedColor = enhanceContrastIfNeeded(targetColor, isDarkTheme)

        // Create output bitmap
        val outputBitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)

        // First pass: Convert to grayscale while preserving alpha
        val grayscalePaint = Paint().apply {
            colorFilter = createGrayscaleFilter()
        }
        canvas.drawBitmap(sourceBitmap, 0f, 0f, grayscalePaint)

        // Second pass: Apply theme color
        val colorPaint = Paint().apply {
            colorFilter = PorterDuffColorFilter(
                enhancedColor.toArgb(),
                PorterDuff.Mode.SRC_ATOP
            )
        }
        canvas.drawBitmap(outputBitmap, 0f, 0f, colorPaint)

        return outputBitmap
    }

    /**
     * Converts a Drawable to a Bitmap at the specified size.
     *
     * @param drawable The drawable to convert
     * @param size The target size in pixels
     * @return A Bitmap representation of the drawable
     */
    private fun drawableToBitmap(drawable: Drawable, size: Int): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            // Scale existing bitmap if needed
            val existingBitmap = drawable.bitmap
            if (existingBitmap.width == size && existingBitmap.height == size) {
                return existingBitmap
            }
            return Bitmap.createScaledBitmap(existingBitmap, size, size, true)
        }

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        return bitmap
    }

    /**
     * Creates a ColorFilter that converts any color to grayscale while preserving alpha.
     */
    private fun createGrayscaleFilter(): ColorFilter {
        val matrix = ColorMatrix().apply {
            setSaturation(0f) // Remove all color saturation
        }
        return ColorMatrixColorFilter(matrix)
    }

    /**
     * Enhances color contrast based on theme to ensure icons are visible.
     *
     * For dark themes: Lightens colors that are too dark
     * For light themes: Darkens colors that are too light
     *
     * @param color The original theme color
     * @param isDarkTheme Whether current theme is dark mode
     * @return An adjusted color with better contrast
     */
    private fun enhanceContrastIfNeeded(color: Color, isDarkTheme: Boolean): Color {
        val luminance = color.luminance()

        return when {
            // Dark theme: ensure color is bright enough (luminance > 0.3)
            isDarkTheme && luminance < MIN_LUMINANCE_DARK -> {
                brightenColor(color, MIN_LUMINANCE_DARK)
            }
            // Light theme: ensure color is dark enough (luminance < 0.6)
            !isDarkTheme && luminance > MAX_LUMINANCE_LIGHT -> {
                darkenColor(color, MAX_LUMINANCE_LIGHT)
            }
            else -> color
        }
    }

    /**
     * Brightens a color to meet the minimum luminance threshold.
     */
    private fun brightenColor(color: Color, targetLuminance: Float): Color {
        val currentLuminance = color.luminance()
        if (currentLuminance >= targetLuminance) return color

        // Scale RGB components to achieve target luminance
        val scale = (targetLuminance / currentLuminance.coerceAtLeast(0.01f))
            .coerceIn(1f, 3f)

        return Color(
            red = min(color.red * scale, 1f),
            green = min(color.green * scale, 1f),
            blue = min(color.blue * scale, 1f),
            alpha = color.alpha
        )
    }

    /**
     * Darkens a color to meet the maximum luminance threshold.
     */
    private fun darkenColor(color: Color, targetLuminance: Float): Color {
        val currentLuminance = color.luminance()
        if (currentLuminance <= targetLuminance) return color

        // Scale RGB components to achieve target luminance
        val scale = (targetLuminance / currentLuminance)
            .coerceIn(0.3f, 1f)

        return Color(
            red = color.red * scale,
            green = color.green * scale,
            blue = color.blue * scale,
            alpha = color.alpha
        )
    }

    companion object {
        // Minimum luminance for dark theme (icons must be bright enough)
        private const val MIN_LUMINANCE_DARK = 0.35f

        // Maximum luminance for light theme (icons must be dark enough)
        private const val MAX_LUMINANCE_LIGHT = 0.55f
    }
}
