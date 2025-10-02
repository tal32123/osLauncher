package com.talauncher.uitests

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.IdlingRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.talauncher.MainActivity
import com.talauncher.data.database.LauncherDatabase
import com.talauncher.data.repository.SettingsRepository
import com.talauncher.uitests.utils.TestPermissionsHelper
import com.talauncher.uitests.utils.TestUsageStatsHelper
import com.talauncher.utils.EspressoIdlingResource
import com.talauncher.utils.PermissionState
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive UI test for the onboarding flow.
 *
 * This test verifies that:
 * 1. Onboarding screen is shown when user first launches the app
 * 2. All required permissions and settings are presented
 * 3. User can complete onboarding by granting all permissions and setting as default launcher
 * 4. User successfully reaches the home screen after onboarding completion
 *
 * Best practices used:
 * - Uses test tags (IDs) rather than text for element selection
 * - Uses dependency injection with mock implementations for testability
 * - Uses IdlingResource to handle async operations
 * - Tests actual user flow from start to finish
 */
@RunWith(AndroidJUnit4::class)
class OnboardingFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var context: Context
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var testPermissionsHelper: TestPermissionsHelper
    private lateinit var testUsageStatsHelper: TestUsageStatsHelper

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Clear all data before each test
        runBlocking {
            val database = LauncherDatabase.getDatabase(context)
            database.clearAllTables()
        }

        // Register IdlingResource for async operations
        IdlingRegistry.getInstance().register(EspressoIdlingResource.getIdlingResource())

        // Initialize repository
        val database = LauncherDatabase.getDatabase(context)
        settingsRepository = SettingsRepository(database.settingsDao())

        // Initialize test helpers with no permissions granted initially
        testPermissionsHelper = TestPermissionsHelper(
            context = context,
            initialPermissionState = PermissionState(
                hasUsageStats = false,
                hasNotifications = false,
                hasContacts = false,
                hasCallPhone = false,
                hasLocation = false
            )
        )

        testUsageStatsHelper = TestUsageStatsHelper(
            context = context,
            isDefaultLauncherState = false
        )
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.getIdlingResource())
    }

    @Test
    fun onboardingFlow_completeAllSteps_reachesHomeScreen() {
        // Wait for onboarding screen to appear
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("onboarding_step_default_launcher")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verify onboarding screen is displayed
        composeTestRule.onNodeWithTag("onboarding_step_default_launcher")
            .assertExists("Default launcher step should be visible")

        // Verify all onboarding steps are present
        composeTestRule.onNodeWithTag("onboarding_step_usage_stats")
            .assertExists("Usage stats step should be visible")

        composeTestRule.onNodeWithTag("onboarding_step_contacts")
            .assertExists("Contacts step should be visible")

        composeTestRule.onNodeWithTag("onboarding_step_location")
            .assertExists("Location step should be visible")

        // Verify incomplete message is shown
        composeTestRule.onNodeWithTag("onboarding_incomplete_message")
            .assertExists("Incomplete message should be visible")

        // Step 1: Set as default launcher
        composeTestRule.onNodeWithTag("onboarding_step_default_launcher_button")
            .assertExists("Default launcher button should exist")
            .performClick()

        // Simulate setting as default launcher
        testUsageStatsHelper.setDefaultLauncher(true)
        composeTestRule.waitForIdle()

        // Step 2: Grant usage stats permission
        composeTestRule.onNodeWithTag("onboarding_step_usage_stats_button")
            .assertExists("Usage stats button should exist")
            .performClick()

        // Simulate granting permission
        testPermissionsHelper.grantPermission(com.talauncher.utils.PermissionType.USAGE_STATS)
        composeTestRule.waitForIdle()

        // Step 3: Grant notifications permission (if Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            composeTestRule.onNodeWithTag("onboarding_step_notifications_button")
                .assertExists("Notifications button should exist")
                .performClick()

            testPermissionsHelper.grantPermission(com.talauncher.utils.PermissionType.NOTIFICATIONS)
            composeTestRule.waitForIdle()
        }

        // Step 4: Grant contacts permission
        composeTestRule.onNodeWithTag("onboarding_step_contacts_button")
            .assertExists("Contacts button should exist")
            .performClick()

        testPermissionsHelper.grantPermission(com.talauncher.utils.PermissionType.CONTACTS)
        composeTestRule.waitForIdle()

        // Step 5: Grant location permission
        composeTestRule.onNodeWithTag("onboarding_step_location_button")
            .assertExists("Location button should exist")
            .performClick()

        testPermissionsHelper.grantPermission(com.talauncher.utils.PermissionType.LOCATION)
        composeTestRule.waitForIdle()

        // Wait for success card to appear
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("onboarding_success_card")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verify success card is shown
        composeTestRule.onNodeWithTag("onboarding_success_card")
            .assertExists("Success card should be visible after completing all steps")

        // Wait for navigation to home screen
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithTag("launcher_home_page")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verify we've reached the home screen
        composeTestRule.onNodeWithTag("launcher_home_page")
            .assertExists("Should navigate to home screen after onboarding completion")

        // Verify we're NOT on the onboarding screen anymore
        composeTestRule.onNodeWithTag("onboarding_step_default_launcher")
            .assertDoesNotExist()

        // Verify onboarding completion is persisted
        runBlocking {
            val isCompleted = settingsRepository.isOnboardingCompleted()
            assert(isCompleted) { "Onboarding should be marked as completed in database" }
        }
    }

    @Test
    fun onboardingFlow_withoutAllPermissions_showsIncompleteMessage() {
        // Wait for onboarding screen to appear
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("onboarding_step_default_launcher")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Grant only some permissions (not all)
        testPermissionsHelper.grantPermission(com.talauncher.utils.PermissionType.USAGE_STATS)
        testPermissionsHelper.grantPermission(com.talauncher.utils.PermissionType.CONTACTS)
        composeTestRule.waitForIdle()

        // Verify incomplete message is still shown
        composeTestRule.onNodeWithTag("onboarding_incomplete_message")
            .assertExists("Incomplete message should still be visible")

        // Verify success card is NOT shown
        composeTestRule.onNodeWithTag("onboarding_success_card")
            .assertDoesNotExist()

        // Verify we're still on onboarding screen
        composeTestRule.onNodeWithTag("onboarding_step_default_launcher")
            .assertExists("Should still be on onboarding screen")

        // Verify we have NOT navigated to home screen
        composeTestRule.onNodeWithTag("launcher_home_page")
            .assertDoesNotExist()
    }

    @Test
    fun onboardingFlow_withoutDefaultLauncher_doesNotComplete() {
        // Wait for onboarding screen to appear
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("onboarding_step_default_launcher")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Grant all permissions but don't set as default launcher
        testPermissionsHelper.grantAllOnboardingPermissions()
        testUsageStatsHelper.setDefaultLauncher(false) // NOT default launcher
        composeTestRule.waitForIdle()

        // Verify success card is NOT shown
        composeTestRule.onNodeWithTag("onboarding_success_card")
            .assertDoesNotExist()

        // Verify incomplete message is shown
        composeTestRule.onNodeWithTag("onboarding_incomplete_message")
            .assertExists("Incomplete message should be visible")

        // Verify we have NOT navigated to home screen
        composeTestRule.onNodeWithTag("launcher_home_page")
            .assertDoesNotExist()
    }

    @Test
    fun onboardingFlow_completedOnboarding_skipsOnboardingScreen() {
        // Mark onboarding as completed
        runBlocking {
            settingsRepository.completeOnboarding()
        }

        // Restart activity
        composeTestRule.activityRule.scenario.recreate()

        // Wait for home screen to appear (should skip onboarding)
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithTag("launcher_home_page")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verify we're on home screen
        composeTestRule.onNodeWithTag("launcher_home_page")
            .assertExists("Should go directly to home screen when onboarding is completed")

        // Verify onboarding screen is NOT shown
        composeTestRule.onNodeWithTag("onboarding_step_default_launcher")
            .assertDoesNotExist()
    }
}
