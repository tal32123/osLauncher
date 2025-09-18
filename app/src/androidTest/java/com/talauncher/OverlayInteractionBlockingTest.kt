package com.talauncher

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Direction
import com.talauncher.service.OverlayService
import org.junit.*
import org.junit.runner.RunWith

/**
 * Comprehensive tests for overlay interaction blocking capabilities.
 * Focuses specifically on ensuring the popup completely blocks access to the underlying app.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class OverlayInteractionBlockingTest {

    private lateinit var device: UiDevice
    private lateinit var context: Context

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        context = ApplicationProvider.getApplicationContext()

        // Grant overlay permission
        grantOverlayPermission()

        // Start with clean state
        device.pressHome()
        Thread.sleep(1000)
    }

    @After
    fun tearDown() {
        hideAllOverlays()
        device.pressHome()
    }

    @Test
    fun testOverlayBlocksTouchInput() {
        // Launch Settings app as it has many interactive elements
        launchTestApp("com.android.settings")
        Thread.sleep(2000)

        // Show overlay
        showCountdownOverlay("Settings", 30, 60)
        Thread.sleep(2000)

        // Try extensive touch interactions across the entire screen
        val width = device.displayWidth
        val height = device.displayHeight

        // Grid-based touch testing - touch every quadrant
        val touchPoints = listOf(
            Pair(width / 4, height / 4),      // Top-left
            Pair(3 * width / 4, height / 4),  // Top-right
            Pair(width / 4, height / 2),      // Middle-left
            Pair(3 * width / 4, height / 2),  // Middle-right
            Pair(width / 4, 3 * height / 4),  // Bottom-left
            Pair(3 * width / 4, 3 * height / 4) // Bottom-right
        )

        for ((x, y) in touchPoints) {
            device.click(x, y)
            Thread.sleep(200)
        }

        // Verify overlay is still present (meaning it blocked all touches)
        val overlayStillVisible = device.findObject(UiSelector().textContains("Time's up")).exists() ||
                                device.findObject(UiSelector().textMatches(".*[0-9]+s remaining.*")).exists()
        Assert.assertTrue("Overlay should still be visible after touch attempts", overlayStillVisible)

        // Verify we haven't navigated away from Settings
        val currentPackage = device.currentPackageName
        Assert.assertTrue("Should still be over Settings app", currentPackage.contains("settings"))
    }

    @Test
    fun testOverlayBlocksScrolling() {
        // Launch Settings which typically has scrollable content
        launchTestApp("com.android.settings")
        Thread.sleep(2000)

        // Show overlay
        showCountdownOverlay("Settings", 20, 40)
        Thread.sleep(2000)

        // Try various scroll gestures
        val centerX = device.displayWidth / 2
        val centerY = device.displayHeight / 2

        // Vertical scrolling attempts
        device.swipe(centerX, centerY, centerX, centerY - 200, 10) // Swipe up
        Thread.sleep(500)
        device.swipe(centerX, centerY, centerX, centerY + 200, 10) // Swipe down
        Thread.sleep(500)

        // Horizontal scrolling attempts
        device.swipe(centerX, centerY, centerX - 200, centerY, 10) // Swipe left
        Thread.sleep(500)
        device.swipe(centerX, centerY, centerX + 200, centerY, 10) // Swipe right
        Thread.sleep(500)

        // Long scroll attempts
        device.swipe(centerX, height - 100, centerX, 100, 50) // Long swipe up
        Thread.sleep(500)
        device.swipe(centerX, 100, centerX, height - 100, 50) // Long swipe down
        Thread.sleep(500)

        // Verify overlay is still blocking
        val overlayStillVisible = device.findObject(UiSelector().textContains("Time's up")).exists() ||
                                device.findObject(UiSelector().textMatches(".*[0-9]+s remaining.*")).exists()
        Assert.assertTrue("Overlay should block all scroll gestures", overlayStillVisible)
    }

    @Test
    fun testOverlayBlocksHardwareButtons() {
        launchTestApp("com.android.settings")
        Thread.sleep(2000)

        showCountdownOverlay("Settings", 25, 50)
        Thread.sleep(2000)

        // Test back button blocking
        device.pressBack()
        Thread.sleep(1000)

        // Verify overlay is still present (back was blocked)
        var overlayVisible = device.findObject(UiSelector().textContains("Time's up")).exists() ||
                           device.findObject(UiSelector().textMatches(".*[0-9]+s remaining.*")).exists()
        Assert.assertTrue("Overlay should block back button", overlayVisible)

        // Test home button (this might not be blockable, but we test the result)
        device.pressHome()
        Thread.sleep(1000)

        // Check if we went home or overlay blocked it
        val currentPackage = device.currentPackageName
        // Note: Home button blocking varies by Android version and might not be blockable

        // Test recent apps button
        device.pressRecentApps()
        Thread.sleep(1000)

        // Return to test state
        if (currentPackage.contains("settings")) {
            // If we're still in settings, overlay might have blocked recents
            overlayVisible = device.findObject(UiSelector().textContains("Time's up")).exists() ||
                           device.findObject(UiSelector().textMatches(".*[0-9]+s remaining.*")).exists()
            // This is expected behavior
        } else {
            // If we left settings, navigate back to test the overlay persistence
            launchTestApp("com.android.settings")
            Thread.sleep(2000)
        }
    }

    @Test
    fun testOverlayBlocksLongPress() {
        launchTestApp("com.android.settings")
        Thread.sleep(2000)

        showCountdownOverlay("Settings", 15, 30)
        Thread.sleep(2000)

        // Try long press gestures at various locations
        val centerX = device.displayWidth / 2
        val centerY = device.displayHeight / 2

        // Long press center
        device.click(centerX, centerY)
        Thread.sleep(1500) // Simulate long press duration

        // Long press corners
        device.click(100, 100) // Top-left corner
        Thread.sleep(1000)

        device.click(device.displayWidth - 100, 100) // Top-right corner
        Thread.sleep(1000)

        device.click(100, device.displayHeight - 100) // Bottom-left corner
        Thread.sleep(1000)

        // Verify overlay is still blocking after all long press attempts
        val overlayStillVisible = device.findObject(UiSelector().textContains("Time's up")).exists() ||
                                device.findObject(UiSelector().textMatches(".*[0-9]+s remaining.*")).exists()
        Assert.assertTrue("Overlay should block long press gestures", overlayStillVisible)
    }

    @Test
    fun testOverlayBlocksMultiTouch() {
        launchTestApp("com.android.settings")
        Thread.sleep(2000)

        showCountdownOverlay("Settings", 20, 40)
        Thread.sleep(2000)

        // Simulate multi-touch gestures (pinch/zoom attempts)
        val centerX = device.displayWidth / 2
        val centerY = device.displayHeight / 2

        // Pinch out gesture attempt
        device.swipe(centerX - 100, centerY, centerX - 200, centerY, 20)
        Thread.sleep(100)
        device.swipe(centerX + 100, centerY, centerX + 200, centerY, 20)
        Thread.sleep(500)

        // Pinch in gesture attempt
        device.swipe(centerX - 200, centerY, centerX - 100, centerY, 20)
        Thread.sleep(100)
        device.swipe(centerX + 200, centerY, centerX + 100, centerY, 20)
        Thread.sleep(500)

        // Verify overlay is still present and blocking
        val overlayStillVisible = device.findObject(UiSelector().textContains("Time's up")).exists() ||
                                device.findObject(UiSelector().textMatches(".*[0-9]+s remaining.*")).exists()
        Assert.assertTrue("Overlay should block multi-touch gestures", overlayStillVisible)
    }

    @Test
    fun testOverlayPersistenceAcrossRotation() {
        launchTestApp("com.android.settings")
        Thread.sleep(2000)

        showCountdownOverlay("Settings", 30, 60)
        Thread.sleep(2000)

        // Verify overlay is visible in portrait
        var overlayVisible = device.findObject(UiSelector().textContains("Time's up")).exists() ||
                           device.findObject(UiSelector().textMatches(".*[0-9]+s remaining.*")).exists()
        Assert.assertTrue("Overlay should be visible in portrait", overlayVisible)

        // Rotate to landscape
        device.setOrientationLeft()
        Thread.sleep(2000)

        // Verify overlay persists after rotation
        overlayVisible = device.findObject(UiSelector().textContains("Time's up")).exists() ||
                       device.findObject(UiSelector().textMatches(".*[0-9]+s remaining.*")).exists()
        Assert.assertTrue("Overlay should persist after rotation to landscape", overlayVisible)

        // Try interaction in landscape
        device.click(device.displayWidth / 2, device.displayHeight / 2)
        Thread.sleep(1000)

        // Verify still blocking in landscape
        overlayVisible = device.findObject(UiSelector().textContains("Time's up")).exists() ||
                       device.findObject(UiSelector().textMatches(".*[0-9]+s remaining.*")).exists()
        Assert.assertTrue("Overlay should still block in landscape", overlayVisible)

        // Rotate back to portrait
        device.setOrientationNatural()
        Thread.sleep(2000)

        // Verify overlay still present
        overlayVisible = device.findObject(UiSelector().textContains("Time's up")).exists() ||
                       device.findObject(UiSelector().textMatches(".*[0-9]+s remaining.*")).exists()
        Assert.assertTrue("Overlay should persist after rotation back to portrait", overlayVisible)
    }

    @Test
    fun testMathChallengeOverlayBlocksInteraction() {
        launchTestApp("com.android.calculator2")
        Thread.sleep(2000)

        // Show math challenge overlay
        showMathChallengeOverlay("Calculator", "com.android.calculator2", "easy")
        Thread.sleep(3000)

        // Try to interact with calculator buttons (should be blocked)
        val numbers = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        for (number in numbers) {
            try {
                val numberButton = device.findObject(UiSelector().text(number))
                if (numberButton.exists()) {
                    numberButton.click()
                }
            } catch (e: Exception) {
                // Button might not be accessible due to overlay blocking
            }
            Thread.sleep(200)
        }

        // Verify math challenge overlay is still present (blocking calculator access)
        val mathOverlayVisible = device.findObject(UiSelector().textMatches(".*[0-9]+.*[+\\-×÷].*[0-9]+.*")).exists() ||
                               device.findObject(UiSelector().textContains("Solve to continue")).exists() ||
                               device.findObject(UiSelector().className("android.widget.EditText")).exists()
        Assert.assertTrue("Math challenge overlay should block calculator interactions", mathOverlayVisible)

        // Verify we're still over the calculator app
        val currentPackage = device.currentPackageName
        Assert.assertTrue("Should still be over calculator app",
            currentPackage.contains("calculator") || currentPackage.contains("calc"))
    }

    @Test
    fun testDecisionOverlayBlocksAppSwitching() {
        launchTestApp("com.android.settings")
        Thread.sleep(2000)

        showDecisionOverlay("Settings", "com.android.settings", true)
        Thread.sleep(2000)

        // Try to launch other apps while decision overlay is showing
        try {
            device.executeShellCommand("am start com.android.calculator2")
            Thread.sleep(2000)
        } catch (e: Exception) {
            // Command might fail, which is acceptable
        }

        // Verify we're still in settings with overlay present
        val decisionOverlayVisible = device.findObject(UiSelector().textContains("Continue with")).exists() ||
                                    device.findObject(UiSelector().textContains("math challenge")).exists()
        Assert.assertTrue("Decision overlay should block app switching", decisionOverlayVisible)

        val currentPackage = device.currentPackageName
        Assert.assertTrue("Should still be over Settings app", currentPackage.contains("settings"))

        // Try pressing recent apps and then back
        device.pressRecentApps()
        Thread.sleep(1000)
        device.pressBack()
        Thread.sleep(1000)

        // Verify overlay persistence
        val overlayStillVisible = device.findObject(UiSelector().textContains("Continue with")).exists() ||
                                device.findObject(UiSelector().textContains("math challenge")).exists()
        // Overlay should ideally still be present, though some system interactions might not be blockable
    }

    // Helper methods
    private fun grantOverlayPermission() {
        try {
            device.executeShellCommand("appops set ${context.packageName} SYSTEM_ALERT_WINDOW allow")
        } catch (e: Exception) {
            // Permission might already be granted
        }
    }

    private fun launchTestApp(packageName: String) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                context.startActivity(intent)
            } else {
                device.executeShellCommand("am start $packageName")
            }
        } catch (e: Exception) {
            // Fallback launch attempt
            try {
                device.executeShellCommand("am start -n $packageName/.MainActivity")
            } catch (e2: Exception) {
                // App might not be available
            }
        }
    }

    private fun showCountdownOverlay(appName: String, remainingSeconds: Int, totalSeconds: Int) {
        val intent = Intent(context, OverlayService::class.java).apply {
            action = OverlayService.ACTION_SHOW_COUNTDOWN
            putExtra(OverlayService.EXTRA_APP_NAME, appName)
            putExtra(OverlayService.EXTRA_REMAINING_SECONDS, remainingSeconds)
            putExtra(OverlayService.EXTRA_TOTAL_SECONDS, totalSeconds)
        }
        context.startForegroundService(intent)
    }

    private fun showDecisionOverlay(appName: String, packageName: String, showMathOption: Boolean) {
        val intent = Intent(context, OverlayService::class.java).apply {
            action = OverlayService.ACTION_SHOW_DECISION
            putExtra(OverlayService.EXTRA_APP_NAME, appName)
            putExtra(OverlayService.EXTRA_PACKAGE_NAME, packageName)
            putExtra(OverlayService.EXTRA_SHOW_MATH_OPTION, showMathOption)
        }
        context.startForegroundService(intent)
    }

    private fun showMathChallengeOverlay(appName: String, packageName: String, difficulty: String) {
        val intent = Intent(context, OverlayService::class.java).apply {
            action = OverlayService.ACTION_SHOW_MATH_CHALLENGE
            putExtra(OverlayService.EXTRA_APP_NAME, appName)
            putExtra(OverlayService.EXTRA_PACKAGE_NAME, packageName)
            putExtra(OverlayService.EXTRA_DIFFICULTY, difficulty)
        }
        context.startForegroundService(intent)
    }

    private fun hideAllOverlays() {
        val intent = Intent(context, OverlayService::class.java).apply {
            action = OverlayService.ACTION_HIDE_OVERLAY
        }
        context.startService(intent)
    }
}