package com.talauncher.icons

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class IconThemerTest {

    @Test
    fun `ensureContrast enforces minimum contrast`() {
        val background = Color.parseColor("#AA0000")
        val foreground = Color.parseColor("#880000")
        val adjusted = ensureContrast(foreground, background)
        val ratio = contrastRatio(adjusted, background)
        assertTrue("Contrast ratio should be >= 4.5 but was $ratio", ratio >= 4.5)
    }

    @Test
    fun `relativeLuminance matches known values`() {
        assertEquals(0.0, relativeLuminance(Color.BLACK), 0.0001)
        assertEquals(1.0, relativeLuminance(Color.WHITE), 0.0001)
        val mid = relativeLuminance(Color.GRAY)
        assertTrue(mid in 0.2..0.6)
    }

    @Test
    @Config(sdk = [28])
    fun `legacy drawable backgrounds adopt theme`() {
        val drawable = ColorDrawable(Color.BLUE)
        val themeColor = Color.RED
        val bitmap: Bitmap = themeDrawableForTesting(drawable, themeColor, 96)
        val center = bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)
        val expected = ColorUtils.blendARGB(Color.BLUE, themeColor, 0.75f)
        val distance = colorDistance(center, expected)
        assertTrue("Center pixel should be close to blended theme color", distance < 0.12)
    }

    private fun colorDistance(@ColorInt a: Int, @ColorInt b: Int): Double {
        val dr = Color.red(a) - Color.red(b)
        val dg = Color.green(a) - Color.green(b)
        val db = Color.blue(a) - Color.blue(b)
        return Math.sqrt((dr * dr + dg * dg + db * db).toDouble()) / 441.6729559300637
    }
}

