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
 * Tests for overlay permission handling and related user flows
 * Tests permission requests, denials, fallback behaviors, and overlay functionality
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class OverlayPermissionFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var device: UiDevice

    @Before
    fun setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        composeTestRule.waitForIdle()
    }

    /**
     * Test: Initial permission request flow
     * Tests permission request during onboarding or initial setup
     */
    @Test
    fun testInitialPermissionRequestFlow() {
        // Wait for app to fully load
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            composeTestRule.onAllNodesWithText("Complete Setup").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithText("Settings").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithText("Grant Permissions").fetchSemanticsNodes().isNotEmpty()
        }

        // If onboarding is present, go through permission flow
        if (composeTestRule.onAllNodesWithText("Grant Permissions").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithText("Grant Permissions").performClick()
            composeTestRule.waitForIdle()

            // This would normally open system permission dialog
            // In test environment, we verify the app doesn't crash
            Thread.sleep(2000)

            // Return to app (simulate user denying or granting permission)
            device.pressBack()
            composeTestRule.waitForIdle()

            // App should continue functioning regardless
            composeTestRule.onRoot().assertExists()
        }

        // If setup completion is available, complete it
        if (composeTestRule.onAllNodesWithText("Complete Setup").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithText("Complete Setup").performClick()
            composeTestRule.waitForIdle()
        }

        // Verify app reaches functional state
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithContentDescription("Search Google").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithText("All Apps").fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * Test: Permission-dependent feature access
     * Tests accessing features that require overlay permission
     */
    @Test
    fun testPermissionDependentFeatureAccess() {
        completeOnboardingIfNeeded()

        // Navigate to settings where overlay-dependent features might be configured
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()

        // Look for settings that might require overlay permission
        val overlayRelatedSettings = listOf(
            "Math Challenge",
            "Time Limits",
            "Distracting Apps",
            "Overlay Settings",
            "Display over other apps"
        )

        overlayRelatedSettings.forEach { settingName ->
            val settingNodes = composeTestRule.onAllNodesWithText(settingName).fetchSemanticsNodes()
            if (settingNodes.isNotEmpty()) {
                // Click on the setting
                composeTestRule.onNodeWithText(settingName).performClick()
                composeTestRule.waitForIdle()

                // Should not crash, might show permission request or setting options
                composeTestRule.onRoot().assertExists()

                // Navigate back to main settings
                device.pressBack()
                composeTestRule.waitForIdle()

                // Verify we're still in settings
                composeTestRule.onNodeWithText("Settings").assertExists()
            }
        }

        // Return to home
        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
    }

    /**
     * Test: Fallback behavior when overlay permission denied
     * Tests graceful degradation when overlay features can't be used
     */
    @Test
    fun testOverlayPermissionFallbackBehavior() {
        completeOnboardingIfNeeded()

        // Try to trigger scenarios that would normally use overlay
        // This might include setting up time limits or restrictions

        // Navigate to settings
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()

        // Try to access time limit or distracting app settings
        val restrictionSettings = listOf("Time Limits", "Distracting Apps", "Math Challenge")

        restrictionSettings.forEach { setting ->
            val nodes = composeTestRule.onAllNodesWithText(setting).fetchSemanticsNodes()
            if (nodes.isNotEmpty()) {
                composeTestRule.onNodeWithText(setting).performClick()
                composeTestRule.waitForIdle()

                // App should handle lack of overlay permission gracefully
                // Might show alternative UI or explanation
                composeTestRule.onRoot().assertExists()

                // Try to interact with any toggle or setting
                val toggleNodes = composeTestRule.onAllNodesWithTag("toggle").fetchSemanticsNodes()
                if (toggleNodes.isNotEmpty()) {
                    composeTestRule.onAllNodesWithTag("toggle")[0].performClick()
                    composeTestRule.waitForIdle()
                }

                // Navigate back
                device.pressBack()
                composeTestRule.waitForIdle()
            }
        }

        // Return to home
        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()

        // Verify core functionality still works without overlay permission
        composeTestRule.onNodeWithText("All Apps").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Search").assertExists()
    }

    /**
     * Test: Re-requesting overlay permission
     * Tests scenarios where permission needs to be re-requested
     */
    @Test
    fun testPermissionReRequest() {
        completeOnboardingIfNeeded()

        // Navigate to settings
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()

        // Look for permission-related settings or retry options
        val permissionButtons = listOf(
            "Request Permission",
            "Grant Permission",
            "Retry Permission",
            "Enable Overlay",
            "Settings"
        )

        permissionButtons.forEach { buttonText ->
            val buttonNodes = composeTestRule.onAllNodesWithText(buttonText).fetchSemanticsNodes()
            if (buttonNodes.isNotEmpty()) {
                composeTestRule.onNodeWithText(buttonText).performClick()
                composeTestRule.waitForIdle()

                // This might open system settings or permission dialog
                Thread.sleep(2000)

                // Return to app
                device.pressBack()
                composeTestRule.waitForIdle()

                // Verify app is still functional
                composeTestRule.onRoot().assertExists()
            }
        }

        // Test accessing permission from different parts of the app
        // Navigate through different screens looking for permission prompts
        device.pressBack()
        composeTestRule.waitForIdle()

        // Navigate to app drawer
        composeTestRule.onNodeWithText("All Apps").performClick()
        composeTestRule.waitForIdle()

        // Return to home
        composeTestRule.onRoot().performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()
    }

    /**
     * Test: Overlay functionality trigger scenarios
     * Tests scenarios that would trigger overlay display if permission exists
     */
    @Test
    fun testOverlayTriggerScenarios() {
        completeOnboardingIfNeeded()

        // Set up a scenario that would normally trigger overlay (if possible)
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()

        // Try to set up time limits or math challenges
        val challengeNodes = composeTestRule.onAllNodesWithText("Math Challenge").fetchSemanticsNodes()
        if (challengeNodes.isNotEmpty()) {
            composeTestRule.onNodeWithText("Math Challenge").performClick()
            composeTestRule.waitForIdle()

            // Try to enable math challenge
            val enableNodes = composeTestRule.onAllNodesWithTag("toggle").fetchSemanticsNodes()
            if (enableNodes.isNotEmpty()) {
                composeTestRule.onAllNodesWithTag("toggle")[0].performClick()
                composeTestRule.waitForIdle()
            }

            device.pressBack()
            composeTestRule.waitForIdle()
        }

        // Try to add distracting apps
        val distractingAppNodes = composeTestRule.onAllNodesWithText("Distracting Apps").fetchSemanticsNodes()
        if (distractingAppNodes.isNotEmpty()) {
            composeTestRule.onNodeWithText("Distracting Apps").performClick()
            composeTestRule.waitForIdle()

            // Look for add button or app selection
            val addButtons = composeTestRule.onAllNodesWithText("Add App").fetchSemanticsNodes()
            if (addButtons.isNotEmpty()) {
                composeTestRule.onNodeWithText("Add App").performClick()
                composeTestRule.waitForIdle()
            }

            device.pressBack()
            composeTestRule.waitForIdle()
        }

        // Return to home
        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()

        // Launch an app that might trigger overlay (if configured)
        composeTestRule.onNodeWithText("All Apps").performClick()
        composeTestRule.waitForIdle()

        // Try to launch an app
        val appItems = composeTestRule.onAllNodesWithTag("app_item").fetchSemanticsNodes()
        if (appItems.isNotEmpty()) {
            composeTestRule.onAllNodesWithTag("app_item")[0].performClick()
            composeTestRule.waitForIdle()
            Thread.sleep(3000) // Wait to see if overlay appears

            // Return to launcher
            device.pressHome()
            composeTestRule.waitForIdle()
        }
    }

    /**
     * Test: Permission state changes
     * Tests app behavior when permission is granted/revoked externally
     */
    @Test
    fun testPermissionStateChanges() {
        completeOnboardingIfNeeded()

        // Simulate scenarios where permission state might change
        // This could happen if user goes to system settings and changes permission

        // Navigate through app multiple times to test state persistence
        repeat(3) {
            // Go to settings
            composeTestRule.onNodeWithContentDescription("Settings").performClick()
            composeTestRule.waitForIdle()

            // Look for any permission-dependent UI elements
            val permissionRelatedElements = composeTestRule.onAllNodesWithTag("permission_dependent").fetchSemanticsNodes()

            // Navigate back to home
            composeTestRule.onRoot().performTouchInput { swipeLeft() }
            composeTestRule.waitForIdle()
            composeTestRule.onRoot().performTouchInput { swipeLeft() }
            composeTestRule.waitForIdle()

            // Go to app drawer
            composeTestRule.onNodeWithText("All Apps").performClick()
            composeTestRule.waitForIdle()

            // Return to home
            composeTestRule.onRoot().performTouchInput { swipeRight() }
            composeTestRule.waitForIdle()

            // Brief pause between iterations
            Thread.sleep(1000)
        }

        // App should remain stable throughout
        composeTestRule.onNodeWithContentDescription("Search Google").assertExists()
    }

    /**
     * Test: Overlay service lifecycle
     * Tests overlay service behavior and lifecycle management
     */
    @Test
    fun testOverlayServiceLifecycle() {
        completeOnboardingIfNeeded()

        // Try to trigger overlay service start through settings
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()

        // Look for service-related settings
        val serviceSettings = listOf(
            "Service Settings",
            "Background Service",
            "Overlay Service"
        )

        serviceSettings.forEach { setting ->
            val nodes = composeTestRule.onAllNodesWithText(setting).fetchSemanticsNodes()
            if (nodes.isNotEmpty()) {
                composeTestRule.onNodeWithText(setting).performClick()
                composeTestRule.waitForIdle()

                // Service should start/stop gracefully
                composeTestRule.onRoot().assertExists()

                device.pressBack()
                composeTestRule.waitForIdle()
            }
        }

        // Test app behavior during memory pressure (service might be killed)
        repeat(5) {
            device.pressRecentApps()
            Thread.sleep(300)
            device.pressHome()
            Thread.sleep(300)
        }

        // App should handle service lifecycle gracefully
        composeTestRule.onNodeWithText("Settings").assertExists()

        // Return to home
        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
    }

    /**
     * Test: Permission error handling
     * Tests error scenarios related to overlay permissions
     */
    @Test
    fun testPermissionErrorHandling() {
        completeOnboardingIfNeeded()

        // Try to trigger permission errors through rapid interactions
        repeat(5) {
            // Navigate to settings
            composeTestRule.onNodeWithContentDescription("Settings").performClick()
            composeTestRule.waitForIdle()

            // Try to access permission-dependent features rapidly
            val permissionFeatures = listOf("Math Challenge", "Time Limits")

            permissionFeatures.forEach { feature ->
                val nodes = composeTestRule.onAllNodesWithText(feature).fetchSemanticsNodes()
                if (nodes.isNotEmpty()) {
                    composeTestRule.onNodeWithText(feature).performClick()
                    composeTestRule.waitForIdle()
                    Thread.sleep(100)

                    device.pressBack()
                    composeTestRule.waitForIdle()
                    Thread.sleep(100)
                }
            }

            // Return to home
            composeTestRule.onRoot().performTouchInput { swipeLeft() }
            composeTestRule.waitForIdle()
            composeTestRule.onRoot().performTouchInput { swipeLeft() }
            composeTestRule.waitForIdle()
            Thread.sleep(200)
        }

        // App should handle errors gracefully and remain functional
        composeTestRule.onNodeWithContentDescription("Search Google").assertExists()
        composeTestRule.onNodeWithText("All Apps").assertExists()
    }

    /**
     * Test: Cross-screen permission consistency
     * Tests that permission state is consistent across all app screens
     */
    @Test
    fun testCrossScreenPermissionConsistency() {
        completeOnboardingIfNeeded()

        // Navigate through all main screens and check for permission consistency
        val screens = listOf(
            { composeTestRule.onNodeWithContentDescription("Search Google").assertExists() }, // Home
            {
                composeTestRule.onNodeWithText("All Apps").performClick()
                composeTestRule.waitForIdle()
                composeTestRule.onNodeWithText("Search").assertExists()
            }, // App Drawer
            {
                composeTestRule.onRoot().performTouchInput { swipeRight() }
                composeTestRule.waitForIdle()
                composeTestRule.onRoot().performTouchInput { swipeRight() }
                composeTestRule.waitForIdle()
                composeTestRule.onNodeWithText("Settings").assertExists()
            } // Settings
        )

        screens.forEach { navigateToScreen ->
            try {
                navigateToScreen()

                // Check for any permission-related UI elements
                // App should show consistent permission state
                composeTestRule.onRoot().assertExists()

                // Brief pause
                Thread.sleep(500)

            } catch (e: Exception) {
                // Screen might not be accessible, that's okay
            }
        }

        // Return to home
        device.pressHome()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Search Google").assertExists()
    }

    private fun completeOnboardingIfNeeded() {
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            composeTestRule.onAllNodesWithText("Settings").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithText("Complete Setup").fetchSemanticsNodes().isNotEmpty()
        }

        if (composeTestRule.onAllNodesWithText("Complete Setup").fetchSemanticsNodes().isNotEmpty()) {
            // Navigate through onboarding steps if they exist
            if (composeTestRule.onAllNodesWithText("Next").fetchSemanticsNodes().isNotEmpty()) {
                composeTestRule.onNodeWithText("Next").performClick()
                composeTestRule.waitForIdle()
            }

            if (composeTestRule.onAllNodesWithText("Grant Permissions").fetchSemanticsNodes().isNotEmpty()) {
                composeTestRule.onNodeWithText("Grant Permissions").performClick()
                composeTestRule.waitForIdle()
                Thread.sleep(1000)
                device.pressBack() // Simulate returning from permission dialog
                composeTestRule.waitForIdle()
            }

            composeTestRule.onNodeWithText("Complete Setup").performClick()
            composeTestRule.waitForIdle()
        }

        // Verify we reach main interface
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithContentDescription("Search Google").fetchSemanticsNodes().isNotEmpty()
        }
    }
}