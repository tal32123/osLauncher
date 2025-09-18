package com.talauncher

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for launcher navigation flows
 * Tests page navigation, back button handling, and home button functionality
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class LauncherNavigationIntegrationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testHorizontalPagerNavigation() {
        composeTestRule.waitForIdle()

        // Wait for onboarding to complete (if needed)
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithText("Settings").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithText("Complete Setup").fetchSemanticsNodes().isNotEmpty()
        }

        // If onboarding is shown, complete it
        if (composeTestRule.onAllNodesWithText("Complete Setup").fetchSemanticsNodes().isNotEmpty()) {
            // Navigate through onboarding steps
            composeTestRule.onNodeWithText("Next").performClick()
            composeTestRule.waitForIdle()

            // Complete onboarding
            composeTestRule.onNodeWithText("Complete Setup").performClick()
            composeTestRule.waitForIdle()
        }

        // Should start on home screen (middle page)
        composeTestRule.onNodeWithContentDescription("Search Google").assertExists()

        // Swipe left to app drawer
        composeTestRule.onRoot().performTouchInput {
            swipeLeft()
        }
        composeTestRule.waitForIdle()

        // Should be on app drawer
        composeTestRule.onNodeWithText("Search apps...").assertExists()

        // Swipe right twice to get to settings
        composeTestRule.onRoot().performTouchInput {
            swipeRight()
        }
        composeTestRule.waitForIdle()

        composeTestRule.onRoot().performTouchInput {
            swipeRight()
        }
        composeTestRule.waitForIdle()

        // Should be on settings screen
        composeTestRule.onNodeWithText("Settings").assertExists()
    }

    @Test
    fun testBackButtonNavigationToHome() {
        composeTestRule.waitForIdle()

        // Wait for main app to load
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithText("Settings").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithText("Complete Setup").fetchSemanticsNodes().isNotEmpty()
        }

        // Complete onboarding if needed
        if (composeTestRule.onAllNodesWithText("Complete Setup").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithText("Complete Setup").performClick()
            composeTestRule.waitForIdle()
        }

        // Navigate to app drawer
        composeTestRule.onRoot().performTouchInput {
            swipeLeft()
        }
        composeTestRule.waitForIdle()

        // Verify we're on app drawer
        composeTestRule.onNodeWithText("Search apps...").assertExists()

        // Press back button (simulated by device back)
        composeTestRule.activity.onBackPressed()
        composeTestRule.waitForIdle()

        // Should return to home screen
        composeTestRule.onNodeWithContentDescription("Search Google").assertExists()
    }

    @Test
    fun testNavigationButtonsInHomeScreen() {
        composeTestRule.waitForIdle()

        // Wait for app to load
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithText("Settings").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithText("Complete Setup").fetchSemanticsNodes().isNotEmpty()
        }

        // Complete onboarding if needed
        if (composeTestRule.onAllNodesWithText("Complete Setup").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithText("Complete Setup").performClick()
            composeTestRule.waitForIdle()
        }

        // Click on "All Apps" button to navigate to app drawer
        composeTestRule.onNodeWithText("All Apps").performClick()
        composeTestRule.waitForIdle()

        // Should be on app drawer
        composeTestRule.onNodeWithText("Search apps...").assertExists()

        // Navigate back to home
        composeTestRule.onRoot().performTouchInput {
            swipeRight()
        }
        composeTestRule.waitForIdle()

        // Click on settings button
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()

        // Should be on settings screen
        composeTestRule.onNodeWithText("Settings").assertExists()
    }

    @Test
    fun testAppLaunchNavigationFlow() {
        composeTestRule.waitForIdle()

        // Wait for app to load and complete onboarding
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithText("Settings").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithText("Complete Setup").fetchSemanticsNodes().isNotEmpty()
        }

        if (composeTestRule.onAllNodesWithText("Complete Setup").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithText("Complete Setup").performClick()
            composeTestRule.waitForIdle()
        }

        // Navigate to app drawer
        composeTestRule.onNodeWithText("All Apps").performClick()
        composeTestRule.waitForIdle()

        // Wait for apps to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("app_item").fetchSemanticsNodes().isNotEmpty()
        }

        // Click on first available app (if any)
        if (composeTestRule.onAllNodesWithTag("app_item").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onAllNodesWithTag("app_item")[0].performClick()
            composeTestRule.waitForIdle()

            // Should navigate back to app drawer after app launch
            composeTestRule.onNodeWithText("Search apps...").assertExists()
        }
    }

    @Test
    fun testOnboardingCompletionFlow() {
        composeTestRule.waitForIdle()

        // If onboarding is present, complete the flow
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            val nodes = composeTestRule.onAllNodesWithText("Complete Setup").fetchSemanticsNodes()
            nodes.isNotEmpty()
        }

        // Navigate through onboarding if present
        if (composeTestRule.onAllNodesWithText("Grant Permissions").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithText("Grant Permissions").performClick()
            composeTestRule.waitForIdle()
        }

        if (composeTestRule.onAllNodesWithText("Next").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithText("Next").performClick()
            composeTestRule.waitForIdle()
        }

        if (composeTestRule.onAllNodesWithText("Complete Setup").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithText("Complete Setup").performClick()
            composeTestRule.waitForIdle()
        }

        // Should reach main launcher interface
        composeTestRule.onNodeWithContentDescription("Search Google").assertExists()
    }

    @Test
    fun testErrorHandlingInNavigation() {
        composeTestRule.waitForIdle()

        // Complete setup if needed
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithText("Settings").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithText("Complete Setup").fetchSemanticsNodes().isNotEmpty()
        }

        if (composeTestRule.onAllNodesWithText("Complete Setup").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithText("Complete Setup").performClick()
            composeTestRule.waitForIdle()
        }

        // Rapid navigation should not crash the app
        repeat(5) {
            composeTestRule.onRoot().performTouchInput { swipeLeft() }
            composeTestRule.waitForIdle()
            composeTestRule.onRoot().performTouchInput { swipeRight() }
            composeTestRule.waitForIdle()
        }

        // App should still be functional
        composeTestRule.onNodeWithContentDescription("Search Google").assertExists()
    }

    @Test
    fun testPersistentStateAcrossNavigation() {
        composeTestRule.waitForIdle()

        // Complete setup
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithText("Settings").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithText("Complete Setup").fetchSemanticsNodes().isNotEmpty()
        }

        if (composeTestRule.onAllNodesWithText("Complete Setup").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithText("Complete Setup").performClick()
            composeTestRule.waitForIdle()
        }

        // Navigate to app drawer and perform search
        composeTestRule.onNodeWithText("All Apps").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Search apps...").performTextInput("test")
        composeTestRule.waitForIdle()

        // Navigate away and back
        composeTestRule.onRoot().performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()

        // Search text should be cleared (depending on implementation)
        composeTestRule.onNodeWithText("Search apps...").assertExists()
    }
}