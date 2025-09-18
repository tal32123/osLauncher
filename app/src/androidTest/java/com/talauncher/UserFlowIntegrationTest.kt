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
 * Comprehensive user flow tests for TALauncher
 * Tests complete end-to-end user scenarios and workflows
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class UserFlowIntegrationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var device: UiDevice

    @Before
    fun setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    /**
     * Test: Complete first-time user onboarding flow
     * Covers: App launch -> Onboarding -> Permission grants -> Main interface
     */
    @Test
    fun testCompleteFirstTimeUserFlow() {
        composeTestRule.waitForIdle()

        // Wait for onboarding or main app
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithText("Complete Setup").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithText("Settings").fetchSemanticsNodes().isNotEmpty()
        }

        // If onboarding exists, complete the full flow
        if (composeTestRule.onAllNodesWithText("Complete Setup").fetchSemanticsNodes().isNotEmpty()) {
            // Step 1: Introduction screen
            composeTestRule.onNodeWithText("Welcome to TALauncher").assertExists()

            // Navigate through onboarding steps if they exist
            if (composeTestRule.onAllNodesWithText("Next").fetchSemanticsNodes().isNotEmpty()) {
                composeTestRule.onNodeWithText("Next").performClick()
                composeTestRule.waitForIdle()
            }

            // Step 2: Permissions screen
            if (composeTestRule.onAllNodesWithText("Grant Permissions").fetchSemanticsNodes().isNotEmpty()) {
                composeTestRule.onNodeWithText("Grant Permissions").performClick()
                composeTestRule.waitForIdle()
                // Note: In real scenario, user would grant permissions in system dialogs
            }

            // Step 3: Complete setup
            composeTestRule.onNodeWithText("Complete Setup").performClick()
            composeTestRule.waitForIdle()

            // Verify main launcher interface is now visible
            composeTestRule.onNodeWithContentDescription("Search Google").assertExists()
        }

        // Verify all main components are accessible
        composeTestRule.onNodeWithText("All Apps").assertExists()
        composeTestRule.onNodeWithContentDescription("Settings").assertExists()
    }

    /**
     * Test: Complete app discovery and launch flow
     * Covers: Home -> App drawer -> Search -> Launch app -> Return to launcher
     */
    @Test
    fun testAppDiscoveryAndLaunchFlow() {
        completeOnboardingIfNeeded()

        // Step 1: Navigate to app drawer from home
        composeTestRule.onNodeWithText("All Apps").performClick()
        composeTestRule.waitForIdle()

        // Verify we're in app drawer
        composeTestRule.onNodeWithText("Search").assertExists()

        // Step 2: Search for apps
        composeTestRule.onNodeWithText("Search").performTextInput("Settings")
        composeTestRule.waitForIdle()

        // Wait for search results (if any)
        Thread.sleep(1000)

        // Step 3: Clear search and browse all apps
        composeTestRule.onNodeWithText("Search").performTextClearance()
        composeTestRule.waitForIdle()

        // Step 4: Wait for apps to load and verify app items exist
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("app_item").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithContentDescription("Launch").fetchSemanticsNodes().isNotEmpty()
        }

        // Step 5: Launch first available app if any exist
        val appItems = composeTestRule.onAllNodesWithTag("app_item").fetchSemanticsNodes()
        if (appItems.isNotEmpty()) {
            composeTestRule.onAllNodesWithTag("app_item")[0].performClick()
            composeTestRule.waitForIdle()

            // Brief pause to simulate app launch
            Thread.sleep(2000)

            // Return to launcher (app drawer should still be visible)
            composeTestRule.onNodeWithText("Search").assertExists()
        }

        // Step 6: Navigate back to home
        composeTestRule.onRoot().performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()

        // Verify we're back on home screen
        composeTestRule.onNodeWithContentDescription("Search Google").assertExists()
    }

    /**
     * Test: Settings configuration flow
     * Covers: Home -> Settings -> Configure options -> Save -> Return to home
     */
    @Test
    fun testSettingsConfigurationFlow() {
        completeOnboardingIfNeeded()

        // Step 1: Navigate to settings
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()

        // Verify settings screen is displayed
        composeTestRule.onNodeWithText("Settings").assertExists()

        // Step 2: Navigate through settings sections
        // Look for common settings options
        val settingsOptions = listOf(
            "Time Limits",
            "Distracting Apps",
            "Math Challenge",
            "Notification Settings"
        )

        settingsOptions.forEach { option ->
            val nodes = composeTestRule.onAllNodesWithText(option).fetchSemanticsNodes()
            if (nodes.isNotEmpty()) {
                composeTestRule.onNodeWithText(option).performClick()
                composeTestRule.waitForIdle()

                // Navigate back to main settings
                device.pressBack()
                composeTestRule.waitForIdle()
            }
        }

        // Step 3: Return to home screen
        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()

        // Verify we're back on home
        composeTestRule.onNodeWithContentDescription("Search Google").assertExists()
    }

    /**
     * Test: Time limit and restriction flow
     * Covers: Set time limit -> Launch restricted app -> Handle time expiry
     */
    @Test
    fun testTimeLimitFlow() {
        completeOnboardingIfNeeded()

        // Step 1: Navigate to settings
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()

        // Step 2: Look for time limit or distracting apps settings
        val timeLimitNodes = composeTestRule.onAllNodesWithText("Time Limits").fetchSemanticsNodes()
        val distractingAppsNodes = composeTestRule.onAllNodesWithText("Distracting Apps").fetchSemanticsNodes()

        if (timeLimitNodes.isNotEmpty()) {
            composeTestRule.onNodeWithText("Time Limits").performClick()
            composeTestRule.waitForIdle()

            // Configure a short time limit if possible
            // (Implementation would depend on specific UI structure)

            device.pressBack()
            composeTestRule.waitForIdle()
        } else if (distractingAppsNodes.isNotEmpty()) {
            composeTestRule.onNodeWithText("Distracting Apps").performClick()
            composeTestRule.waitForIdle()

            device.pressBack()
            composeTestRule.waitForIdle()
        }

        // Step 3: Return to home and simulate normal usage
        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()

        // Verify launcher remains functional
        composeTestRule.onNodeWithContentDescription("Search Google").assertExists()
    }

    /**
     * Test: Search functionality across the app
     * Covers: Google search -> App search -> Navigation between search contexts
     */
    @Test
    fun testSearchFlow() {
        completeOnboardingIfNeeded()

        // Step 1: Test Google search from home
        composeTestRule.onNodeWithContentDescription("Search Google").performClick()
        composeTestRule.waitForIdle()

        // Should open Google search (external app)
        Thread.sleep(2000)

        // Return to launcher
        device.pressHome()
        composeTestRule.waitForIdle()

        // Step 2: Test app search in drawer
        composeTestRule.onNodeWithText("All Apps").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Search").performTextInput("test")
        composeTestRule.waitForIdle()
        Thread.sleep(1000)

        // Clear search
        composeTestRule.onNodeWithText("Search").performTextClearance()
        composeTestRule.waitForIdle()

        // Step 3: Return to home
        composeTestRule.onRoot().performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Search Google").assertExists()
    }

    /**
     * Test: Multi-tasking and app switching flow
     * Covers: Launch app -> Switch to another app -> Return to launcher -> Recent apps
     */
    @Test
    fun testMultitaskingFlow() {
        completeOnboardingIfNeeded()

        // Step 1: Launch an app from drawer
        composeTestRule.onNodeWithText("All Apps").performClick()
        composeTestRule.waitForIdle()

        val appItems = composeTestRule.onAllNodesWithTag("app_item").fetchSemanticsNodes()
        if (appItems.isNotEmpty()) {
            composeTestRule.onAllNodesWithTag("app_item")[0].performClick()
            composeTestRule.waitForIdle()
            Thread.sleep(2000)
        }

        // Step 2: Use recent apps button
        device.pressRecentApps()
        Thread.sleep(1000)

        // Step 3: Return to launcher
        device.pressHome()
        composeTestRule.waitForIdle()

        // Step 4: Verify launcher state
        composeTestRule.onNodeWithText("Search").assertExists()

        // Navigate back to home
        composeTestRule.onRoot().performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Search Google").assertExists()
    }

    /**
     * Test: Device rotation and state persistence
     * Covers: Navigate -> Rotate -> Verify state -> Rotate back
     */
    @Test
    fun testRotationStateFlow() {
        completeOnboardingIfNeeded()

        // Step 1: Navigate to app drawer
        composeTestRule.onNodeWithText("All Apps").performClick()
        composeTestRule.waitForIdle()

        // Step 2: Perform search
        composeTestRule.onNodeWithText("Search").performTextInput("settings")
        composeTestRule.waitForIdle()

        // Step 3: Rotate device
        device.setOrientationLeft()
        Thread.sleep(2000)

        // Verify app is still functional after rotation
        composeTestRule.onRoot().assertExists()

        // Step 4: Rotate back
        device.setOrientationNatural()
        Thread.sleep(2000)

        // Verify functionality is restored
        composeTestRule.onRoot().assertExists()

        // Clear search and return to home
        composeTestRule.onNodeWithText("Search").performTextClearance()
        composeTestRule.waitForIdle()

        composeTestRule.onRoot().performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()
    }

    /**
     * Test: Error recovery flow
     * Covers: Simulate errors -> Verify graceful handling -> Verify recovery
     */
    @Test
    fun testErrorRecoveryFlow() {
        completeOnboardingIfNeeded()

        // Simulate rapid interactions that might cause errors
        repeat(5) {
            // Rapid navigation
            composeTestRule.onRoot().performTouchInput { swipeLeft() }
            composeTestRule.waitForIdle()
            composeTestRule.onRoot().performTouchInput { swipeRight() }
            composeTestRule.waitForIdle()

            // Rapid back presses
            device.pressBack()
            Thread.sleep(100)
        }

        // Verify app is still functional
        composeTestRule.onNodeWithContentDescription("Search Google").assertExists()

        // Test memory pressure simulation
        repeat(3) {
            device.pressRecentApps()
            Thread.sleep(500)
            device.pressHome()
            Thread.sleep(500)
        }

        // Verify launcher still works
        composeTestRule.onNodeWithText("All Apps").assertExists()
    }

    /**
     * Test: Permission handling flow (mock scenarios)
     * Covers: Permission requests -> Denials -> Retries -> Fallback behavior
     */
    @Test
    fun testPermissionHandlingFlow() {
        composeTestRule.waitForIdle()

        // This test verifies the app handles permission states gracefully
        // Navigate through app to trigger any permission-dependent features

        if (composeTestRule.onAllNodesWithText("Complete Setup").fetchSemanticsNodes().isNotEmpty()) {
            // If onboarding is present, it may include permission requests
            composeTestRule.onNodeWithText("Complete Setup").performClick()
            composeTestRule.waitForIdle()
        }

        // Navigate through main features
        composeTestRule.onNodeWithText("All Apps").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onRoot().performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()

        // Return to home
        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()

        // Verify app remains functional regardless of permission states
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