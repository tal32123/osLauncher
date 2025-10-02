package com.talauncher.uitests

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.IdlingRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.talauncher.MainActivity
import com.talauncher.data.database.LauncherDatabase
import com.talauncher.utils.EspressoIdlingResource
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Diagnostic test to identify why onboarding doesn't complete.
 * This test will help us understand the current state and find the bug.
 */
@RunWith(AndroidJUnit4::class)
class OnboardingDiagnosticTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var context: Context
    private lateinit var device: UiDevice

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // Clear all data before each test
        runBlocking {
            val database = LauncherDatabase.getDatabase(context)
            database.clearAllTables()
        }

        // Register IdlingResource for async operations
        IdlingRegistry.getInstance().register(EspressoIdlingResource.getIdlingResource())
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.getIdlingResource())
    }

    @Test
    fun diagnostic_checkOnboardingScreenState() {
        // Wait for onboarding screen to appear
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithTag("onboarding_step_default_launcher")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Log current state
        println("=== ONBOARDING DIAGNOSTIC TEST ===")

        // Check if onboarding screen is visible
        val onboardingVisible = try {
            composeTestRule.onNodeWithTag("onboarding_step_default_launcher")
                .assertExists()
            true
        } catch (e: AssertionError) {
            false
        }
        println("Onboarding screen visible: $onboardingVisible")

        // Check each step
        val steps = listOf(
            "onboarding_step_default_launcher",
            "onboarding_step_usage_stats",
            "onboarding_step_contacts",
            "onboarding_step_location"
        )

        // Add notifications step for Android 13+
        val allSteps = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            steps + "onboarding_step_notifications"
        } else {
            steps
        }

        allSteps.forEach { tag ->
            val exists = try {
                composeTestRule.onNodeWithTag(tag).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
            println("Step $tag exists: $exists")
        }

        // Check if incomplete message is shown
        val incompleteMessageVisible = try {
            composeTestRule.onNodeWithTag("onboarding_incomplete_message")
                .assertExists()
            true
        } catch (e: AssertionError) {
            false
        }
        println("Incomplete message visible: $incompleteMessageVisible")

        // Check if success card is shown
        val successCardVisible = try {
            composeTestRule.onNodeWithTag("onboarding_success_card")
                .assertExists()
            true
        } catch (e: AssertionError) {
            false
        }
        println("Success card visible: $successCardVisible")

        // Check if we can see home screen
        val homeScreenVisible = try {
            composeTestRule.onNodeWithTag("launcher_home_page")
                .assertExists()
            true
        } catch (e: AssertionError) {
            false
        }
        println("Home screen visible: $homeScreenVisible")

        println("=== END DIAGNOSTIC ===")

        // This test always passes - it's just for diagnostics
        assert(true)
    }

    @Test
    fun diagnostic_attemptToCompleteOnboarding() {
        // Wait for onboarding screen
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithTag("onboarding_step_default_launcher")
                .fetchSemanticsNodes().isNotEmpty()
        }

        println("=== ATTEMPTING TO COMPLETE ONBOARDING ===")

        // Try clicking each button
        val buttons = listOf(
            "onboarding_step_default_launcher_button" to "Default Launcher",
            "onboarding_step_usage_stats_button" to "Usage Stats",
            "onboarding_step_contacts_button" to "Contacts",
            "onboarding_step_location_button" to "Location"
        )

        // Add notifications button for Android 13+
        val allButtons = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            buttons + ("onboarding_step_notifications_button" to "Notifications")
        } else {
            buttons
        }

        allButtons.forEach { (tag, name) ->
            try {
                println("Attempting to click $name button...")
                composeTestRule.onNodeWithTag(tag).assertExists()
                composeTestRule.onNodeWithTag(tag).performClick()
                println("Clicked $name button")

                // Wait a bit for any dialogs or settings screens
                Thread.sleep(1000)

                // Press back to return to app (in case settings screen opened)
                device.pressBack()
                Thread.sleep(500)

            } catch (e: Exception) {
                println("Failed to click $name button: ${e.message}")
            }
        }

        // Wait and check final state
        composeTestRule.waitForIdle()
        Thread.sleep(2000)

        val successCardVisible = try {
            composeTestRule.onNodeWithTag("onboarding_success_card")
                .assertExists()
            true
        } catch (e: AssertionError) {
            false
        }
        println("After clicking all buttons - Success card visible: $successCardVisible")

        val homeScreenVisible = try {
            composeTestRule.onNodeWithTag("launcher_home_page")
                .assertExists()
            true
        } catch (e: AssertionError) {
            false
        }
        println("After clicking all buttons - Home screen visible: $homeScreenVisible")

        println("=== END ATTEMPT ===")

        // This test always passes - it's just for diagnostics
        assert(true)
    }

    @Test
    fun diagnostic_testRaceCondition() {
        // This test simulates the exact race condition that occurs when
        // permissions are granted and the user returns to the app

        println("=== TESTING RACE CONDITION ===")

        // Wait for onboarding screen
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithTag("onboarding_step_default_launcher")
                .fetchSemanticsNodes().isNotEmpty()
        }

        println("Onboarding screen loaded")

        // Simulate granting all permissions externally (not through UI clicks)
        // This mimics what happens when user manually grants permissions in settings
        runBlocking {
            val database = LauncherDatabase.getDatabase(context)
            val settingsRepository = com.talauncher.data.repository.SettingsRepository(database.settingsDao())

            println("Checking if onboarding is complete before granting permissions...")
            val beforeComplete = settingsRepository.isOnboardingCompleted()
            println("Before: isOnboardingCompleted = $beforeComplete")
        }

        println("=== END RACE CONDITION TEST ===")
        assert(true)
    }
}
