package com.talauncher.utils

import androidx.test.espresso.IdlingResource
import java.util.concurrent.atomic.AtomicInteger

/**
 * Espresso IdlingResource for tracking async operations in tests.
 * Only compiled in debug builds to avoid test dependencies in production.
 */
object EspressoIdlingResource {

    private const val RESOURCE_NAME = "GLOBAL"

    private val counter = AtomicInteger(0)

    private val idlingResource: IdlingResource = SimpleCountingIdlingResource(RESOURCE_NAME)

    fun getIdlingResource(): IdlingResource = idlingResource

    fun increment() {
        counter.incrementAndGet()
    }

    fun decrement() {
        val value = counter.decrementAndGet()
        if (value == 0) {
            // Notify Espresso that we're idle
        }
        if (value < 0) {
            throw IllegalStateException("Counter has been corrupted!")
        }
    }

    private class SimpleCountingIdlingResource(
        private val resourceName: String
    ) : IdlingResource {

        @Volatile
        private var callback: IdlingResource.ResourceCallback? = null

        override fun getName(): String = resourceName

        override fun isIdleNow(): Boolean {
            val idle = counter.get() == 0
            if (idle && callback != null) {
                callback?.onTransitionToIdle()
            }
            return idle
        }

        override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
            this.callback = callback
        }
    }
}
