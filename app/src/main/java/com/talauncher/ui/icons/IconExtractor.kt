package com.talauncher.ui.icons

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

/**
 * Extracts icon drawables from PackageManager with proper error handling.
 * Handles different Android versions and icon types.
 *
 * Single Responsibility: Icon extraction from system only.
 */
class IconExtractor(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    /**
     * Extracts the best available icon for the given package.
     *
     * @param packageName The package name to get icon for
     * @return The icon Drawable, or null if extraction fails
     */
    fun extractIcon(packageName: String): Drawable? {
        return try {
            val drawable = packageManager.getApplicationIcon(packageName)
            // Always return a mutated copy to avoid affecting other instances
            drawable.mutate()
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "Package not found: $packageName", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract icon for $packageName", e)
            null
        }
    }

    /**
     * Checks if the package supports adaptive icons (API 26+).
     *
     * @param packageName The package name to check
     * @return true if adaptive icon is available, false otherwise
     */
    fun hasAdaptiveIcon(packageName: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return false
        }

        return try {
            val drawable = packageManager.getApplicationIcon(packageName)
            isAdaptiveIcon(drawable)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if a drawable is an AdaptiveIconDrawable (API 26+).
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun isAdaptiveIcon(drawable: Drawable): Boolean {
        return drawable is android.graphics.drawable.AdaptiveIconDrawable
    }

    /**
     * Checks if the package supports monochrome icons (API 33+).
     *
     * @param packageName The package name to check
     * @return true if monochrome icon is available, false otherwise
     */
    fun hasMonochromeIcon(packageName: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return false
        }

        return try {
            val drawable = packageManager.getApplicationIcon(packageName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                checkMonochromeLayer(drawable)
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if an AdaptiveIconDrawable has a monochrome layer (API 33+).
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkMonochromeLayer(drawable: Drawable): Boolean {
        if (drawable !is android.graphics.drawable.AdaptiveIconDrawable) {
            return false
        }

        return try {
            drawable.monochrome != null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check monochrome layer", e)
            false
        }
    }

    companion object {
        private const val TAG = "IconExtractor"
    }
}
