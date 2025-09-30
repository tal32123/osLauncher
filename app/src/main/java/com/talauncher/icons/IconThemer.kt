package com.talauncher.icons

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import androidx.annotation.ColorInt
import androidx.annotation.Px
import androidx.annotation.VisibleForTesting
import androidx.core.graphics.ColorUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MIN_CONTRAST_RATIO = 4.5
private const val BACKGROUND_BLEND_RATIO = 0.75f
private const val DEFAULT_CORNER_RADIUS_FRACTION = 0.22f

private val iconCacheLock = Any()
private val iconCache = object : LruCache<String, Bitmap>(32 * 1024) {
    override fun sizeOf(key: String, value: Bitmap): Int {
        return value.byteCount / 1024
    }
}

/**
 * Themes an application icon so that the background adopts the provided [themeColor] while the
 * foreground glyph remains legible across API levels. Work is dispatched off the main thread.
 */
suspend fun themeIcon(
    context: Context,
    packageName: String,
    @ColorInt themeColor: Int,
    @Px sizePx: Int
): Bitmap = withContext(Dispatchers.Default) {
    val cacheKey = cacheKey(context, packageName, themeColor, sizePx)
    synchronized(iconCacheLock) {
        iconCache.get(cacheKey)
    }?.let { cached ->
        val config = cached.config ?: Bitmap.Config.ARGB_8888
        return@withContext cached.copy(config, false)
    }

    val drawable = loadIconDrawable(context, packageName)
    val bitmap = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && drawable is AdaptiveIconDrawable ->
            drawMonochromeAwareAdaptive(drawable, themeColor, sizePx)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable ->
            drawAdaptive(drawable, themeColor, sizePx)
        else ->
            drawLegacy(drawable, themeColor, sizePx)
    }

    synchronized(iconCacheLock) {
        iconCache.put(cacheKey, bitmap)
    }
    val config = bitmap.config ?: Bitmap.Config.ARGB_8888
    bitmap.copy(config, false)
}

/** Checks whether the target app exposes a monochrome layer for themed icons on Android 13+. */
fun supportsMonochrome(context: Context, packageName: String): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
    val drawable = runCatching { loadIconDrawable(context, packageName) }.getOrNull()
    return drawable is AdaptiveIconDrawable && drawable.monochrome != null
}

/**
 * Ensures the provided foreground color meets the desired contrast ratio against the background.
 */
@ColorInt
fun ensureContrast(
    @ColorInt fgColor: Int,
    @ColorInt bgColor: Int,
    minRatio: Double = MIN_CONTRAST_RATIO
): Int {
    if (contrastRatio(fgColor, bgColor) >= minRatio) return fgColor

    val fgHsl = FloatArray(3)
    ColorUtils.colorToHSL(fgColor, fgHsl)
    val bgLuminance = ColorUtils.calculateLuminance(bgColor)
    val targetLightness = if (bgLuminance > 0.5) 0.1f else 0.92f
    val adjustedHsl = fgHsl.copyOf()
    adjustedHsl[2] = targetLightness
    var candidate = ColorUtils.HSLToColor(adjustedHsl)

    if (contrastRatio(candidate, bgColor) >= minRatio) {
        return candidate
    }

    val whiteContrast = contrastRatio(Color.WHITE, bgColor)
    val blackContrast = contrastRatio(Color.BLACK, bgColor)
    candidate = if (whiteContrast >= blackContrast) Color.WHITE else Color.BLACK

    if (contrastRatio(candidate, bgColor) >= minRatio) {
        return candidate
    }

    // As a last resort, lerp between candidate and theme extremes until contrast is met.
    val extreme = if (bgLuminance > 0.5) Color.BLACK else Color.WHITE
    var blend = 0.5f
    repeat(6) {
        val blended = ColorUtils.blendARGB(candidate, extreme, blend)
        if (contrastRatio(blended, bgColor) >= minRatio) {
            return blended
        }
        blend = (blend + 1f) / 2f
    }
    return candidate
}

private fun cacheKey(
    context: Context,
    packageName: String,
    @ColorInt themeColor: Int,
    @Px sizePx: Int
): String {
    val nightMode = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
    return "$packageName|$themeColor|$sizePx|$nightMode|${Build.VERSION.SDK_INT}"
}

