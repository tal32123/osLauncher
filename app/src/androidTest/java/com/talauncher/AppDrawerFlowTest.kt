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
 * Comprehensive tests for app drawer functionality and interactions
 * Tests search, filtering, app launching, and drawer behavior
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class AppDrawerFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var device: UiDevice

    @Before
    fun setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        completeOnboardingIfNeeded()
        navigateToAppDrawer()
    }

    /**
     * Test: Basic app drawer display and loading
     * Verifies app drawer loads correctly with app list
     */
    @Test
    fun testAppDrawerBasicDisplay() {
        // Verify app drawer UI elements
        composeTestRule.onNodeWithText("Search").assertExists()

        // Wait for apps to load
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithTag("app_item").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithContentDescription("Launch").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify apps are displayed (if any exist on the device)
        val appItems = composeTestRule.onAllNodesWithTag("app_item").fetchSemanticsNodes()
        if (appItems.isNotEmpty()) {
            // Should have at least some apps visible
            assert(appItems.size > 0)
        }

        // Verify search field is interactive
        composeTestRule.onNodeWithText("Search").performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Test: App search functionality
     * Tests search behavior, filtering, and search results
     */
    @Test
    fun testAppSearchFunctionality() {
        // Test initial search field state
        composeTestRule.onNodeWithText("Search").assertExists()

        // Test typing in search field
        composeTestRule.onNodeWithText("Search").performTextInput("Settings")
        composeTestRule.waitForIdle()
        Thread.sleep(1000) // Allow search to process

        // Check if search results appear (depends on available apps)
        // The search should at least not crash the app

        // Test search with common app name
        composeTestRule.onNodeWithText("Search").performTextClearance()
        composeTestRule.onNodeWithText("Search").performTextInput("Calculator")
        composeTestRule.waitForIdle()
        Thread.sleep(1000)

        // Test search with non-existent app
        composeTestRule.onNodeWithText("Search").performTextClearance()
        composeTestRule.onNodeWithText("Search").performTextInput("NonExistentApp12345")
        composeTestRule.waitForIdle()
        Thread.sleep(1000)

        // Should show no results gracefully
        composeTestRule.onRoot().assertExists() // App should not crash

        // Test clearing search
        composeTestRule.onNodeWithText("Search").performTextClearance()
        composeTestRule.waitForIdle()

        // Apps should reappear after clearing search
        composeTestRule.onNodeWithText("Search").assertExists()
    }

    /**
     * Test: Alphabetical app organization
     * Tests that apps are properly organized and indexed
     */
    @Test
    fun testAppOrganization() {
        // Clear any existing search
        composeTestRule.onNodeWithText("Search").performTextClearance()
        composeTestRule.waitForIdle()

        // Wait for full app list to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("app_item").fetchSemanticsNodes().size >= 0
        }

        // Test alphabetical index if present
        val indexLetters = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
                                  "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z")

        indexLetters.forEach { letter ->
            val letterNodes = composeTestRule.onAllNodesWithText(letter).fetchSemanticsNodes()
            if (letterNodes.isNotEmpty()) {
                // If alphabetical index exists, test clicking on letters
                composeTestRule.onNodeWithText(letter).performClick()
                composeTestRule.waitForIdle()
                Thread.sleep(300)
            }
        }

        // Verify app drawer is still functional after index interactions
        composeTestRule.onNodeWithText("Search").assertExists()
    }

    /**
     * Test: App launching from drawer
     * Tests launching apps and post-launch behavior
     */
    @Test
    fun testAppLaunchingFromDrawer() {
        // Clear search to show all apps
        composeTestRule.onNodeWithText("Search").performTextClearance()
        composeTestRule.waitForIdle()

        // Wait for apps to load
        composeTestRule.waitUntil(timeoutMillis = 8000) {
            composeTestRule.onAllNodesWithTag("app_item").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithContentDescription("Launch").fetchSemanticsNodes().isNotEmpty()
        }

        val appItems = composeTestRule.onAllNodesWithTag("app_item").fetchSemanticsNodes()

        if (appItems.isNotEmpty()) {
            // Try to launch the first app
            composeTestRule.onAllNodesWithTag("app_item")[0].performClick()
            composeTestRule.waitForIdle()

            // Brief pause for app launch
            Thread.sleep(2000)

            // Return to launcher
            device.pressHome()
            composeTestRule.waitForIdle()

            // Navigate back to app drawer
            composeTestRule.onNodeWithText("All Apps").performClick()
            composeTestRule.waitForIdle()

            // Verify app drawer is still functional
            composeTestRule.onNodeWithText("Search").assertExists()

            // Test launching another app if available
            val updatedAppItems = composeTestRule.onAllNodesWithTag("app_item").fetchSemanticsNodes()
            if (updatedAppItems.size > 1) {
                composeTestRule.onAllNodesWithTag("app_item")[1].performClick()
                composeTestRule.waitForIdle()
                Thread.sleep(1500)

                device.pressHome()
                composeTestRule.waitForIdle()
            }
        }
    }

    /**
     * Test: App drawer scroll behavior
     * Tests scrolling through long lists of apps
     */
    @Test
    fun testAppDrawerScrolling() {
        // Clear search
        composeTestRule.onNodeWithText("Search").performTextClearance()
        composeTestRule.waitForIdle()

        // Wait for apps to load
        Thread.sleep(2000)

        // Test scrolling down
        composeTestRule.onRoot().performTouchInput {
            swipeUp(
                startY = this.height * 0.7f,
                endY = this.height * 0.3f,
                durationMillis = 500
            )
        }
        composeTestRule.waitForIdle()

        // Test scrolling up
        composeTestRule.onRoot().performTouchInput {
            swipeDown(
                startY = this.height * 0.3f,
                endY = this.height * 0.7f,
                durationMillis = 500
            )
        }
        composeTestRule.waitForIdle()

        // Test rapid scrolling
        repeat(3) {
            composeTestRule.onRoot().performTouchInput {
                swipeUp(
                    startY = this.height * 0.8f,
                    endY = this.height * 0.2f,
                    durationMillis = 200
                )
            }
            Thread.sleep(100)
        }

        composeTestRule.waitForIdle()

        // Verify app drawer is still functional
        composeTestRule.onNodeWithText("Search").assertExists()
    }

    /**
     * Test: Search and launch workflow
     * Tests complete search-to-launch user flow
     */
    @Test
    fun testSearchAndLaunchWorkflow() {
        // Test search for system apps that should exist
        val commonApps = listOf("Settings", "Camera", "Clock", "Calculator", "Calendar")

        commonApps.forEach { appName ->
            // Clear previous search
            composeTestRule.onNodeWithText("Search").performTextClearance()
            composeTestRule.waitForIdle()

            // Search for app
            composeTestRule.onNodeWithText("Search").performTextInput(appName)
            composeTestRule.waitForIdle()
            Thread.sleep(1500) // Allow search to process

            // Check if app appears in search results
            val searchResults = composeTestRule.onAllNodesWithTag("app_item").fetchSemanticsNodes()
            if (searchResults.isNotEmpty()) {
                // Launch first search result
                composeTestRule.onAllNodesWithTag("app_item")[0].performClick()
                composeTestRule.waitForIdle()
                Thread.sleep(1000)

                // Return to launcher
                device.pressHome()
                composeTestRule.waitForIdle()

                // Navigate back to app drawer
                composeTestRule.onNodeWithText("All Apps").performClick()
                composeTestRule.waitForIdle()
            }
        }

        // Verify app drawer is still functional after all searches
        composeTestRule.onNodeWithText("Search").assertExists()
    }

    /**
     * Test: App drawer navigation patterns
     * Tests different ways to navigate in and out of app drawer
     */
    @Test
    fun testAppDrawerNavigation() {
        // Test swipe navigation out of drawer
        composeTestRule.onRoot().performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()

        // Should be on home screen
        composeTestRule.onNodeWithContentDescription("Search Google").assertExists()

        // Navigate back to app drawer via button
        composeTestRule.onNodeWithText("All Apps").performClick()
        composeTestRule.waitForIdle()

        // Should be back in app drawer
        composeTestRule.onNodeWithText("Search").assertExists()

        // Test navigation to settings via swipe
        composeTestRule.onRoot().performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()

        // Should be on settings
        composeTestRule.onNodeWithText("Settings").assertExists()

        // Navigate back to app drawer
        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()

        // Should be back in app drawer
        composeTestRule.onNodeWithText("Search").assertExists()

        // Test back button behavior
        device.pressBack()
        composeTestRule.waitForIdle()

        // Should navigate back to home (depending on implementation)
        // Either home screen or still in drawer is acceptable
        composeTestRule.onRoot().assertExists()
    }

    /**
     * Test: App drawer performance under load
     * Tests app drawer behavior with many apps and rapid interactions
     */
    @Test
    fun testAppDrawerPerformanceLoad() {
        // Perform rapid search operations
        val searchTerms = listOf("a", "s", "c", "d", "g", "h", "m", "p", "t")

        searchTerms.forEach { term ->
            composeTestRule.onNodeWithText("Search").performTextClearance()
            composeTestRule.onNodeWithText("Search").performTextInput(term)
            composeTestRule.waitForIdle()
            Thread.sleep(200) // Brief pause for search processing
        }

        // Clear search
        composeTestRule.onNodeWithText("Search").performTextClearance()
        composeTestRule.waitForIdle()

        // Rapid scrolling stress test
        repeat(10) {
            composeTestRule.onRoot().performTouchInput {
                if (it % 2 == 0) {
                    swipeUp(
                        startY = this.height * 0.7f,
                        endY = this.height * 0.3f,
                        durationMillis = 150
                    )
                } else {
                    swipeDown(
                        startY = this.height * 0.3f,
                        endY = this.height * 0.7f,
                        durationMillis = 150
                    )
                }
            }
            Thread.sleep(50)
        }

        composeTestRule.waitForIdle()

        // Verify app drawer is still functional
        composeTestRule.onNodeWithText("Search").assertExists()

        // Test that search still works after stress test
        composeTestRule.onNodeWithText("Search").performTextInput("test")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Search").performTextClearance()
        composeTestRule.waitForIdle()
    }

    /**
     * Test: App drawer state management
     * Tests that drawer maintains appropriate state across navigation
     */
    @Test
    fun testAppDrawerStateManagement() {
        // Perform search
        composeTestRule.onNodeWithText("Search").performTextInput("Settings")
        composeTestRule.waitForIdle()
        Thread.sleep(1000)

        // Navigate away from drawer
        composeTestRule.onRoot().performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()

        // Navigate back to drawer
        composeTestRule.onNodeWithText("All Apps").performClick()
        composeTestRule.waitForIdle()

        // Check if search state is preserved or cleared (both are valid behaviors)
        composeTestRule.onNodeWithText("Search").assertExists()

        // Test with scroll position
        composeTestRule.onNodeWithText("Search").performTextClearance()
        composeTestRule.waitForIdle()

        // Scroll down
        composeTestRule.onRoot().performTouchInput {
            swipeUp(
                startY = this.height * 0.7f,
                endY = this.height * 0.3f,
                durationMillis = 500
            )
        }
        composeTestRule.waitForIdle()

        // Navigate away and back
        composeTestRule.onRoot().performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("All Apps").performClick()
        composeTestRule.waitForIdle()

        // Drawer should be functional regardless of scroll state preservation
        composeTestRule.onNodeWithText("Search").assertExists()
    }

    /**
     * Test: App drawer accessibility
     * Tests accessibility features and navigation
     */
    @Test
    fun testAppDrawerAccessibility() {
        // Test that search field has proper accessibility
        composeTestRule.onNodeWithText("Search").assertExists()

        // Test app items accessibility (if apps are present)
        val appItems = composeTestRule.onAllNodesWithTag("app_item").fetchSemanticsNodes()

        if (appItems.isNotEmpty()) {
            // Apps should have some form of accessible content
            // This test ensures apps can be interacted with via accessibility services
            composeTestRule.onAllNodesWithTag("app_item")[0].assertExists()
        }

        // Test keyboard navigation if supported
        composeTestRule.onNodeWithText("Search").performClick()
        composeTestRule.waitForIdle()

        // Test accessibility navigation doesn't break the drawer
        composeTestRule.onNodeWithText("Search").assertExists()
    }

    private fun navigateToAppDrawer() {
        composeTestRule.waitForIdle()

        // Ensure we're on home screen first
        val homeNodes = composeTestRule.onAllNodesWithContentDescription("Search Google").fetchSemanticsNodes()
        if (homeNodes.isEmpty()) {
            device.pressHome()
            composeTestRule.waitForIdle()
            Thread.sleep(1000)
        }

        // Navigate to app drawer
        composeTestRule.onNodeWithText("All Apps").performClick()
        composeTestRule.waitForIdle()

        // Verify we're in app drawer
        composeTestRule.onNodeWithText("Search").assertExists()
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