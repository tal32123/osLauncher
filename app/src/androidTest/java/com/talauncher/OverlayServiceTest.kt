package com.talauncher

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import com.talauncher.service.OverlayService
import org.junit.*
import org.junit.runner.RunWith

/**
 * Direct test of OverlayService functionality
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class OverlayServiceTest {

    private lateinit var device: UiDevice
    private lateinit var context: Context

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        context = ApplicationProvider.getApplicationContext()

        // Grant overlay permission
        try {
            device.executeShellCommand("appops set ${context.packageName} SYSTEM_ALERT_WINDOW allow")
        } catch (e: Exception) {
            // Permission might already be granted
        }
    }

    @Test
    fun testMathChallengeOverlayAppears() {
        // Start the overlay service with math challenge action
        val intent = Intent(context, OverlayService::class.java).apply {
            action = OverlayService.ACTION_SHOW_MATH_CHALLENGE
            putExtra(OverlayService.EXTRA_APP_NAME, "YouTube")
            putExtra(OverlayService.EXTRA_PACKAGE_NAME, "com.google.android.youtube")
            putExtra(OverlayService.EXTRA_DIFFICULTY, "easy")
        }

        context.startForegroundService(intent)
        Thread.sleep(3000) // Wait for overlay to appear

        // Verify overlay is displayed
        val mathChallengeTitle = device.findObject(
            UiSelector().textContains("Time's up")
        )

        val mathProblem = device.findObject(
            UiSelector().textMatches(".*[0-9]+.*[+\\-×÷].*[0-9]+.*")
        )

        val solveText = device.findObject(
            UiSelector().textContains("Solve to continue")
        )

        // At least one of these should be present
        val overlayPresent = mathChallengeTitle.exists() || mathProblem.exists() || solveText.exists()

        Assert.assertTrue("Math challenge overlay should be visible", overlayPresent)

        // Clean up
        hideOverlay()
    }

    @Test
    fun testCountdownOverlayAppears() {
        // Start countdown overlay
        val intent = Intent(context, OverlayService::class.java).apply {
            action = OverlayService.ACTION_SHOW_COUNTDOWN
            putExtra(OverlayService.EXTRA_APP_NAME, "YouTube")
            putExtra(OverlayService.EXTRA_REMAINING_SECONDS, 10)
            putExtra(OverlayService.EXTRA_TOTAL_SECONDS, 30)
        }

        context.startForegroundService(intent)
        Thread.sleep(2000) // Wait for overlay to appear

        // Verify countdown overlay is displayed
        val timesUpText = device.findObject(
            UiSelector().textContains("Time's up")
        )

        val countdownText = device.findObject(
            UiSelector().textMatches(".*[0-9]+s remaining.*")
        )

        val closeText = device.findObject(
            UiSelector().textContains("will close soon")
        )

        val overlayPresent = timesUpText.exists() || countdownText.exists() || closeText.exists()

        Assert.assertTrue("Countdown overlay should be visible", overlayPresent)

        // Clean up
        hideOverlay()
    }

    @Test
    fun testDecisionOverlayAppears() {
        // Start decision overlay
        val intent = Intent(context, OverlayService::class.java).apply {
            action = OverlayService.ACTION_SHOW_DECISION
            putExtra(OverlayService.EXTRA_APP_NAME, "YouTube")
            putExtra(OverlayService.EXTRA_PACKAGE_NAME, "com.google.android.youtube")
            putExtra(OverlayService.EXTRA_SHOW_MATH_OPTION, true)
        }

        context.startForegroundService(intent)
        Thread.sleep(2000) // Wait for overlay to appear

        // Verify decision overlay is displayed
        val continueText = device.findObject(
            UiSelector().textContains("Continue with")
        )

        val mathChallengeButton = device.findObject(
            UiSelector().textContains("math challenge")
        )

        val setTimerButton = device.findObject(
            UiSelector().textContains("Set a new timer")
        )

        val closeButton = device.findObject(
            UiSelector().textContains("Close app")
        )

        val overlayPresent = continueText.exists() || mathChallengeButton.exists() ||
                           setTimerButton.exists() || closeButton.exists()

        Assert.assertTrue("Decision overlay should be visible", overlayPresent)

        // Test math challenge button if present
        if (mathChallengeButton.exists()) {
            mathChallengeButton.click()
            Thread.sleep(2000)

            // Verify math challenge overlay appears after clicking
            val mathProblem = device.findObject(
                UiSelector().textMatches(".*[0-9]+.*[+\\-×÷].*[0-9]+.*")
            )

            Assert.assertTrue("Math challenge should appear after clicking button",
                           mathProblem.exists())
        }

        // Clean up
        hideOverlay()
    }

    private fun hideOverlay() {
        val hideIntent = Intent(context, OverlayService::class.java).apply {
            action = OverlayService.ACTION_HIDE_OVERLAY
        }
        context.startService(hideIntent)
        Thread.sleep(1000)
    }
}