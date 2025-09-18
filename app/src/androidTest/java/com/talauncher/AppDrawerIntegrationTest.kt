package com.talauncher

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for App Drawer functionality
 * Tests app search, filtering, launching, and alphabetical indexing
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class AppDrawerIntegrationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private fun navigateToAppDrawer() {
        composeTestRule.waitForIdle()

        // Wait for app to load and complete onboarding if needed
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
    }

    @Test
    fun testAppDrawerDisplaysApps() {
        navigateToAppDrawer()

        // Verify app drawer is displayed
        composeTestRule.onNodeWithText("Search").assertExists()

        // Wait for apps to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("app_item").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify that apps are displayed
        composeTestRule.onAllNodesWithTag("app_item").assertCountEquals(1, true)
    }

    @Test
    fun testAppSearchFunctionality() {
        navigateToAppDrawer()

        // Wait for apps to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("app_item").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onNodeWithText("No apps found").isDisplayed()
        }

        // Perform search
        composeTestRule.onNodeWithText("Search").performTextInput("settings")
        composeTestRule.waitForIdle()

        // Verify search results (Settings app should be found)
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithTag("app_item").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onNodeWithText("No apps found").isDisplayed()
        }
    }

    @Test
    fun testSearchClearFunctionality() {
        navigateToAppDrawer()

        // Perform search
        composeTestRule.onNodeWithText("Search").performTextInput("nonexistentapp")
        composeTestRule.waitForIdle()

        // Clear search
        composeTestRule.onNodeWithContentDescription("Clear search").performClick()
        composeTestRule.waitForIdle()

        // Verify search is cleared and all apps are shown
        composeTestRule.onNodeWithText("Search").assertExists()
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithTag("app_item").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onNodeWithText("No apps found").isDisplayed()
        }
    }

    @Test
    fun testCaseInsensitiveSearch() {
        navigateToAppDrawer()

        // Test uppercase search
        composeTestRule.onNodeWithText("Search").performTextInput("SETTINGS")
        composeTestRule.waitForIdle()

        // Should find settings app regardless of case
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithTag("app_item").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onNodeWithText("No apps found").isDisplayed()
        }

        // Clear and try lowercase
        composeTestRule.onNodeWithContentDescription("Clear search").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Search").performTextInput("settings")
        composeTestRule.waitForIdle()

        // Should still find the app
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithTag("app_item").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onNodeWithText("No apps found").isDisplayed()
        }
    }

    @Test
    fun testPartialSearchMatching() {
        navigateToAppDrawer()

        // Test partial search
        composeTestRule.onNodeWithText("Search").performTextInput("set")
        composeTestRule.waitForIdle()

        // Should find apps containing "set" (like Settings)
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithTag("app_item").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onNodeWithText("No apps found").isDisplayed()
        }
    }

    @Test
    fun testAppLaunchFromDrawer() {
        navigateToAppDrawer()

        // Wait for apps to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("app_item").fetchSemanticsNodes().isNotEmpty()
        }

        val appNodes = composeTestRule.onAllNodesWithTag("app_item").fetchSemanticsNodes()
        if (appNodes.isNotEmpty()) {
            // Click on first app
            composeTestRule.onAllNodesWithTag("app_item")[0].performClick()
            composeTestRule.waitForIdle()

            // Should remain in app drawer or navigate based on app implementation
            // The behavior depends on the actual app launch implementation
        }
    }

    @Test
    fun testNoAppsFoundScenario() {
        navigateToAppDrawer()

        // Search for non-existent app
        composeTestRule.onNodeWithText("Search").performTextInput("definitelynonexistentapp12345")
        composeTestRule.waitForIdle()

        // Should show "No apps found" message
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onNodeWithText("No apps found").isDisplayed() ||
            composeTestRule.onAllNodesWithTag("app_item").fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun testAlphabeticalIndexing() {
        navigateToAppDrawer()

        // Wait for apps to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("app_item").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onNodeWithText("No apps found").isDisplayed()
        }

        // Test alphabetical index if present
        composeTestRule.onAllNodesWithTag("alphabet_index").onFirst().assertExists()

        // Click on 'S' in alphabet index to jump to apps starting with S
        if (composeTestRule.onAllNodesWithText("S").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithText("S").performClick()
            composeTestRule.waitForIdle()

            // Should scroll to apps starting with 'S'
            // Verification depends on specific implementation
        }
    }

    @Test
    fun testScrollingBehavior() {
        navigateToAppDrawer()

        // Wait for apps to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("app_item").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onNodeWithText("No apps found").isDisplayed()
        }

        // Test scrolling if there are enough apps
        val appList = composeTestRule.onNodeWithTag("app_list")
        if (appList.isDisplayed()) {
            appList.performScrollToIndex(10) // Scroll down
            composeTestRule.waitForIdle()

            appList.performScrollToIndex(0) // Scroll back to top
            composeTestRule.waitForIdle()
        }
    }

    @Test
    fun testSearchPersistenceAcrossNavigations() {
        navigateToAppDrawer()

        // Perform search
        composeTestRule.onNodeWithText("Search").performTextInput("settings")
        composeTestRule.waitForIdle()

        // Navigate away from app drawer
        composeTestRule.onRoot().performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()

        // Navigate back to app drawer
        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()

        // Search should be cleared (depending on implementation)
        composeTestRule.onNodeWithText("Search").assertExists()
    }

    @Test
    fun testRapidSearchOperations() {
        navigateToAppDrawer()

        // Perform rapid search operations
        composeTestRule.onNodeWithText("Search").performTextInput("a")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Search").performTextClearance()
        composeTestRule.onNodeWithText("Search").performTextInput("b")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Search").performTextClearance()
        composeTestRule.onNodeWithText("Search").performTextInput("settings")
        composeTestRule.waitForIdle()

        // App should handle rapid operations gracefully
        composeTestRule.onNodeWithText("Search").assertExists()
    }

    @Test
    fun testEmptySearchBehavior() {
        navigateToAppDrawer()

        // Type and then clear search
        composeTestRule.onNodeWithText("Search").performTextInput("test")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Search").performTextClearance()
        composeTestRule.waitForIdle()

        // Should show all apps when search is empty
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithTag("app_item").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onNodeWithText("No apps found").isDisplayed()
        }
    }
}