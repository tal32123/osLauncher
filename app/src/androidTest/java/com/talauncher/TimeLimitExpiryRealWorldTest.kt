package com.talauncher

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import com.talauncher.service.OverlayService
import org.junit.*
import org.junit.runner.RunWith

/**
 * Real-world scenario tests for time limit expiry popups.
 * Tests the actual user experience when time limits are exceeded
 * and popups appear over currently running apps.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class TimeLimitExpiryRealWorldTest {

    private lateinit var device: UiDevice
    private lateinit var context: Context

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        context = ApplicationProvider.getApplicationContext()

        // Grant necessary permissions
        grantOverlayPermission()
        grantNotificationPermission()

        // Clean state
        device.pressHome()
        Thread.sleep(1000)
    }

    @After
    fun tearDown() {
        hideAllOverlays()
        device.pressHome()
    }

    @Test
    fun testUserScenario_WatchingVideoWhenTimeLimitExpires() {
        // Simulate user watching a video when time limit expires
        launchYouTubeOrVideoApp()
        Thread.sleep(3000)

        // Simulate time limit expiry with countdown
        showCountdownOverlay("YouTube", 10, 60)
        Thread.sleep(2000)

        // Verify user can see they have limited time left
        val countdownVisible = device.findObject(UiSelector().textMatches(".*[0-9]+s remaining.*")).exists() ||
                             device.findObject(UiSelector().textContains("Time's up")).exists()
        Assert.assertTrue("User should see countdown while watching video", countdownVisible)

        // User tries to continue watching (should be blocked)
        attemptVideoInteractions()

        // Verify user cannot interact with video controls
        val overlayStillBlocking = device.findObject(UiSelector().textContains("Time's up")).exists() ||
                                 device.findObject(UiSelector().textMatches(".*[0-9]+s remaining.*")).exists()
        Assert.assertTrue("Video interactions should be blocked by overlay", overlayStillBlocking)

        // Wait for countdown to finish and decision dialog to appear
        Thread.sleep(12000) // Wait for countdown to complete

        // Verify decision dialog appears over video
        val decisionVisible = device.findObject(UiSelector().textContains("Continue with")).exists() ||
                            device.findObject(UiSelector().textContains("math challenge")).exists()
        Assert.assertTrue("Decision dialog should appear over video", decisionVisible)
    }

    @Test
    fun testUserScenario_BrowsingWhenTimeLimitExpires() {
        // User browsing settings/apps when time expires
        launchTestApp("com.android.settings", "Settings")
        Thread.sleep(2000)

        // User is scrolling through settings
        device.swipe(device.displayWidth / 2, device.displayHeight - 200,
                    device.displayWidth / 2, 200, 20)
        Thread.sleep(1000)

        // Time limit expires - show countdown
        showCountdownOverlay("Settings", 5, 30)
        Thread.sleep(1000)

        // User tries to continue browsing (should be blocked)
        attemptBrowsingInteractions()

        // Verify browsing is blocked
        val overlayBlocking = device.findObject(UiSelector().textContains("Time's up")).exists() ||
                            device.findObject(UiSelector().textMatches(".*[0-9]+s remaining.*")).exists()
        Assert.assertTrue("Browsing should be blocked by overlay", overlayBlocking)

        // Wait for countdown to complete
        Thread.sleep(7000)

        // User sees decision dialog and chooses math challenge
        val mathChallengeButton = device.findObject(UiSelector().textMatches(".*[Mm]ath.*[Cc]hallenge.*"))
        if (mathChallengeButton.exists()) {
            mathChallengeButton.click()
            Thread.sleep(2000)

            // Verify math challenge appears over settings
            val mathChallengeVisible = device.findObject(UiSelector().textMatches(".*[0-9]+.*[+\\-×÷].*[0-9]+.*")).exists() ||
                                     device.findObject(UiSelector().textContains("Solve to continue")).exists()
            Assert.assertTrue("Math challenge should appear over settings", mathChallengeVisible)

            // User should still be in settings app context
            val stillInSettings = device.currentPackageName.contains("settings")
            Assert.assertTrue("Should still be in Settings app during math challenge", stillInSettings)
        }
    }

    @Test
    fun testUserScenario_GameWhenTimeLimitExpires() {
        // Launch Calculator as a proxy for an interactive app (game-like)
        launchTestApp("com.android.calculator2", "Calculator")
        Thread.sleep(2000)

        // User is actively using the app
        attemptCalculatorInteractions()
        Thread.sleep(1000)

        // Time expires suddenly with decision dialog (no countdown)
        showDecisionOverlay("Calculator", "com.android.calculator2", true)
        Thread.sleep(2000)

        // User tries to continue playing/using app (should be blocked)
        attemptCalculatorInteractions()

        // Verify game/app interaction is completely blocked
        val decisionStillVisible = device.findObject(UiSelector().textContains("Continue with")).exists() ||
                                 device.findObject(UiSelector().textContains("math challenge")).exists()
        Assert.assertTrue("Decision dialog should block all game interactions", decisionStillVisible)

        // User chooses to solve math challenge
        val mathButton = device.findObject(UiSelector().textMatches(".*[Mm]ath.*[Cc]hallenge.*"))
        if (mathButton.exists()) {
            mathButton.click()
            Thread.sleep(2000)

            // Verify math challenge completely overlays the game
            val mathVisible = device.findObject(UiSelector().textMatches(".*[0-9]+.*[+\\-×÷].*[0-9]+.*")).exists()
            Assert.assertTrue("Math challenge should completely overlay the game", mathVisible)

            // Try to solve the math problem
            attemptMathChallengeSolution()
        }
    }

    @Test
    fun testUserScenario_MultiAppSwitchingBlocked() {
        // User has Settings open
        launchTestApp("com.android.settings", "Settings")
        Thread.sleep(2000)

        // Time limit expires with decision dialog
        showDecisionOverlay("Settings", "com.android.settings", false)
        Thread.sleep(2000)

        // User tries to switch to other apps (should be blocked)
        device.pressRecentApps()
        Thread.sleep(1000)
        device.pressBack()
        Thread.sleep(1000)

        // User tries to go home (may or may not be blocked depending on Android version)
        device.pressHome()
        Thread.sleep(1000)

        // Try to launch another app directly
        try {
            device.executeShellCommand("am start com.android.calculator2")
            Thread.sleep(2000)
        } catch (e: Exception) {
            // Command might fail due to overlay blocking
        }

        // Verify overlay is still controlling the experience
        val overlayControlling = device.findObject(UiSelector().textContains("Continue with")).exists() ||
                               device.findObject(UiSelector().textContains("Set a new timer")).exists() ||
                               device.currentPackageName.contains("settings")
        Assert.assertTrue("Overlay should maintain control over user experience", overlayControlling)
    }

    @Test
    fun testUserScenario_PhysicalButtonsBlocked() {
        launchTestApp("com.android.settings", "Settings")
        Thread.sleep(2000)

        // Show blocking overlay
        showMathChallengeOverlay("Settings", "com.android.settings", "easy")
        Thread.sleep(3000)

        // User frantically tries all physical buttons
        device.pressBack()
        Thread.sleep(500)
        device.pressBack()
        Thread.sleep(500)
        device.pressBack()
        Thread.sleep(500)

        // Verify overlay persists despite back button mashing
        val mathStillVisible = device.findObject(UiSelector().textMatches(".*[0-9]+.*[+\\-×÷].*[0-9]+.*")).exists() ||
                             device.findObject(UiSelector().textContains("Solve to continue")).exists()
        Assert.assertTrue("Math challenge should persist despite back button attempts", mathStillVisible)

        // Try menu button if available
        try {
            device.pressMenu()
            Thread.sleep(1000)
        } catch (e: Exception) {
            // Menu button might not be available on all devices
        }

        // Verify still blocked
        val stillBlocked = device.findObject(UiSelector().textMatches(".*[0-9]+.*[+\\-×÷].*[0-9]+.*")).exists() ||
                         device.findObject(UiSelector().textContains("Solve")).exists()
        Assert.assertTrue("Overlay should block menu button too", stillBlocked)
    }

    @Test
    fun testUserScenario_NotificationInteractionBlocked() {
        launchTestApp("com.android.settings", "Settings")
        Thread.sleep(2000)

        // Show overlay
        showCountdownOverlay("Settings", 15, 30)
        Thread.sleep(2000)

        // User tries to pull down notification shade
        device.swipe(device.displayWidth / 2, 0, device.displayWidth / 2, device.displayHeight / 2, 10)
        Thread.sleep(1000)

        // User tries to access quick settings
        device.swipe(device.displayWidth / 2, 0, device.displayWidth / 2, device.displayHeight / 2, 5)
        Thread.sleep(1000)

        // Verify overlay maintains control
        val overlayMaintainsControl = device.findObject(UiSelector().textContains("Time's up")).exists() ||
                                    device.findObject(UiSelector().textMatches(".*[0-9]+s remaining.*")).exists()
        Assert.assertTrue("Overlay should block notification shade access", overlayMaintainsControl)

        // Verify still in Settings app context
        val stillInSettings = device.currentPackageName.contains("settings")
        Assert.assertTrue("Should still be in Settings app context", stillInSettings)
    }

    @Test
    fun testUserScenario_CompleteFlowWithMathSolution() {
        // Complete realistic user flow from start to finish
        launchTestApp("com.android.calculator2", "Calculator")
        Thread.sleep(2000)

        // 1. Countdown phase
        showCountdownOverlay("Calculator", 3, 10)
        Thread.sleep(1000)

        // User sees countdown and tries to rush calculations
        attemptCalculatorInteractions()
        Thread.sleep(4000) // Wait for countdown to finish

        // 2. Decision phase
        val decisionVisible = device.findObject(UiSelector().textContains("Continue with")).exists()
        if (decisionVisible) {
            // User chooses math challenge
            val mathButton = device.findObject(UiSelector().textMatches(".*[Mm]ath.*[Cc]hallenge.*"))
            if (mathButton.exists()) {
                mathButton.click()
                Thread.sleep(2000)
            }
        }

        // 3. Math challenge phase
        val mathProblem = device.findObject(UiSelector().textMatches(".*[0-9]+.*[+\\-×÷].*[0-9]+.*"))
        if (mathProblem.exists()) {
            // User solves the math problem
            val problemText = mathProblem.text
            val answer = calculateSimpleAnswer(problemText)

            val inputField = device.findObject(UiSelector().className("android.widget.EditText"))
            if (inputField.exists()) {
                inputField.click()
                inputField.clearTextField()
                inputField.setText(answer.toString())

                val submitButton = device.findObject(UiSelector().textContains("Submit"))
                if (submitButton.exists()) {
                    submitButton.click()
                    Thread.sleep(2000)

                    // 4. Verify user can continue or gets prompted for new timer
                    val overlayGone = !device.findObject(UiSelector().textContains("Solve")).exists()
                    val backToCalculator = device.currentPackageName.contains("calculator")

                    Assert.assertTrue("After solving math challenge, user should regain access or get timer prompt",
                        overlayGone || backToCalculator)
                }
            }
        }
    }

    // Helper methods for realistic user interactions
    private fun attemptVideoInteractions() {
        // Simulate user trying to interact with video controls
        val centerX = device.displayWidth / 2
        val centerY = device.displayHeight / 2

        // Try to tap play/pause
        device.click(centerX, centerY)
        Thread.sleep(500)

        // Try to seek
        device.swipe(centerX - 100, centerY + 200, centerX + 100, centerY + 200, 10)
        Thread.sleep(500)

        // Try volume gestures
        device.swipe(device.displayWidth - 50, centerY + 100, device.displayWidth - 50, centerY - 100, 10)
        Thread.sleep(500)
    }

    private fun attemptBrowsingInteractions() {
        // Simulate user trying to browse/scroll
        val centerX = device.displayWidth / 2
        val centerY = device.displayHeight / 2

        // Try scrolling
        device.swipe(centerX, centerY + 100, centerX, centerY - 100, 10)
        Thread.sleep(500)
        device.swipe(centerX, centerY - 100, centerX, centerY + 100, 10)
        Thread.sleep(500)

        // Try tapping items
        device.click(centerX, centerY / 2)
        Thread.sleep(500)
        device.click(centerX, centerY)
        Thread.sleep(500)
    }

    private fun attemptCalculatorInteractions() {
        // Try to use calculator buttons
        val buttons = listOf("1", "2", "3", "+", "4", "5", "6", "=")
        for (button in buttons) {
            try {
                val buttonElement = device.findObject(UiSelector().text(button))
                if (buttonElement.exists()) {
                    buttonElement.click()
                }
            } catch (e: Exception) {
                // Button might not be accessible
            }
            Thread.sleep(200)
        }
    }

    private fun attemptMathChallengeSolution() {
        val mathProblem = device.findObject(UiSelector().textMatches(".*[0-9]+.*[+\\-×÷].*[0-9]+.*"))
        if (mathProblem.exists()) {
            val answer = calculateSimpleAnswer(mathProblem.text)
            val inputField = device.findObject(UiSelector().className("android.widget.EditText"))
            if (inputField.exists()) {
                inputField.click()
                inputField.clearTextField()
                inputField.setText(answer.toString())
                Thread.sleep(1000)
            }
        }
    }

    private fun calculateSimpleAnswer(problemText: String): Int {
        try {
            val cleanText = problemText.replace(Regex("[^0-9+\\-×÷]"), "")
            when {
                cleanText.contains("+") -> {
                    val parts = cleanText.split("+")
                    return parts[0].toInt() + parts[1].toInt()
                }
                cleanText.contains("-") -> {
                    val parts = cleanText.split("-")
                    return parts[0].toInt() - parts[1].toInt()
                }
                cleanText.contains("×") -> {
                    val parts = cleanText.split("×")
                    return parts[0].toInt() * parts[1].toInt()
                }
                cleanText.contains("÷") -> {
                    val parts = cleanText.split("÷")
                    return parts[0].toInt() / parts[1].toInt()
                }
                else -> return 1
            }
        } catch (e: Exception) {
            return 1
        }
    }

    // Standard helper methods
    private fun grantOverlayPermission() {
        try {
            device.executeShellCommand("appops set ${context.packageName} SYSTEM_ALERT_WINDOW allow")
        } catch (e: Exception) {
            // Permission might already be granted
        }
    }

    private fun grantNotificationPermission() {
        try {
            device.executeShellCommand("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS")
        } catch (e: Exception) {
            // Permission might already be granted
        }
    }

    private fun launchYouTubeOrVideoApp() {
        // Try to launch YouTube or fallback to any video-capable app
        val videoApps = listOf(
            "com.google.android.youtube",
            "com.android.gallery3d",
            "com.google.android.videos"
        )

        for (packageName in videoApps) {
            try {
                val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return
                }
            } catch (e: Exception) {
                continue
            }
        }

        // Fallback to Calculator if no video apps available
        launchTestApp("com.android.calculator2", "Calculator")
    }

    private fun launchTestApp(packageName: String, appName: String) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                context.startActivity(intent)
            } else {
                device.executeShellCommand("am start $packageName")
            }
        } catch (e: Exception) {
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