private fun loadIconDrawable(context: Context, packageName: String): Drawable {
    val pm = context.packageManager
    val appInfo = pm.getApplicationInfo(packageName, 0)
    val density = context.resources.displayMetrics.densityDpi
    val iconId = appInfo.icon
    val drawable = if (iconId != 0) {
        try {
            pm.getResourcesForApplication(appInfo).getDrawableForDensity(iconId, density, context.theme)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    } else null

    return (drawable ?: pm.getApplicationIcon(packageName)).mutate()
}

private fun drawMonochromeAwareAdaptive(
    drawable: AdaptiveIconDrawable,
    @ColorInt themeColor: Int,
    @Px sizePx: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val clipPath = adaptiveClipPath(sizePx, drawable)
    canvas.save()
    canvas.clipPath(clipPath)

    paint.color = themeColor
    canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), paint)

    canvas.restore()

    val foregroundColor = ensureContrast(Color.WHITE, themeColor)
    val monochrome = drawable.monochrome
    if (monochrome != null) {
        val fg = monochrome.mutate()
        fg.setTint(foregroundColor)
        fg.setBounds(0, 0, sizePx, sizePx)
        canvas.save()
        canvas.clipPath(clipPath)
        fg.draw(canvas)
        canvas.restore()
        return bitmap
    }

    // Fallback to adaptive drawing if monochrome is absent (should not happen on this path).
    return drawAdaptive(drawable, themeColor, sizePx)
}

private fun drawAdaptive(
    drawable: AdaptiveIconDrawable,
    @ColorInt themeColor: Int,
    @Px sizePx: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val clipPath = adaptiveClipPath(sizePx, drawable)

    canvas.save()
    canvas.clipPath(clipPath)

    val background = drawable.background?.mutate()
    if (background != null) {
        tintDrawable(background, themeColor)
        background.setBounds(0, 0, sizePx, sizePx)
        background.draw(canvas)
    } else {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = themeColor }
        canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), paint)
    }

    canvas.restore()

    val foregroundDrawable = drawable.foreground?.mutate()
    if (foregroundDrawable != null) {
        val fgBitmap = foregroundDrawable.toBitmap(sizePx)
        val avgColor = averageOpaqueColor(fgBitmap)
        val needsContrast = contrastRatio(avgColor, themeColor) < MIN_CONTRAST_RATIO
        val paint = if (needsContrast) Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = PorterDuffColorFilter(
                ensureContrast(avgColor, themeColor),
                PorterDuff.Mode.SRC_ATOP
            )
        } else null
        canvas.save()
        canvas.clipPath(clipPath)
        canvas.drawBitmap(fgBitmap, 0f, 0f, paint)
        canvas.restore()
    }

    return bitmap
}

private fun drawLegacy(
    drawable: Drawable,
    @ColorInt themeColor: Int,
    @Px sizePx: Int
): Bitmap {
    val baseBitmap = drawable.toBitmap(sizePx)
    val pixels = IntArray(sizePx * sizePx)
    baseBitmap.getPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)

    val bgColor = estimateBackgroundColor(pixels, sizePx, sizePx)
    val bgLuminance = ColorUtils.calculateLuminance(bgColor)

    for (i in pixels.indices) {
        val color = pixels[i]
        val alpha = Color.alpha(color)
        if (alpha <= 16) continue
        val distance = colorDistance(color, bgColor)
        val lumDiff = kotlin.math.abs(ColorUtils.calculateLuminance(color) - bgLuminance)
        if (distance < 0.16 && lumDiff < 0.16) {
            val blended = ColorUtils.blendARGB(bgColor, themeColor, BACKGROUND_BLEND_RATIO)
            pixels[i] = (alpha shl 24) or (blended and 0x00FFFFFF)
        } else {
            val adjusted = ensureContrast(color, themeColor)
            if (contrastRatio(color, themeColor) < MIN_CONTRAST_RATIO) {
                val blended = ColorUtils.blendARGB(color, adjusted, 0.6f)
                pixels[i] = (alpha shl 24) or (blended and 0x00FFFFFF)
            }
        }
    }

    val themedBitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    themedBitmap.setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)

    val masked = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(masked)
    val maskPath = roundedRectPath(sizePx)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    canvas.drawPath(maskPath, paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(themedBitmap, 0f, 0f, paint)
    paint.xfermode = null

    return masked
}

private fun tintDrawable(drawable: Drawable, @ColorInt color: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        drawable.setTint(color)
        drawable.setTintMode(PorterDuff.Mode.SRC_IN)
    } else {
        @Suppress("DEPRECATION")
        drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN)
    }
}

private fun Drawable.toBitmap(@Px sizePx: Int): Bitmap {
    if (this is BitmapDrawable && bitmap.width >= sizePx && bitmap.height >= sizePx) {
        return Bitmap.createScaledBitmap(bitmap, sizePx, sizePx, true)
    }
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val oldBounds = bounds
    setBounds(0, 0, sizePx, sizePx)
    draw(canvas)
    setBounds(oldBounds.left, oldBounds.top, oldBounds.right, oldBounds.bottom)
    return bitmap
}

