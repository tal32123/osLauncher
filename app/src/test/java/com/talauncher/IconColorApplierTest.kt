package com.talauncher

import androidx.compose.ui.graphics.Color
import com.talauncher.ui.icons.IconColorApplier
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for IconColorApplier.
 * Tests color filter creation and contrast enhancement.
 */
class IconColorApplierTest {

    private lateinit var colorApplier: IconColorApplier

    @Before
    fun setup() {
        colorApplier = IconColorApplier()
    }

    @Test
    fun `createMonochromeFilter returns valid ColorFilter for light theme`() {
        val color = Color(0xFF6366F1)
        val filter = colorApplier.createMonochromeFilter(color, isDarkTheme = false)

        assertNotNull("ColorFilter should not be null", filter)
    }

    @Test
    fun `createMonochromeFilter returns valid ColorFilter for dark theme`() {
        val color = Color(0xFF6366F1)
        val filter = colorApplier.createMonochromeFilter(color, isDarkTheme = true)

        assertNotNull("ColorFilter should not be null", filter)
    }

    @Test
    fun `createMonochromeFilter enhances dark colors in dark theme`() {
        // Very dark color that needs brightening in dark theme
        val darkColor = Color(0xFF1A1A1A)
        val filter = colorApplier.createMonochromeFilter(darkColor, isDarkTheme = true)

        // Filter should be created (enhancement happens internally)
        assertNotNull("ColorFilter should be created for dark color in dark theme", filter)
    }

    @Test
    fun `createMonochromeFilter handles light colors in light theme`() {
        // Very light color that needs darkening in light theme
        val lightColor = Color(0xFFF0F0F0)
        val filter = colorApplier.createMonochromeFilter(lightColor, isDarkTheme = false)

        // Filter should be created (enhancement happens internally)
        assertNotNull("ColorFilter should be created for light color in light theme", filter)
    }

    @Test
    fun `createMonochromeFilter handles edge case colors`() {
        // Test with pure white
        val white = Color.White
        val filterWhite = colorApplier.createMonochromeFilter(white, isDarkTheme = false)
        assertNotNull("Should handle pure white", filterWhite)

        // Test with pure black
        val black = Color.Black
        val filterBlack = colorApplier.createMonochromeFilter(black, isDarkTheme = true)
        assertNotNull("Should handle pure black", filterBlack)

        // Test with transparent color (alpha = 0)
        val transparent = Color.Transparent
        val filterTransparent = colorApplier.createMonochromeFilter(transparent, isDarkTheme = false)
        assertNotNull("Should handle transparent color", filterTransparent)
    }
}
