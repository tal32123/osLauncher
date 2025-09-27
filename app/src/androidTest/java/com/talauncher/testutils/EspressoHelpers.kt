package com.talauncher.testutils

import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.idling.CountingIdlingResource
import com.talauncher.utils.EspressoIdlingResource
import org.hamcrest.Matcher
import android.view.View
import androidx.test.espresso.ViewInteraction

/**
 * Helper functions for Espresso testing to reduce boilerplate and improve test readability
 * These utilities make common Espresso operations more concise and maintainable
 */

/**
 * Extension functions for more readable Espresso interactions
 */
fun Int.click(): ViewInteraction = Espresso.onView(ViewMatchers.withId(this)).perform(ViewActions.click())

fun Int.type(text: String): ViewInteraction =
    Espresso.onView(ViewMatchers.withId(this)).perform(ViewActions.typeText(text))

fun Int.clearAndType(text: String): ViewInteraction =
    Espresso.onView(ViewMatchers.withId(this))
        .perform(ViewActions.clearText())
        .perform(ViewActions.typeText(text))

fun Int.assertDisplayed(): ViewInteraction =
    Espresso.onView(ViewMatchers.withId(this)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

fun Int.assertNotDisplayed(): ViewInteraction =
    Espresso.onView(ViewMatchers.withId(this)).check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE)))

fun Int.assertText(text: String): ViewInteraction =
    Espresso.onView(ViewMatchers.withId(this)).check(ViewAssertions.matches(ViewMatchers.withText(text)))

fun Int.assertEnabled(): ViewInteraction =
    Espresso.onView(ViewMatchers.withId(this)).check(ViewAssertions.matches(ViewMatchers.isEnabled()))

fun Int.assertDisabled(): ViewInteraction =
    Espresso.onView(ViewMatchers.withId(this)).check(ViewAssertions.matches(ViewMatchers.isNotEnabled()))

/**
 * Helper for waiting for UI to be idle
 */
fun waitForIdle() {
    Espresso.onIdle()
}

/**
 * Helper class for managing IdlingResources in tests
 */
object IdlingResourceHelper {

    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.getIdlingResource())
    }

    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.getIdlingResource())
    }

    /**
     * Execute a block with idling resource registered and automatically unregister afterwards
     */
    inline fun withIdlingResource(block: () -> Unit) {
        registerIdlingResource()
        try {
            block()
        } finally {
            unregisterIdlingResource()
        }
    }
}

/**
 * Common UI navigation helpers
 */
object NavigationHelpers {

    fun pressBack() {
        Espresso.pressBack()
    }

    fun pressBackAndWait() {
        Espresso.pressBack()
        waitForIdle()
    }

    fun closeKeyboard() {
        Espresso.closeSoftKeyboard()
    }
}

/**
 * Custom matchers for common UI patterns
 */
object CustomMatchers {

    /**
     * Matcher for views that are visible and clickable
     */
    fun isVisibleAndClickable(): Matcher<View> {
        return ViewMatchers.allOf(
            ViewMatchers.isDisplayed(),
            ViewMatchers.isClickable()
        )
    }

    /**
     * Matcher for views with specific content description
     */
    fun withContentDescription(description: String): Matcher<View> {
        return ViewMatchers.withContentDescription(description)
    }
}

/**
 * Test lifecycle helpers for common setup/teardown patterns
 */
object TestLifecycleHelpers {

    /**
     * Setup common test prerequisites
     */
    fun setupTest() {
        IdlingResourceHelper.registerIdlingResource()
        NavigationHelpers.closeKeyboard()
    }

    /**
     * Cleanup after test completion
     */
    fun teardownTest() {
        IdlingResourceHelper.unregisterIdlingResource()
    }

    /**
     * Execute a test with automatic setup and teardown
     */
    inline fun executeTest(block: () -> Unit) {
        setupTest()
        try {
            block()
        } finally {
            teardownTest()
        }
    }
}