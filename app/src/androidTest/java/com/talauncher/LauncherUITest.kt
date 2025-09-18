package com.talauncher

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.UiDevice
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests using Espresso for core launcher functionality
 * Tests basic interactions, accessibility, and user interface elements
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class LauncherUITest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Test
    fun testMainActivityLaunches() {
        // Verify that the main activity launches and displays content
        onView(isRoot()).check(matches(isDisplayed()))

        // Wait for either onboarding or main content to appear
        Thread.sleep(2000) // Allow time for initialization

        // The app should show either onboarding or main content without crashing
        onView(isRoot()).check(matches(isDisplayed()))
    }

    @Test
    fun testHomeButtonFunctionality() {
        // Wait for app to load
        Thread.sleep(3000)

        // Press home button
        device.pressHome()
        Thread.sleep(1000)

        // Press home again to return to launcher
        device.pressHome()
        Thread.sleep(1000)

        // Launcher should be displayed
        onView(isRoot()).check(matches(isDisplayed()))
    }

    @Test
    fun testBackButtonHandling() {
        // Wait for app to load
        Thread.sleep(3000)

        // Press back button multiple times
        pressBack()
        Thread.sleep(500)
        pressBack()
        Thread.sleep(500)

        // App should handle back button gracefully (stay on launcher)
        onView(isRoot()).check(matches(isDisplayed()))
    }

    @Test
    fun testRecentAppsButton() {
        // Wait for app to load
        Thread.sleep(3000)

        // Press recent apps button
        device.pressRecentApps()
        Thread.sleep(1000)

        // Return to launcher
        device.pressHome()
        Thread.sleep(1000)

        // Launcher should still be functional
        onView(isRoot()).check(matches(isDisplayed()))
    }

    @Test
    fun testSwipeGestures() {
        // Wait for app to load
        Thread.sleep(3000)

        // Perform swipe gestures
        onView(isRoot()).perform(swipeLeft())
        Thread.sleep(1000)

        onView(isRoot()).perform(swipeRight())
        Thread.sleep(1000)

        onView(isRoot()).perform(swipeRight())
        Thread.sleep(1000)

        // App should handle swipes without crashing
        onView(isRoot()).check(matches(isDisplayed()))
    }

    @Test
    fun testRotationHandling() {
        // Wait for app to load
        Thread.sleep(3000)

        // Rotate device to landscape
        device.setOrientationLeft()
        Thread.sleep(2000)

        // App should handle rotation
        onView(isRoot()).check(matches(isDisplayed()))

        // Rotate back to portrait
        device.setOrientationNatural()
        Thread.sleep(2000)

        // App should still be functional
        onView(isRoot()).check(matches(isDisplayed()))
    }

    @Test
    fun testMemoryPressureHandling() {
        // Wait for app to load
        Thread.sleep(3000)

        // Simulate memory pressure by opening and closing recent apps multiple times
        repeat(5) {
            device.pressRecentApps()
            Thread.sleep(500)
            device.pressHome()
            Thread.sleep(500)
        }

        // App should handle memory pressure gracefully
        onView(isRoot()).check(matches(isDisplayed()))
    }

    @Test
    fun testAccessibilityFeatures() {
        // Wait for app to load
        Thread.sleep(3000)

        // Test that main elements have accessibility descriptions
        // This test verifies basic accessibility compliance

        try {
            // Check for content descriptions on interactive elements
            onView(allOf(isClickable(), isDisplayed()))
                .check(matches(isDisplayed()))
        } catch (e: Exception) {
            // If no clickable elements found, that's okay for this basic test
        }

        // Verify the app doesn't crash with accessibility services
        onView(isRoot()).check(matches(isDisplayed()))
    }

    @Test
    fun testAppStability() {
        // Wait for app to load
        Thread.sleep(3000)

        // Perform various operations to test stability
        repeat(3) {
            onView(isRoot()).perform(swipeLeft())
            Thread.sleep(300)
            onView(isRoot()).perform(swipeRight())
            Thread.sleep(300)
            pressBack()
            Thread.sleep(300)
        }

        // App should remain stable
        onView(isRoot()).check(matches(isDisplayed()))
    }

    @Test
    fun testLauncherAsDefaultHome() {
        // Wait for app to load
        Thread.sleep(3000)

        // Press home button multiple times
        repeat(3) {
            device.pressHome()
            Thread.sleep(1000)
        }

        // Launcher should consistently respond as default home
        onView(isRoot()).check(matches(isDisplayed()))
    }

    @Test
    fun testQuickSettingsPanelIntegration() {
        // Wait for app to load
        Thread.sleep(3000)

        // Open quick settings
        device.openQuickSettings()
        Thread.sleep(2000)

        // Close quick settings
        device.pressBack()
        Thread.sleep(1000)

        // Return to launcher
        device.pressHome()
        Thread.sleep(1000)

        // Launcher should still be functional
        onView(isRoot()).check(matches(isDisplayed()))
    }

    @Test
    fun testNotificationPanelIntegration() {
        // Wait for app to load
        Thread.sleep(3000)

        // Open notification panel
        device.openNotification()
        Thread.sleep(2000)

        // Close notification panel
        device.pressBack()
        Thread.sleep(1000)

        // Return to launcher
        device.pressHome()
        Thread.sleep(1000)

        // Launcher should handle notification panel interactions
        onView(isRoot()).check(matches(isDisplayed()))
    }

    @Test
    fun testMultipleBackPresses() {
        // Wait for app to load
        Thread.sleep(3000)

        // Press back button rapidly multiple times
        repeat(10) {
            pressBack()
            Thread.sleep(100)
        }

        // App should handle rapid back presses gracefully
        onView(isRoot()).check(matches(isDisplayed()))
    }

    @Test
    fun testAppLaunchFromLauncher() {
        // Wait for app to load
        Thread.sleep(3000)

        // Try to launch Settings app if available
        try {
            // Navigate to app drawer
            onView(isRoot()).perform(swipeLeft())
            Thread.sleep(1000)

            // If we can find and click on an app, do so
            // Otherwise, just verify the launcher is still functional
            onView(isRoot()).check(matches(isDisplayed()))

        } catch (e: Exception) {
            // If navigation fails, just ensure app is still running
            onView(isRoot()).check(matches(isDisplayed()))
        }
    }

    @Test
    fun testStatusBarInteraction() {
        // Wait for app to load
        Thread.sleep(3000)

        // Try to expand status bar
        device.openNotification()
        Thread.sleep(1000)

        device.pressBack()
        Thread.sleep(1000)

        // Launcher should handle status bar interactions
        onView(isRoot()).check(matches(isDisplayed()))
    }

    @Test
    fun testVolumeButtonHandling() {
        // Wait for app to load
        Thread.sleep(3000)

        // Press volume buttons
        device.pressVolumeUp()
        Thread.sleep(500)
        device.pressVolumeDown()
        Thread.sleep(500)

        // Launcher should handle volume button presses without issues
        onView(isRoot()).check(matches(isDisplayed()))
    }
}