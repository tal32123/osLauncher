package com.talauncher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.talauncher.ui.icons.IconRenderer
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Visual recognition tests to ensure themed icons remain distinguishable.
 *
 * This test verifies that:
 * 1. Icons don't become solid colored shapes
 * 2. Different apps have visually distinct icons
 * 3. Icons maintain recognizable features in both dark and light themes
 */
@RunWith(AndroidJUnit4::class)
class IconVisualRecognitionTest {

    private lateinit var context: Context
    private lateinit var iconRenderer: IconRenderer

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        iconRenderer = IconRenderer(context)
    }

    @Test
    fun themedIcons_haveSufficientVisualComplexity() {
        // Test with multiple system apps that should have distinct icons
        val testPackages = listOf(
            "com.android.settings",
            "com.android.chrome",
            "com.android.calculator2"
        )

        val themeColor = Color(0xFF6366F1) // Indigo
        val iconSize = 256 // Larger size for better analysis

        for (packageName in testPackages) {
            val lightIcon = iconRenderer.renderThemedIcon(
                packageName = packageName,
                themeColor = themeColor,
                iconSize = iconSize,
                isDarkTheme = false
            )

            val darkIcon = iconRenderer.renderThemedIcon(
                packageName = packageName,
                themeColor = themeColor,
                iconSize = iconSize,
                isDarkTheme = true
            )

            if (lightIcon != null) {
                val lightBitmap = drawableToBitmap(lightIcon, iconSize)
                val lightComplexity = calculateVisualComplexity(lightBitmap)

                assertTrue(
                    "Light theme icon for $packageName should have sufficient detail (got $lightComplexity)",
                    lightComplexity > MIN_VISUAL_COMPLEXITY
                )
            }

            if (darkIcon != null) {
                val darkBitmap = drawableToBitmap(darkIcon, iconSize)
                val darkComplexity = calculateVisualComplexity(darkBitmap)

                assertTrue(
                    "Dark theme icon for $packageName should have sufficient detail (got $darkComplexity)",
                    darkComplexity > MIN_VISUAL_COMPLEXITY
                )
            }
        }
    }

    @Test
    fun themedIcons_areVisuallySimilarToOriginals() {
        val packageName = "com.android.settings"
        val themeColor = Color(0xFF6366F1)
        val iconSize = 256

        val originalIcon = iconRenderer.renderOriginalIcon(packageName)
        val themedIcon = iconRenderer.renderThemedIcon(
            packageName = packageName,
            themeColor = themeColor,
            iconSize = iconSize,
            isDarkTheme = false
        )

        assertNotNull("Original icon should exist", originalIcon)
        assertNotNull("Themed icon should exist", themedIcon)

        val originalBitmap = drawableToBitmap(originalIcon!!, iconSize)
        val themedBitmap = drawableToBitmap(themedIcon!!, iconSize)

        // Calculate structural similarity - themed should preserve shape
        val similarity = calculateStructuralSimilarity(originalBitmap, themedBitmap)

        assertTrue(
            "Themed icon should preserve original structure (similarity: $similarity)",
            similarity > MIN_STRUCTURAL_SIMILARITY
        )
    }

    @Test
    fun differentApps_produceDistinguishableThemedIcons() {
        val packages = listOf(
            "com.android.settings",
            "com.android.chrome"
        )

        val themeColor = Color(0xFF6366F1)
        val iconSize = 256

        val icons = packages.mapNotNull { pkg ->
            iconRenderer.renderThemedIcon(pkg, themeColor, iconSize, isDarkTheme = false)
        }

        assertTrue("Should have at least 2 icons", icons.size >= 2)

        // Convert to bitmaps
        val bitmaps = icons.map { drawableToBitmap(it, iconSize) }

        // Compare each pair - they should be different
        for (i in 0 until bitmaps.size - 1) {
            for (j in i + 1 until bitmaps.size) {
                val similarity = calculateStructuralSimilarity(bitmaps[i], bitmaps[j])

                assertTrue(
                    "Icons for ${packages[i]} and ${packages[j]} should be distinguishable (similarity: $similarity)",
                    similarity < MAX_SIMILAR_ICONS_THRESHOLD
                )
            }
        }
    }

    /**
     * Calculates visual complexity by measuring edge density.
     * Returns a value between 0.0 (solid color) and 1.0 (maximum detail).
     */
    private fun calculateVisualComplexity(bitmap: Bitmap): Double {
        val width = bitmap.width
        val height = bitmap.height
        var edgeCount = 0
        var totalPixels = 0

        // Simple edge detection - count pixels that differ from their neighbors
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val centerPixel = bitmap.getPixel(x, y)
                val rightPixel = bitmap.getPixel(x + 1, y)
                val bottomPixel = bitmap.getPixel(x, y + 1)

                // Check if there's a significant difference (edge)
                if (pixelDifference(centerPixel, rightPixel) > EDGE_THRESHOLD ||
                    pixelDifference(centerPixel, bottomPixel) > EDGE_THRESHOLD) {
                    edgeCount++
                }
                totalPixels++
            }
        }

        return edgeCount.toDouble() / totalPixels.toDouble()
    }

    /**
     * Calculates structural similarity between two bitmaps.
     * Returns a value between 0.0 (completely different) and 1.0 (identical structure).
     */
    private fun calculateStructuralSimilarity(bitmap1: Bitmap, bitmap2: Bitmap): Double {
        if (bitmap1.width != bitmap2.width || bitmap1.height != bitmap2.height) {
            return 0.0
        }

        val width = bitmap1.width
        val height = bitmap1.height
        var matchingEdges = 0
        var totalEdges = 0

        // Compare edge patterns (structure) rather than colors
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val hasEdge1 = hasEdgeAtPixel(bitmap1, x, y)
                val hasEdge2 = hasEdgeAtPixel(bitmap2, x, y)

                if (hasEdge1 || hasEdge2) {
                    totalEdges++
                    if (hasEdge1 == hasEdge2) {
                        matchingEdges++
                    }
                }
            }
        }

        return if (totalEdges > 0) {
            matchingEdges.toDouble() / totalEdges.toDouble()
        } else {
            0.0
        }
    }

    private fun hasEdgeAtPixel(bitmap: Bitmap, x: Int, y: Int): Boolean {
        val centerPixel = bitmap.getPixel(x, y)
        val rightPixel = bitmap.getPixel(x + 1, y)
        val bottomPixel = bitmap.getPixel(x, y + 1)

        return pixelDifference(centerPixel, rightPixel) > EDGE_THRESHOLD ||
               pixelDifference(centerPixel, bottomPixel) > EDGE_THRESHOLD
    }

    private fun pixelDifference(pixel1: Int, pixel2: Int): Int {
        val r1 = (pixel1 shr 16) and 0xFF
        val g1 = (pixel1 shr 8) and 0xFF
        val b1 = pixel1 and 0xFF

        val r2 = (pixel2 shr 16) and 0xFF
        val g2 = (pixel2 shr 8) and 0xFF
        val b2 = pixel2 and 0xFF

        return kotlin.math.abs(r1 - r2) + kotlin.math.abs(g1 - g2) + kotlin.math.abs(b1 - b2)
    }

    private fun drawableToBitmap(drawable: android.graphics.drawable.Drawable, size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        return bitmap
    }

    companion object {
        // Minimum edge density for an icon to be considered detailed (not solid)
        // A solid color would have ~0, typical icons have 0.05-0.20
        private const val MIN_VISUAL_COMPLEXITY = 0.02

        // Minimum structural similarity to consider themed icon preserves original shape
        private const val MIN_STRUCTURAL_SIMILARITY = 0.60

        // Maximum similarity for icons to be considered distinguishable
        private const val MAX_SIMILAR_ICONS_THRESHOLD = 0.80

        // Pixel difference threshold to detect edges (0-765 scale)
        private const val EDGE_THRESHOLD = 100
    }
}
