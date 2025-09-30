package com.talauncher.utils

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.collection.LruCache
import androidx.core.graphics.ColorUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 * IconThemer recolors app icons so that their background matches the provided theme color while the
 * glyph/foreground remains legible across Android API levels.
 */
object IconThemer {

    private const val TAG = "IconThemer"
    private const val SAFE_ZONE = 0.9f
    private const val BACKGROUND_EDGE_SAMPLE_RATIO = 0.15f
    private const val MAX_FOREGROUND_SCALE = 1.35f
    private const val MIN_FOREGROUND_SCALE = 0.65f

    private val cacheLock = Any()
    private val cache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(cacheSizeKb()) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.allocationByteCount / 1024
        }
    }

    private fun cacheSizeKb(): Int {
        val maxMemory = Runtime.getRuntime().maxMemory() / 1024
        val cacheSize = (maxMemory / 32).toInt()
        return max(cacheSize, 1024)
    }

    suspend fun themeIcon(
        context: Context,
        packageName: String,
        @ColorInt themeColor: Int,
        sizePx: Int
    ): Bitmap = withContext(Dispatchers.Default) {
        val key = listOf(packageName, themeColor, sizePx, Build.VERSION.SDK_INT).joinToString(":")
        synchronized(cacheLock) {
            cache.get(key)
        }?.let { return@withContext it }

        val pm = context.packageManager
        val drawable = try {
            pm.getApplicationIcon(packageName)
        } catch (notFound: PackageManager.NameNotFoundException) {
            Log.w(TAG, "Icon not found for $packageName, falling back to default icon", notFound)
            context.applicationInfo.loadIcon(pm)
        }

        drawable.mutate()

        val bitmap = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && drawable is AdaptiveIconDrawable && drawable.monochrome != null ->
                composeMonochrome(drawable, themeColor, sizePx)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable ->
                composeAdaptive(drawable, themeColor, sizePx)
            else ->
                composeBitmap(drawable, themeColor, sizePx)
        }

        synchronized(cacheLock) {
            cache.put(key, bitmap)
        }
        bitmap
    }

    fun supportsMonochrome(context: Context, packageName: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        return try {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            drawable is AdaptiveIconDrawable && drawable.monochrome != null
        } catch (notFound: PackageManager.NameNotFoundException) {
            false
        }
    }

    @ColorInt
    fun ensureContrast(@ColorInt fgColor: Int, @ColorInt bgColor: Int, minRatio: Double = 4.5): Int {
        val currentRatio = contrastRatio(fgColor, bgColor)
        if (currentRatio >= minRatio) return fgColor

        val whiteRatio = contrastRatio(Color.WHITE, bgColor)
        val blackRatio = contrastRatio(Color.BLACK, bgColor)

        var candidate = if (whiteRatio >= blackRatio) Color.WHITE else Color.BLACK
        var candidateRatio = max(whiteRatio, blackRatio)

        if (candidateRatio >= minRatio) {
            return candidate
        }

        candidate = if (candidate == Color.WHITE) Color.WHITE else Color.BLACK

        val blendTarget = candidate
        var bestColor = candidate
        var bestRatio = candidateRatio

        // Try to gradually blend towards the target (white or black) to reach the minimum ratio.
        val steps = 12
        for (i in 1..steps) {
            val t = i / steps.toFloat()
            val blended = ColorUtils.blendARGB(fgColor, blendTarget, t)
            val ratio = contrastRatio(blended, bgColor)
            if (ratio > bestRatio) {
                bestRatio = ratio
                bestColor = blended
                if (ratio >= minRatio) {
                    break
                }
            }
        }

        return bestColor
    }

    @VisibleForTesting
    fun clearCache() {
        synchronized(cacheLock) {
            cache.evictAll()
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun composeMonochrome(
        adaptive: AdaptiveIconDrawable,
        @ColorInt themeColor: Int,
        size: Int
    ): Bitmap {
        val bitmap = createBaseBitmap(size)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)

        val maskPath = adaptive.iconMask ?: createRoundedMask()
        val mask = scalePath(maskPath, size)

        paint.style = Paint.Style.FILL
        paint.color = themeColor
        canvas.drawPath(mask, paint)

        val foregroundColor = ensureContrast(Color.WHITE, themeColor)
        val monochromeDrawable = adaptive.monochrome?.mutate()
        monochromeDrawable?.let {
            it.setTint(foregroundColor)
            it.setTintMode(PorterDuff.Mode.SRC_IN)
            val inset = ((1f - SAFE_ZONE) * size / 2f).toInt()
            it.setBounds(inset, inset, size - inset, size - inset)
            it.draw(canvas)
        }

        return bitmap
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun composeAdaptive(
        adaptive: AdaptiveIconDrawable,
        @ColorInt themeColor: Int,
        size: Int
    ): Bitmap {
        val bitmap = createBaseBitmap(size)
        val canvas = Canvas(bitmap)

        val background = adaptive.background?.mutate()
        if (background != null) {
            background.setTint(themeColor)
            background.setTintMode(PorterDuff.Mode.SRC_IN)
            background.setBounds(0, 0, size, size)
            background.draw(canvas)
        } else {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.color = themeColor
            val maskPath = scalePath(adaptive.iconMask ?: createRoundedMask(), size)
            canvas.drawPath(maskPath, paint)
        }

        val foreground = adaptive.foreground?.mutate()
        if (foreground != null) {
            val inset = ((1f - SAFE_ZONE) * size / 2f).toInt()
            foreground.setBounds(inset, inset, size - inset, size - inset)

            val foregroundBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val fgCanvas = Canvas(foregroundBitmap)
            foreground.draw(fgCanvas)
            val avgForeground = sampleForegroundColor(foregroundBitmap)

            val ratio = contrastRatio(avgForeground, themeColor)
            if (ratio < 4.5) {
                val darkBackground = relativeLuminance(themeColor) < 0.5
                val matrix = ColorMatrix()
                val scale = if (darkBackground) MAX_FOREGROUND_SCALE else MIN_FOREGROUND_SCALE
                matrix.setScale(scale, scale, scale, 1f)
                foreground.colorFilter = ColorMatrixColorFilter(matrix)
            }

            foreground.draw(canvas)
        }

        return bitmap
    }

    private fun composeBitmap(
        drawable: Drawable,
        @ColorInt themeColor: Int,
        size: Int
    ): Bitmap {
        val bitmap = createBaseBitmap(size)
        val canvas = Canvas(bitmap)

        val base = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val baseCanvas = Canvas(base)

        drawable.setBounds(0, 0, size, size)
        drawable.draw(baseCanvas)

        val pixels = IntArray(size * size)
        base.getPixels(pixels, 0, size, 0, 0, size, size)

        val backgroundColor = estimateBackgroundColor(pixels, size)
        val overlayColor = ColorUtils.blendARGB(backgroundColor, themeColor, 0.8f)

        val recolored = IntArray(pixels.size)
        val bgLuminance = relativeLuminance(themeColor)
        var foregroundMisses = 0
        var foregroundTotal = 0

        for (index in pixels.indices) {
            val color = pixels[index]
            val alpha = Color.alpha(color)
            if (alpha == 0) {
                recolored[index] = Color.TRANSPARENT
                continue
            }

            val x = index % size
            val y = index / size
            val isEdge = x < size * BACKGROUND_EDGE_SAMPLE_RATIO ||
                x >= size * (1 - BACKGROUND_EDGE_SAMPLE_RATIO) ||
                y < size * BACKGROUND_EDGE_SAMPLE_RATIO ||
                y >= size * (1 - BACKGROUND_EDGE_SAMPLE_RATIO)

            val distance = colorDistance(color, backgroundColor)
            val treatAsBackground = (isEdge && distance < 80) || distance < 35

            if (treatAsBackground) {
                recolored[index] = overlayPixel(color, overlayColor)
            } else {
                foregroundTotal++
                val contrasted = ensureContrast(color, themeColor)
                val ratio = contrastRatio(contrasted, themeColor)
                if (ratio < 4.5) {
                    foregroundMisses++
                    val adjustTarget = if (bgLuminance < 0.5) Color.WHITE else Color.BLACK
                    recolored[index] = ColorUtils.blendARGB(color, adjustTarget, 0.35f)
                } else {
                    recolored[index] = contrasted
                }
            }
        }

        if (foregroundTotal > 0 && foregroundMisses > 0) {
            Log.d(TAG, "Foreground contrast misses: $foregroundMisses / $foregroundTotal")
        }

        bitmap.setPixels(recolored, 0, size, 0, 0, size, size)
        return bitmap
    }

    private fun overlayPixel(@ColorInt original: Int, @ColorInt overlay: Int): Int {
        val alpha = Color.alpha(original) / 255f
        val themed = ColorUtils.blendARGB(overlay, original, 0.2f)
        val r = (Color.red(themed) * alpha).toInt()
        val g = (Color.green(themed) * alpha).toInt()
        val b = (Color.blue(themed) * alpha).toInt()
        val a = Color.alpha(original)
        return Color.argb(a, r, g, b)
    }

    private fun colorDistance(@ColorInt start: Int, @ColorInt end: Int): Double {
        val startLab = DoubleArray(3)
        val endLab = DoubleArray(3)
        ColorUtils.colorToLAB(start, startLab)
        ColorUtils.colorToLAB(end, endLab)
        return ColorUtils.distanceEuclidean(startLab, endLab)
    }

    private fun estimateBackgroundColor(pixels: IntArray, size: Int): Int {
        var sumR = 0.0
        var sumG = 0.0
        var sumB = 0.0
        var count = 0

        val inset = max(1, (size * BACKGROUND_EDGE_SAMPLE_RATIO).toInt())
        for (y in 0 until size) {
            for (x in 0 until size) {
                if (x < inset || y < inset || x >= size - inset || y >= size - inset) {
                    val color = pixels[y * size + x]
                    val alpha = Color.alpha(color)
                    if (alpha > 80) {
                        sumR += Color.red(color)
                        sumG += Color.green(color)
                        sumB += Color.blue(color)
                        count++
                    }
                }
            }
        }

        if (count == 0) {
            // Fallback to average of all opaque pixels.
            for (color in pixels) {
                val alpha = Color.alpha(color)
                if (alpha > 80) {
                    sumR += Color.red(color)
                    sumG += Color.green(color)
                    sumB += Color.blue(color)
                    count++
                }
            }
        }

        if (count == 0) {
            return Color.TRANSPARENT
        }

        val r = (sumR / count).toInt()
        val g = (sumG / count).toInt()
        val b = (sumB / count).toInt()
        return Color.rgb(r, g, b)
    }

    private fun sampleForegroundColor(bitmap: Bitmap): Int {
        val size = bitmap.width
        val pixels = IntArray(size * size)
        bitmap.getPixels(pixels, 0, size, 0, 0, size, size)
        var sumR = 0.0
        var sumG = 0.0
        var sumB = 0.0
        var count = 0

        for (color in pixels) {
            val alpha = Color.alpha(color)
            if (alpha > 80) {
                sumR += Color.red(color)
                sumG += Color.green(color)
                sumB += Color.blue(color)
                count++
            }
        }

        if (count == 0) {
            return Color.WHITE
        }

        return Color.rgb((sumR / count).toInt(), (sumG / count).toInt(), (sumB / count).toInt())
    }

    private fun createBaseBitmap(size: Int): Bitmap {
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    }

    private fun createRoundedMask(): Path {
        val path = Path()
        path.addRoundRect(RectF(0f, 0f, 100f, 100f), 30f, 30f, Path.Direction.CW)
        return path
    }

    private fun scalePath(path: Path, size: Int): Path {
        val bounds = RectF()
        path.computeBounds(bounds, true)
        val matrix = Matrix()
        val scaleX = size / bounds.width()
        val scaleY = size / bounds.height()
        matrix.postTranslate(-bounds.left, -bounds.top)
        matrix.postScale(scaleX, scaleY)
        val scaled = Path(path)
        scaled.transform(matrix)
        return scaled
    }

    @VisibleForTesting
    fun contrastRatio(@ColorInt first: Int, @ColorInt second: Int): Double {
        val l1 = relativeLuminance(first)
        val l2 = relativeLuminance(second)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    @VisibleForTesting
    fun relativeLuminance(@ColorInt color: Int): Double {
        val r = linearizedComponent(Color.red(color) / 255.0)
        val g = linearizedComponent(Color.green(color) / 255.0)
        val b = linearizedComponent(Color.blue(color) / 255.0)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun linearizedComponent(component: Double): Double {
        return if (component <= 0.03928) {
            component / 12.92
        } else {
            Math.pow((component + 0.055) / 1.055, 2.4)
        }
    }

    @VisibleForTesting
    fun themeDrawableForTests(
        drawable: Drawable,
        @ColorInt themeColor: Int,
        size: Int
    ): Bitmap {
        drawable.mutate()
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable ->
                composeAdaptive(drawable, themeColor, size)
            else -> composeBitmap(drawable, themeColor, size)
        }
    }
}

