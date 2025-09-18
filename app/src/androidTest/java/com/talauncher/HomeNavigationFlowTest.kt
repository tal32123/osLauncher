package com.talauncher

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Focused tests for home screen navigation patterns and gestures
 * Tests specific navigation behaviors, gesture handling, and home screen interactions
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class HomeNavigationFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var device: UiDevice

    @Before
    fun setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        completeOnboardingIfNeeded()
    }

    /**
     * Test: Home screen swipe navigation consistency
     * Verifies that swipe gestures work consistently in both directions
     */
    @Test
    fun testHomeSwipeNavigationConsistency() {
        // Start on home screen
        ensureOnHomeScreen()

        // Test left swipe to app drawer
        composeTestRule.onRoot().performTouchInput {
            swipeLeft(
                startX = this.width * 0.8f,
                endX = this.width * 0.2f,
                durationMillis = 300
            )
        }
        composeTestRule.waitForIdle()

        // Verify we're on app drawer
        composeTestRule.onNodeWithText("Search apps...").assertExists()

        // Test right swipe back to home
        composeTestRule.onRoot().performTouchInput {
            swipeRight(
                startX = this.width * 0.2f,
                endX = this.width * 0.8f,
                durationMillis = 300
            )
        }
        composeTestRule.waitForIdle()

        // Verify we're back on home
        composeTestRule.onNodeWithContentDescription("Search Google").assertExists()

        // Test right swipe to settings
        composeTestRule.onRoot().performTouchInput {
            swipeRight(
                startX = this.width * 0.2f,
                endX = this.width * 0.8f,
                durationMillis = 300
            )
        }
        composeTestRule.waitForIdle()

        // Verify we're on settings
        composeTestRule.onNodeWithText("Settings").assertExists()

        // Test left swipe back to home
        composeTestRule.onRoot().performTouchInput {
            swipeLeft(
                startX = this.width * 0.8f,
                endX = this.width * 0.2f,
                durationMillis = 300
            )
        }
        composeTestRule.waitForIdle()

        // Verify we're back on home
        composeTestRule.onNodeWithContentDescription("Search Google").assertExists()
    }

    /**
     * Test: Home screen button navigation
     * Tests navigation using tap targets instead of gestures
     */
    @Test
    fun testHomeButtonNavigation() {
        ensureOnHomeScreen()

        // Test "All Apps" button navigation
        composeTestRule.onNodeWithText("All Apps").performClick()
        composeTestRule.waitForIdle()

        // Verify navigation to app drawer
        composeTestRule.onNodeWithText("Search apps...").assertExists()

        // Test return to home via gesture
        composeTestRule.onRoot().performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()

        // Verify we're back on home
        composeTestRule.onNodeWithContentDescription("Search Google").assertExists()

        // Test settings button navigation
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()

        // Verify navigation to settings
        composeTestRule.onNodeWithText("Settings").assertExists()

        // Return to home
        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Search Google").assertExists()
    }

    /**
     * Test: Google search interaction from home
     * Tests the search functionality and external app launching
     */
    @Test
    fun testGoogleSearchInteraction() {
        ensureOnHomeScreen()

        // Test clicking on Google search
        composeTestRule.onNodeWithContentDescription("Search Google").performClick()
        composeTestRule.waitForIdle()

        // Brief pause to allow external app launch
        Thread.sleep(2000)

        // Return to launcher using home button
        device.pressHome()
        composeTestRule.waitForIdle()

        // Verify we're back on home screen
        composeTestRule.onNodeWithContentDescription("Search Google").assertExists()

        // Test that the launcher is still functional
        composeTestRule.onNodeWithText("All Apps").assertExists()
    }

    /**
     * Test: Back button behavior from home screen
     * Tests that back button doesn't exit the launcher inappropriately
     */
    @Test
    fun testBackButtonFromHome() {
        ensureOnHomeScreen()

        // Press back button multiple times
        repeat(5) {
            device.pressBack()
            Thread.sleep(300)
        }

        // Launcher should remain active and functional
        composeTestRule.onNodeWithContentDescription("Search Google").assertExists()
        composeTestRule.onNodeWithText("All Apps").assertExists()
    }

    /**
     * Test: Home button behavior and launcher as default home
     * Tests that pressing home button returns to launcher consistently
     */
    @Test
    fun testHomeButtonBehavior() {
        // Navigate away from home
        composeTestRule.onNodeWithText("All Apps").performClick()
        composeTestRule.waitForIdle()

        // Press home button
        device.pressHome()
        composeTestRule.waitForIdle()

        // Should return to home screen
        composeTestRule.onNodeWithContentDescription("Search Google").assertExists()

        // Navigate to settings
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()

        // Press home button again
        device.pressHome()
        composeTestRule.waitForIdle()

        // Should return to home screen
        composeTestRule.onNodeWithContentDescription("Search Google").assertExists()

        // Test multiple rapid home presses
        repeat(3) {
            device.pressHome()
            Thread.sleep(500)
        }

        // Launcher should remain stable
        composeTestRule.onNodeWithContentDescription("Search Google").assertExists()
    }

    /**
     * Test: Home screen state persistence
     * Tests that home screen maintains its state across navigation
     */
    @Test
    fun testHomeStatePersistence() {
        ensureOnHomeScreen()

        // Navigate away and back multiple times
        repeat(3) {
            // Go to app drawer
            composeTestRule.onNodeWithText("All Apps").performClick()
            composeTestRule.waitForIdle()

            // Return to home
            composeTestRule.onRoot().performTouchInput { swipeRight() }
            composeTestRule.waitForIdle()

            // Verify home elements are still present
            composeTestRule.onNodeWithContentDescription("Search Google").assertExists()
            composeTestRule.onNodeWithText("All Apps").assertExists()

            // Go to settings
            composeTestRule.onNodeWithContentDescription("Settings").performClick()
            composeTestRule.waitForIdle()

            // Return to home
            composeTestRule.onRoot().performTouchInput { swipeLeft() }
            composeTestRule.waitForIdle()

            // Verify home elements are still present
            composeTestRule.onNodeWithContentDescription("Search Google").assertExists()
        }
    }

    /**
     * Test: Rapid navigation stress test
     * Tests that rapid navigation doesn't break the home screen
     */
    @Test
    fun testRapidHomeNavigation() {
        ensureOnHomeScreen()

        // Perform rapid navigation
        repeat(10) {
            // Rapid swipes
            composeTestRule.onRoot().performTouchInput { swipeLeft() }
            composeTestRule.waitForIdle()

            composeTestRule.onRoot().performTouchInput { swipeRight() }
            composeTestRule.waitForIdle()

            // Rapid button presses
            if (it % 2 == 0) {
                device.pressBack()
                Thread.sleep(100)
            }
        }

        // Verify home screen is still functional
        composeTestRule.onNodeWithContentDescription("Search Google").assertExists()
        composeTestRule.onNodeWithText("All Apps").assertExists()

        // Test that navigation still works
        composeTestRule.onNodeWithText("All Apps").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Search apps...").assertExists()

        // Return to home
        composeTestRule.onRoot().performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Search Google").assertExists()
    }

    /**
     * Test: Touch edge cases for home navigation
     * Tests edge swipes, partial swipes, and touch variations
     */
    @Test
    fun testTouchEdgeCases() {
        ensureOnHomeScreen()

        // Test partial swipe (should not trigger navigation)
        composeTestRule.onRoot().performTouchInput {
            swipeLeft(
                startX = this.width * 0.6f,
                endX = this.width * 0.4f,
                durationMillis = 100
            )
        }
        composeTestRule.waitForIdle()

        // Should still be on home screen
        composeTestRule.onNodeWithContentDescription("Search Google").assertExists()

        // Test edge swipe from left edge
        composeTestRule.onRoot().performTouchInput {
            swipeRight(
                startX = 10f,
                endX = this.width * 0.7f,
                durationMillis = 400
            )
        }
        composeTestRule.waitForIdle()

        // Should navigate to settings
        composeTestRule.onNodeWithText("Settings").assertExists()

        // Return to home
        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()

        // Test edge swipe from right edge
        composeTestRule.onRoot().performTouchInput {
            swipeLeft(
                startX = this.width - 10f,
                endX = this.width * 0.3f,
                durationMillis = 400
            )
        }
        composeTestRule.waitForIdle()

        // Should navigate to app drawer
        composeTestRule.onNodeWithText("Search apps...").assertExists()

        // Return to home
        composeTestRule.onRoot().performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Search Google").assertExists()
    }

    /**
     * Test: Home screen accessibility features
     * Tests that navigation works with accessibility features enabled
     */
    @Test
    fun testAccessibilityNavigation() {
        ensureOnHomeScreen()

        // Test that clickable elements have proper accessibility descriptions
        composeTestRule.onNodeWithContentDescription("Search Google").assertExists()
        composeTestRule.onNodeWithContentDescription("Settings").assertExists()

        // Test navigation with accessibility actions
        composeTestRule.onNodeWithText("All Apps").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Search apps...").assertExists()

        // Return to home using gesture
        composeTestRule.onRoot().performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()

        // Verify accessibility of home elements
        composeTestRule.onNodeWithContentDescription("Search Google").assertExists()
    }

    /**
     * Test: Home screen orientation changes
     * Tests that home navigation works correctly after device rotation
     */
    @Test
    fun testOrientationChangeNavigation() {
        ensureOnHomeScreen()

        // Rotate to landscape
        device.setOrientationLeft()
        Thread.sleep(2000)

        // Test navigation in landscape
        composeTestRule.onNodeWithText("All Apps").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Search apps...").assertExists()

        // Return to home
        composeTestRule.onRoot().performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Search Google").assertExists()

        // Rotate back to portrait
        device.setOrientationNatural()
        Thread.sleep(2000)

        // Test navigation in portrait
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Settings").assertExists()

        // Return to home
        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Search Google").assertExists()
    }

    private fun ensureOnHomeScreen() {
        composeTestRule.waitForIdle()

        // Try to navigate to home if not already there
        val maxAttempts = 5
        var attempts = 0

        while (attempts < maxAttempts) {
            val homeNodes = composeTestRule.onAllNodesWithContentDescription("Search Google").fetchSemanticsNodes()
            if (homeNodes.isNotEmpty()) {
                // We're on home screen
                break
            }

            // Try to navigate to home
            device.pressHome()
            composeTestRule.waitForIdle()
            Thread.sleep(500)

            // Or try swiping if we're on adjacent screen
            composeTestRule.onRoot().performTouchInput { swipeRight() }
            composeTestRule.waitForIdle()
            Thread.sleep(500)

            attempts++
        }

        // Verify we're on home screen
        composeTestRule.onNodeWithContentDescription("Search Google").assertExists()
    }

    private fun completeOnboardingIfNeeded() {
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithText("Settings").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithText("Complete Setup").fetchSemanticsNodes().isNotEmpty()
        }

        if (composeTestRule.onAllNodesWithText("Complete Setup").fetchSemanticsNodes().isNotEmpty()) {
            if (composeTestRule.onAllNodesWithText("Next").fetchSemanticsNodes().isNotEmpty()) {
                composeTestRule.onNodeWithText("Next").performClick()
                composeTestRule.waitForIdle()
            }

            composeTestRule.onNodeWithText("Complete Setup").performClick()
            composeTestRule.waitForIdle()
        }
    }
}