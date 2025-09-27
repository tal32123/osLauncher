package com.talauncher.utils

/**
 * Helper for IdlingResource that works in both production and test builds.
 * Uses reflection to avoid dependencies on test libraries in production.
 */
object IdlingResourceHelper {

    private val idlingResourceClass = try {
        Class.forName("com.talauncher.utils.EspressoIdlingResource")
    } catch (e: ClassNotFoundException) {
        null
    }

    fun increment() {
        try {
            idlingResourceClass?.getMethod("increment")?.invoke(null)
        } catch (e: Exception) {
            // Ignore in production
        }
    }

    fun decrement() {
        try {
            idlingResourceClass?.getMethod("decrement")?.invoke(null)
        } catch (e: Exception) {
            // Ignore in production
        }
    }
}