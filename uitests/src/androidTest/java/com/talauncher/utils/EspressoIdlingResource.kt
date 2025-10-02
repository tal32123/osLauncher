package com.talauncher.utils

import androidx.test.espresso.IdlingResource
import java.util.concurrent.atomic.AtomicInteger

/**
 * Provides an IdlingResource for Espresso tests to wait for async operations.
 * This implementation must be in androidTest to access Espresso dependencies.
 */
object EspressoIdlingResource {

    private const val RESOURCE_NAME = "GLOBAL"
    private val counter = AtomicInteger(0)

    private val idlingResource: IdlingResource = object : IdlingResource {
        override fun getName(): String = RESOURCE_NAME

        override fun isIdleNow(): Boolean {
            return counter.get() == 0
        }

        private var callback: IdlingResource.ResourceCallback? = null

        override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
            this.callback = callback
        }

        fun transitionToIdle() {
            callback?.onTransitionToIdle()
        }
    }

    fun getIdlingResource(): IdlingResource = idlingResource

    fun increment() {
        counter.incrementAndGet()
    }

    fun decrement() {
        val value = counter.decrementAndGet()
        if (value == 0) {
            (idlingResource as? IdlingResource)?.let { resource ->
                // Notify Espresso when we transition to idle
                (resource as? Any)?.let {
                    it::class.java.getDeclaredMethod("transitionToIdle").invoke(it)
                }
            }
        }
        if (value < 0) {
            throw IllegalStateException("Counter has been corrupted!")
        }
    }
}
