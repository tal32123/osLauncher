package com.talauncher

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.UiDevice
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

/**
 * Stress and performance tests for the launcher
 * Tests app behavior under heavy load, rapid interactions, and performance constraints
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class StressAndPerformanceTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private fun waitForAppToLoad() {
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            composeTestRule.onAllNodesWithText("Settings").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithText("Complete Setup").fetchSemanticsNodes().isNotEmpty()
        }

        // Complete onboarding if needed
        if (composeTestRule.onAllNodesWithText("Complete Setup").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithText("Complete Setup").performClick()
            composeTestRule.waitForIdle()
        }
    }

    @Test
    fun testRapidNavigationStress() {
        waitForAppToLoad()

        val executionTime = measureTimeMillis {
            // Perform rapid navigation between screens
            repeat(50) {
                composeTestRule.onRoot().performTouchInput { swipeLeft() }
                composeTestRule.waitForIdle()

                composeTestRule.onRoot().performTouchInput { swipeRight() }
                composeTestRule.waitForIdle()

                composeTestRule.onRoot().performTouchInput { swipeRight() }
                composeTestRule.waitForIdle()

                composeTestRule.onRoot().performTouchInput { swipeLeft() }
                composeTestRule.waitForIdle()
            }
        }

        // Should complete rapid navigation within reasonable time (under 30 seconds)
        assert(executionTime < 30000) { "Rapid navigation took too long: ${executionTime}ms" }

        // App should still be responsive after stress test
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun testMemoryPressureHandling() {
        waitForAppToLoad()

        // Create memory pressure by rapid app drawer operations
        repeat(100) {
            // Navigate to app drawer
            composeTestRule.onNodeWithText("All Apps").performClick()
            composeTestRule.waitForIdle()

            // Perform search operations
            composeTestRule.onNodeWithText("Search apps...").performTextInput("test$it")
            composeTestRule.waitForIdle()

            // Clear search
            composeTestRule.onNodeWithContentDescription("Clear search").performClick()
            composeTestRule.waitForIdle()

            // Navigate back to home
            composeTestRule.onRoot().performTouchInput { swipeRight() }
            composeTestRule.waitForIdle()

            // Throttle to prevent overwhelming the system
            if (it % 10 == 0) {
                Thread.sleep(100)
            }
        }

        // App should still be functional after memory pressure
        composeTestRule.onNodeWithContentDescription("Search Google").assertExists()
    }

    @Test
    fun testConcurrentUIInteractions() {
        waitForAppToLoad()

        val executionTime = measureTimeMillis {
            // Simulate concurrent UI interactions
            repeat(20) { iteration ->
                // Multiple rapid touches
                composeTestRule.onRoot().performTouchInput {
                    down(center)
                    up()
                }

                // Rapid swipes in different directions
                composeTestRule.onRoot().performTouchInput { swipeLeft() }
                composeTestRule.onRoot().performTouchInput { swipeRight() }
                composeTestRule.onRoot().performTouchInput { swipeUp() }
                composeTestRule.onRoot().performTouchInput { swipeDown() }

                composeTestRule.waitForIdle()
            }
        }

        // Should handle concurrent interactions efficiently
        assert(executionTime < 10000) { "Concurrent interactions took too long: ${executionTime}ms" }

        // UI should remain stable
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun testLongRunningSearchOperations() {
        waitForAppToLoad()

        // Navigate to app drawer
        composeTestRule.onNodeWithText("All Apps").performClick()
        composeTestRule.waitForIdle()

        val searchQueries = listOf(
            "settings", "camera", "calculator", "browser", "maps",
            "nonexistent", "a", "ab", "abc", "abcd", "abcde",
            "1", "12", "123", "@#$", "测试", "🔍"
        )

        val executionTime = measureTimeMillis {
            searchQueries.forEach { query ->
                composeTestRule.onNodeWithText("Search apps...").performTextClearance()
                composeTestRule.onNodeWithText("Search apps...").performTextInput(query)
                composeTestRule.waitForIdle()

                // Wait for search results to stabilize
                Thread.sleep(100)
            }
        }

        // Should handle various search queries efficiently
        assert(executionTime < 15000) { "Search operations took too long: ${executionTime}ms" }

        // Clear final search
        composeTestRule.onNodeWithContentDescription("Clear search").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun testDeviceRotationStress() {
        waitForAppToLoad()

        repeat(10) {
            // Rotate to landscape
            device.setOrientationLeft()
            composeTestRule.waitForIdle()
            Thread.sleep(500)

            // Perform operations in landscape
            composeTestRule.onRoot().performTouchInput { swipeLeft() }
            composeTestRule.waitForIdle()

            // Rotate to portrait
            device.setOrientationNatural()
            composeTestRule.waitForIdle()
            Thread.sleep(500)

            // Perform operations in portrait
            composeTestRule.onRoot().performTouchInput { swipeRight() }
            composeTestRule.waitForIdle()
        }

        // App should handle rotation stress gracefully
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun testBackgroundForegroundCycling() {
        waitForAppToLoad()

        repeat(20) {
            // Send app to background
            device.pressHome()
            Thread.sleep(200)

            // Bring app to foreground
            device.pressRecentApps()
            Thread.sleep(200)
            device.pressHome() // Return to launcher
            Thread.sleep(200)
        }

        // App should handle background/foreground cycling
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun testRapidTextInputStress() {
        waitForAppToLoad()

        // Navigate to app drawer
        composeTestRule.onNodeWithText("All Apps").performClick()
        composeTestRule.waitForIdle()

        val executionTime = measureTimeMillis {
            repeat(100) { index ->
                val text = "rapid$index"
                composeTestRule.onNodeWithText("Search apps...").performTextInput(text)
                composeTestRule.waitForIdle()

                composeTestRule.onNodeWithText("Search apps...").performTextClearance()
                composeTestRule.waitForIdle()
            }
        }

        // Should handle rapid text input efficiently
        assert(executionTime < 20000) { "Rapid text input took too long: ${executionTime}ms" }
    }

    @Test
    fun testLowMemoryScenario() {
        waitForAppToLoad()

        // Simulate low memory by creating many UI operations
        repeat(50) {
            // Navigate through all pages rapidly
            composeTestRule.onRoot().performTouchInput { swipeLeft() }
            composeTestRule.onRoot().performTouchInput { swipeRight() }
            composeTestRule.onRoot().performTouchInput { swipeRight() }
            composeTestRule.onRoot().performTouchInput { swipeLeft() }

            // Perform search operations
            composeTestRule.onNodeWithText("All Apps").performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("Search apps...").performTextInput("test$it")
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithContentDescription("Clear search").performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onRoot().performTouchInput { swipeRight() }
            composeTestRule.waitForIdle()
        }

        // App should survive low memory conditions
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun testUIResponseTime() {
        waitForAppToLoad()

        // Test UI response times for common operations
        val swipeTime = measureTimeMillis {
            composeTestRule.onRoot().performTouchInput { swipeLeft() }
            composeTestRule.waitForIdle()
        }

        val buttonClickTime = measureTimeMillis {
            composeTestRule.onNodeWithText("All Apps").performClick()
            composeTestRule.waitForIdle()
        }

        val searchTime = measureTimeMillis {
            composeTestRule.onNodeWithText("Search apps...").performTextInput("test")
            composeTestRule.waitForIdle()
        }

        // UI operations should be responsive (under 1 second each)
        assert(swipeTime < 1000) { "Swipe took too long: ${swipeTime}ms" }
        assert(buttonClickTime < 1000) { "Button click took too long: ${buttonClickTime}ms" }
        assert(searchTime < 1000) { "Search took too long: ${searchTime}ms" }
    }

    @Test
    fun testContinuousUsageSimulation() {
        waitForAppToLoad()

        // Simulate 5 minutes of continuous usage
        val startTime = System.currentTimeMillis()
        val fiveMinutes = 5 * 60 * 1000L

        var operationCount = 0
        while (System.currentTimeMillis() - startTime < fiveMinutes) {
            when (operationCount % 6) {
                0 -> {
                    composeTestRule.onRoot().performTouchInput { swipeLeft() }
                    composeTestRule.waitForIdle()
                }
                1 -> {
                    composeTestRule.onRoot().performTouchInput { swipeRight() }
                    composeTestRule.waitForIdle()
                }
                2 -> {
                    composeTestRule.onNodeWithText("All Apps").performClick()
                    composeTestRule.waitForIdle()
                }
                3 -> {
                    composeTestRule.onNodeWithText("Search apps...").performTextInput("test")
                    composeTestRule.waitForIdle()
                }
                4 -> {
                    composeTestRule.onNodeWithContentDescription("Clear search").performClick()
                    composeTestRule.waitForIdle()
                }
                5 -> {
                    composeTestRule.onRoot().performTouchInput { swipeRight() }
                    composeTestRule.waitForIdle()
                }
            }
            operationCount++

            // Small pause to prevent overwhelming
            Thread.sleep(50)
        }

        // App should survive continuous usage
        composeTestRule.onRoot().assertExists()

        // Should have performed many operations
        assert(operationCount > 100) { "Should have performed many operations: $operationCount" }
    }
}