package com.talauncher.ui.icons

import android.graphics.drawable.Drawable
import android.util.Log
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Manages caching of rendered icon drawables for performance optimization.
 *
 * Uses an LRU (Least Recently Used) cache to store rendered icons in memory.
 * Cache keys are based on package name, theme color, and icon size to ensure
 * correct icons are returned for different configurations.
 *
 * Single Responsibility: Icon caching only.
 */
class IconCacheManager(
    maxSizeMB: Int = DEFAULT_CACHE_SIZE_MB
) {
    // Calculate max cache entries based on memory size
    // Assuming average icon is ~100KB
    private val maxEntries = (maxSizeMB * 1024 * 1024) / AVERAGE_ICON_SIZE_BYTES

    private val cache = LruCache<String, Drawable>(maxEntries.toInt())

    /**
     * Retrieves a cached icon if available.
     *
     * @param packageName The package name
     * @param themeColor The theme color used for rendering
     * @param iconSize The icon size in pixels
     * @param isDarkTheme Whether dark theme is active
     * @return The cached Drawable, or null if not found
     */
    fun get(
        packageName: String,
        themeColor: Color,
        iconSize: Int,
        isDarkTheme: Boolean
    ): Drawable? {
        val key = createCacheKey(packageName, themeColor, iconSize, isDarkTheme)
        return cache.get(key)
    }

    /**
     * Stores a rendered icon in the cache.
     *
     * @param packageName The package name
     * @param themeColor The theme color used for rendering
     * @param iconSize The icon size in pixels
     * @param isDarkTheme Whether dark theme is active
     * @param drawable The rendered drawable to cache
     */
    fun put(
        packageName: String,
        themeColor: Color,
        iconSize: Int,
        isDarkTheme: Boolean,
        drawable: Drawable
    ) {
        val key = createCacheKey(packageName, themeColor, iconSize, isDarkTheme)
        cache.put(key, drawable)
    }

    /**
     * Clears all cached icons.
     * Call this when theme changes or when memory pressure is detected.
     */
    fun clear() {
        cache.evictAll()
        Log.d(TAG, "Icon cache cleared")
    }

    /**
     * Invalidates cache entries for a specific package.
     * Call this when an app updates its icon.
     *
     * @param packageName The package to invalidate
     */
    fun invalidatePackage(packageName: String) {
        val snapshot = cache.snapshot()
        snapshot.keys.filter { it.startsWith("$packageName|") }
            .forEach { cache.remove(it) }
    }

    /**
     * Invalidates all cache entries for a specific theme color.
     * Call this when theme color changes.
     *
     * @param themeColor The color to invalidate
     */
    fun invalidateColor(themeColor: Color) {
        val colorHex = themeColor.toArgb().toString(16)
        val snapshot = cache.snapshot()
        snapshot.keys.filter { it.contains("|$colorHex|") }
            .forEach { cache.remove(it) }
    }

    /**
     * Returns cache statistics for monitoring.
     */
    fun getStats(): CacheStats {
        return CacheStats(
            size = cache.size(),
            maxSize = cache.maxSize(),
            hitCount = cache.hitCount(),
            missCount = cache.missCount(),
            hitRate = calculateHitRate()
        )
    }

    private fun calculateHitRate(): Float {
        val total = cache.hitCount() + cache.missCount()
        return if (total > 0) {
            cache.hitCount().toFloat() / total
        } else {
            0f
        }
    }

    /**
     * Creates a unique cache key from rendering parameters.
     *
     * Format: packageName|colorHex|size|isDark
     */
    private fun createCacheKey(
        packageName: String,
        themeColor: Color,
        iconSize: Int,
        isDarkTheme: Boolean
    ): String {
        val colorHex = themeColor.toArgb().toString(16)
        val darkSuffix = if (isDarkTheme) "d" else "l"
        return "$packageName|$colorHex|$iconSize|$darkSuffix"
    }

    data class CacheStats(
        val size: Int,
        val maxSize: Int,
        val hitCount: Int,
        val missCount: Int,
        val hitRate: Float
    ) {
        override fun toString(): String {
            return "CacheStats(size=$size/$maxSize, hits=$hitCount, misses=$missCount, hitRate=${String.format("%.2f%%", hitRate * 100)})"
        }
    }

    companion object {
        private const val TAG = "IconCacheManager"
        private const val DEFAULT_CACHE_SIZE_MB = 50
        private const val AVERAGE_ICON_SIZE_BYTES = 100_000 // ~100KB per icon
    }
}
