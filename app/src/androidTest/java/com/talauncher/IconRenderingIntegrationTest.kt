package com.talauncher

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.talauncher.ui.icons.IconRenderer
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for IconRenderer.
 * Tests end-to-end icon rendering with real PackageManager.
 */
@RunWith(AndroidJUnit4::class)
class IconRenderingIntegrationTest {

    private lateinit var context: Context
    private lateinit var iconRenderer: IconRenderer

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        iconRenderer = IconRenderer(context)
    }

    @Test
    fun renderThemedIcon_forSystemApp_returnsDrawable() {
        // Use Android Settings as a system app that should always exist
        val packageName = "com.android.settings"
        val themeColor = Color.Blue
        val iconSize = 128

        val drawable = iconRenderer.renderThemedIcon(
            packageName = packageName,
            themeColor = themeColor,
            iconSize = iconSize,
            isDarkTheme = false
        )

        assertNotNull("Themed icon should be rendered for system app", drawable)
        assertTrue("Icon should have valid size",
            drawable!!.intrinsicWidth > 0 && drawable.intrinsicHeight > 0)
    }

    @Test
    fun renderThemedIcon_forLauncherApp_returnsDrawable() {
        // Use our own app package
        val packageName = context.packageName
        val themeColor = Color.Red
        val iconSize = 128

        val drawable = iconRenderer.renderThemedIcon(
            packageName = packageName,
            themeColor = themeColor,
            iconSize = iconSize,
            isDarkTheme = true
        )

        assertNotNull("Themed icon should be rendered for launcher app", drawable)
    }

    @Test
    fun renderOriginalIcon_forSystemApp_returnsDrawable() {
        val packageName = "com.android.settings"

        val drawable = iconRenderer.renderOriginalIcon(packageName)

        assertNotNull("Original icon should be returned for system app", drawable)
        assertTrue("Icon should have valid size",
            drawable!!.intrinsicWidth > 0 && drawable.intrinsicHeight > 0)
    }

    @Test
    fun renderThemedIcon_withDifferentColors_producesDistinctResults() {
        val packageName = context.packageName
        val iconSize = 128

        val blueIcon = iconRenderer.renderThemedIcon(
            packageName, Color.Blue, iconSize, isDarkTheme = false
        )
        val redIcon = iconRenderer.renderThemedIcon(
            packageName, Color.Red, iconSize, isDarkTheme = false
        )

        assertNotNull("Blue themed icon should be rendered", blueIcon)
        assertNotNull("Red themed icon should be rendered", redIcon)
        // Icons should be different objects (not cached as same)
        assertNotSame("Different colored icons should be distinct", blueIcon, redIcon)
    }

    @Test
    fun renderThemedIcon_withCaching_improvesPerformance() {
        val packageName = context.packageName
        val themeColor = Color.Blue
        val iconSize = 128

        // First render (cache miss)
        val startTime1 = System.nanoTime()
        val drawable1 = iconRenderer.renderThemedIcon(
            packageName, themeColor, iconSize, isDarkTheme = false
        )
        val duration1 = System.nanoTime() - startTime1

        // Second render (cache hit)
        val startTime2 = System.nanoTime()
        val drawable2 = iconRenderer.renderThemedIcon(
            packageName, themeColor, iconSize, isDarkTheme = false
        )
        val duration2 = System.nanoTime() - startTime2

        assertNotNull("First render should succeed", drawable1)
        assertNotNull("Second render should succeed", drawable2)

        // Second render should be faster due to caching
        assertTrue("Cached render should be faster than first render",
            duration2 < duration1)
    }

    @Test
    fun renderThemedIcon_forNonExistentPackage_returnsNull() {
        val nonExistentPackage = "com.nonexistent.fake.package.that.does.not.exist"
        val drawable = iconRenderer.renderThemedIcon(
            nonExistentPackage, Color.Blue, 128, isDarkTheme = false
        )

        assertNull("Should return null for non-existent package", drawable)
    }

    @Test
    fun cacheInvalidation_clearsCache() {
        val packageName = context.packageName
        val themeColor = Color.Blue
        val iconSize = 128

        // Render to populate cache
        iconRenderer.renderThemedIcon(packageName, themeColor, iconSize, isDarkTheme = false)

        // Clear cache
        iconRenderer.clearCache()

        // Get cache stats
        val stats = iconRenderer.getCacheStats()

        // After clearing, size should be 0
        assertEquals("Cache should be empty after clear", 0, stats.size)
    }

    @Test
    fun renderThemedIcon_inDarkAndLightTheme_producesDistinctIcons() {
        val packageName = context.packageName
        val themeColor = Color(0xFF6366F1) // Indigo
        val iconSize = 128

        val lightIcon = iconRenderer.renderThemedIcon(
            packageName, themeColor, iconSize, isDarkTheme = false
        )
        val darkIcon = iconRenderer.renderThemedIcon(
            packageName, themeColor, iconSize, isDarkTheme = true
        )

        assertNotNull("Light theme icon should be rendered", lightIcon)
        assertNotNull("Dark theme icon should be rendered", darkIcon)
        // Should be different due to contrast enhancement
        assertNotSame("Dark and light theme icons should be distinct", lightIcon, darkIcon)
    }
}
