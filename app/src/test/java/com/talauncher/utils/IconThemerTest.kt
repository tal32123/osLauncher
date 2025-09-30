package com.talauncher.utils

import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class IconThemerTest {

    @Before
    fun setUp() {
        IconThemer.clearCache()
    }

    @Test
    fun ensureContrast_increasesContrastWhenNeeded() {
        val lowContrast = Color.rgb(120, 120, 120)
        val background = Color.rgb(140, 0, 0)
        val adjusted = IconThemer.ensureContrast(lowContrast, background)
        val ratio = IconThemer.contrastRatio(adjusted, background)
        assertTrue("Expected ratio >= 4.5 but was $ratio", ratio >= 4.5)
    }

    @Test
    fun contrastRatio_matchesWCAGForBlackAndWhite() {
        val ratio = IconThemer.contrastRatio(Color.BLACK, Color.WHITE)
        assertEquals(21.0, ratio, 0.1)
    }

    @Test
    fun relativeLuminance_handlesPrimaryColors() {
        val redLuminance = IconThemer.relativeLuminance(Color.RED)
        val greenLuminance = IconThemer.relativeLuminance(Color.GREEN)
        assertTrue(greenLuminance > redLuminance)
    }

    @Test
    fun themingBitmapDrawable_blendsBackgroundTowardThemeColor() {
        val size = 96
        val drawable = ColorDrawable(Color.BLUE)
        val themed = IconThemer.themeDrawableForTests(drawable, Color.RED, size)
        val center = themed.getPixel(size / 2, size / 2)
        val ratio = IconThemer.contrastRatio(center, Color.RED)
        assertTrue("Center pixel should lean toward theme color", ratio < 3.0)
    }

    @Test
    fun themingAdaptiveIcon_preservesForegroundContrast() {
        val size = 128
        val background = ColorDrawable(Color.WHITE)
        val foreground = ColorDrawable(Color.BLACK)
        val adaptive = AdaptiveIconDrawable(background, foreground)
        val themed = IconThemer.themeDrawableForTests(adaptive, Color.parseColor("#FF3D00"), size)
        val center = themed.getPixel(size / 2, size / 2)
        val ratio = IconThemer.contrastRatio(center, Color.parseColor("#FF3D00"))
        assertTrue("Foreground should contrast strongly", ratio >= 4.5)
    }
}