private fun averageOpaqueColor(bitmap: Bitmap): Int {
    val width = bitmap.width
    val height = bitmap.height
    var r = 0.0
    var g = 0.0
    var b = 0.0
    var count = 0
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    for (color in pixels) {
        val alpha = Color.alpha(color)
        if (alpha < 32) continue
        r += Color.red(color)
        g += Color.green(color)
        b += Color.blue(color)
        count++
    }
    if (count == 0) return Color.WHITE
    return Color.rgb((r / count).toInt(), (g / count).toInt(), (b / count).toInt())
}

private fun estimateBackgroundColor(pixels: IntArray, width: Int, height: Int): Int {
    val samples = mutableListOf<Int>()
    val stepX = (width / 8).coerceAtLeast(1)
    val stepY = (height / 8).coerceAtLeast(1)
    for (x in 0 until width step stepX) {
        for (y in listOf(0, height - 1)) {
            val color = pixels[y * width + x]
            if (Color.alpha(color) > 64) samples.add(color)
        }
    }
    for (y in 0 until height step stepY) {
        for (x in listOf(0, width - 1)) {
            val color = pixels[y * width + x]
            if (Color.alpha(color) > 64) samples.add(color)
        }
    }
    if (samples.isEmpty()) {
        return ColorUtils.blendARGB(Color.BLACK, Color.WHITE, 0.5f)
    }
    var r = 0.0
    var g = 0.0
    var b = 0.0
    for (color in samples) {
        r += Color.red(color)
        g += Color.green(color)
        b += Color.blue(color)
    }
    return Color.rgb((r / samples.size).toInt(), (g / samples.size).toInt(), (b / samples.size).toInt())
}

private fun colorDistance(@ColorInt a: Int, @ColorInt b: Int): Double {
    val dr = Color.red(a) - Color.red(b)
    val dg = Color.green(a) - Color.green(b)
    val db = Color.blue(a) - Color.blue(b)
    return kotlin.math.sqrt((dr * dr + dg * dg + db * db).toDouble()) / 441.6729559300637
}

private fun adaptiveClipPath(@Px sizePx: Int, drawable: AdaptiveIconDrawable): Path {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val mask = Path(drawable.iconMask)
        val bounds = RectF()
        mask.computeBounds(bounds, true)
        val matrix = Matrix()
        val scaleX = sizePx / bounds.width()
        val scaleY = sizePx / bounds.height()
        matrix.setScale(scaleX, scaleY)
        matrix.postTranslate(-bounds.left * scaleX, -bounds.top * scaleY)
        mask.transform(matrix)
        mask
    } else {
        roundedRectPath(sizePx)
    }
}

private fun roundedRectPath(@Px sizePx: Int): Path {
    val radius = sizePx * DEFAULT_CORNER_RADIUS_FRACTION
    return Path().apply {
        addRoundRect(
            RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat()),
            radius,
            radius,
            Path.Direction.CW
        )
    }
}

@VisibleForTesting
internal fun themeDrawableForTesting(
    drawable: Drawable,
    @ColorInt themeColor: Int,
    @Px sizePx: Int
): Bitmap {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && drawable is AdaptiveIconDrawable && drawable.monochrome != null ->
            drawMonochromeAwareAdaptive(drawable, themeColor, sizePx)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable ->
            drawAdaptive(drawable, themeColor, sizePx)
        else ->
            drawLegacy(drawable, themeColor, sizePx)
    }
}

fun relativeLuminance(@ColorInt color: Int): Double {
    val r = Color.red(color) / 255.0
    val g = Color.green(color) / 255.0
    val b = Color.blue(color) / 255.0
    val rLin = if (r <= 0.03928) r / 12.92 else Math.pow((r + 0.055) / 1.055, 2.4)
    val gLin = if (g <= 0.03928) g / 12.92 else Math.pow((g + 0.055) / 1.055, 2.4)
    val bLin = if (b <= 0.03928) b / 12.92 else Math.pow((b + 0.055) / 1.055, 2.4)
    return 0.2126 * rLin + 0.7152 * gLin + 0.0722 * bLin
}

fun contrastRatio(@ColorInt a: Int, @ColorInt b: Int): Double {
    val l1 = relativeLuminance(a)
    val l2 = relativeLuminance(b)
    val light = maxOf(l1, l2)
    val dark = minOf(l1, l2)
    return (light + 0.05) / (dark + 0.05)
}

