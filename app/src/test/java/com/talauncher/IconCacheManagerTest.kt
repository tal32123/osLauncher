package com.talauncher

import android.graphics.drawable.ColorDrawable
import androidx.compose.ui.graphics.Color
import com.talauncher.ui.icons.IconCacheManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for IconCacheManager.
 * Tests caching, invalidation, and cache statistics.
 */
class IconCacheManagerTest {

    private lateinit var cacheManager: IconCacheManager

    @Before
    fun setup() {
        cacheManager = IconCacheManager(maxSizeMB = 1) // Small cache for testing
    }

    @Test
    fun `put and get icon from cache`() {
        val packageName = "com.test.app"
        val color = Color.Blue
        val size = 128
        val drawable = ColorDrawable(android.graphics.Color.BLUE)

        // Put in cache
        cacheManager.put(packageName, color, size, isDarkTheme = false, drawable)

        // Get from cache
        val cached = cacheManager.get(packageName, color, size, isDarkTheme = false)

        assertNotNull("Cached drawable should not be null", cached)
        assertEquals("Cached drawable should match", drawable, cached)
    }

    @Test
    fun `get returns null for non-existent entry`() {
        val cached = cacheManager.get("com.nonexistent", Color.Red, 128, isDarkTheme = false)

        assertNull("Should return null for non-existent entry", cached)
    }

    @Test
    fun `cache distinguishes between different colors`() {
        val packageName = "com.test.app"
        val size = 128
        val blueDrawable = ColorDrawable(android.graphics.Color.BLUE)
        val redDrawable = ColorDrawable(android.graphics.Color.RED)

        // Put with blue color
        cacheManager.put(packageName, Color.Blue, size, isDarkTheme = false, blueDrawable)

        // Put with red color
        cacheManager.put(packageName, Color.Red, size, isDarkTheme = false, redDrawable)

        // Get blue
        val cachedBlue = cacheManager.get(packageName, Color.Blue, size, isDarkTheme = false)
        assertEquals("Should get blue drawable", blueDrawable, cachedBlue)

        // Get red
        val cachedRed = cacheManager.get(packageName, Color.Red, size, isDarkTheme = false)
        assertEquals("Should get red drawable", redDrawable, cachedRed)
    }

    @Test
    fun `cache distinguishes between dark and light theme`() {
        val packageName = "com.test.app"
        val color = Color.Blue
        val size = 128
        val lightDrawable = ColorDrawable(android.graphics.Color.LTGRAY)
        val darkDrawable = ColorDrawable(android.graphics.Color.DKGRAY)

        // Put for light theme
        cacheManager.put(packageName, color, size, isDarkTheme = false, lightDrawable)

        // Put for dark theme
        cacheManager.put(packageName, color, size, isDarkTheme = true, darkDrawable)

        // Get for light theme
        val cachedLight = cacheManager.get(packageName, color, size, isDarkTheme = false)
        assertEquals("Should get light theme drawable", lightDrawable, cachedLight)

        // Get for dark theme
        val cachedDark = cacheManager.get(packageName, color, size, isDarkTheme = true)
        assertEquals("Should get dark theme drawable", darkDrawable, cachedDark)
    }

    @Test
    fun `clear removes all cached entries`() {
        // Add multiple entries
        cacheManager.put("com.app1", Color.Blue, 128, false, ColorDrawable(android.graphics.Color.BLUE))
        cacheManager.put("com.app2", Color.Red, 128, false, ColorDrawable(android.graphics.Color.RED))

        // Clear cache
        cacheManager.clear()

        // Verify entries are gone
        assertNull("Entry 1 should be cleared",
            cacheManager.get("com.app1", Color.Blue, 128, false))
        assertNull("Entry 2 should be cleared",
            cacheManager.get("com.app2", Color.Red, 128, false))
    }

    @Test
    fun `invalidatePackage removes only specified package`() {
        val package1 = "com.app1"
        val package2 = "com.app2"
        val color = Color.Blue
        val size = 128

        // Add entries for both packages
        cacheManager.put(package1, color, size, false, ColorDrawable(android.graphics.Color.BLUE))
        cacheManager.put(package2, color, size, false, ColorDrawable(android.graphics.Color.RED))

        // Invalidate package1
        cacheManager.invalidatePackage(package1)

        // Verify package1 is gone but package2 remains
        assertNull("Package1 should be invalidated",
            cacheManager.get(package1, color, size, false))
        assertNotNull("Package2 should remain",
            cacheManager.get(package2, color, size, false))
    }

    @Test
    fun `invalidateColor removes entries with specified color`() {
        val packageName = "com.test.app"
        val blueColor = Color.Blue
        val redColor = Color.Red
        val size = 128

        // Add entries with different colors
        cacheManager.put(packageName, blueColor, size, false, ColorDrawable(android.graphics.Color.BLUE))
        cacheManager.put(packageName, redColor, size, false, ColorDrawable(android.graphics.Color.RED))

        // Invalidate blue color
        cacheManager.invalidateColor(blueColor)

        // Verify blue is gone but red remains
        assertNull("Blue color entry should be invalidated",
            cacheManager.get(packageName, blueColor, size, false))
        assertNotNull("Red color entry should remain",
            cacheManager.get(packageName, redColor, size, false))
    }

    @Test
    fun `getStats returns accurate cache statistics`() {
        val packageName = "com.test.app"
        val color = Color.Blue
        val size = 128
        val drawable = ColorDrawable(android.graphics.Color.BLUE)

        // Add entry
        cacheManager.put(packageName, color, size, false, drawable)

        // Trigger hit
        cacheManager.get(packageName, color, size, false)

        // Trigger miss
        cacheManager.get("com.nonexistent", color, size, false)

        // Get stats
        val stats = cacheManager.getStats()

        assertTrue("Should have at least 1 hit", stats.hitCount >= 1)
        assertTrue("Should have at least 1 miss", stats.missCount >= 1)
        assertTrue("Size should be > 0", stats.size > 0)
        assertTrue("Hit rate should be between 0 and 1",
            stats.hitRate >= 0f && stats.hitRate <= 1f)
    }
}
