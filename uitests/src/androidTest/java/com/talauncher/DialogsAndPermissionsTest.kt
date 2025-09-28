package com.talauncher

import android.util.Log
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.espresso.IdlingRegistry
import com.talauncher.utils.EspressoIdlingResource
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class DialogsAndPermissionsTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.getIdlingResource())
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.getIdlingResource())
    }

    private fun ensureOnHomeScreen() {
        // Wait for either onboarding screen or main app to appear
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            try {
                // Check if we're already on the main app
                composeTestRule.onNodeWithTag("launcher_navigation_pager").assertExists()
                return@waitUntil true
            } catch (e: AssertionError) {
                // Check if we're on onboarding screen
                try {
                    composeTestRule.onNodeWithTag("onboarding_step_default_launcher_button").assertExists()
                    // Complete onboarding flow
                    try {
                        composeTestRule.onNodeWithTag("onboarding_step_usage_stats_button").performClick()
                        composeTestRule.waitForIdle()
                    } catch (ex: Exception) { /* Already completed */ }

                    try {
                        composeTestRule.onNodeWithTag("onboarding_step_notifications_button").performClick()
                        composeTestRule.waitForIdle()
                    } catch (ex: Exception) { /* Not required or already completed */ }

                    try {
                        composeTestRule.onNodeWithTag("onboarding_step_overlay_button").performClick()
                        composeTestRule.waitForIdle()
                    } catch (ex: Exception) { /* Already completed */ }

                    try {
                        composeTestRule.onNodeWithTag("onboarding_step_default_launcher_button").performClick()
                        composeTestRule.waitForIdle()
                    } catch (ex: Exception) { /* Already completed */ }

                    // Check if we reached main app after onboarding
                    try {
                        composeTestRule.onNodeWithTag("launcher_navigation_pager").assertExists()
                        return@waitUntil true
                    } catch (ex: AssertionError) {
                        return@waitUntil false
                    }
                } catch (e2: AssertionError) {
                    // Neither onboarding nor main app found yet
                    return@waitUntil false
                }
            }
        }
    }

    @Test
    fun appActionDialog_HideApp() {
        Log.d("DialogsAndPermissionsTest", "Running appActionDialog_HideApp test")
        ensureOnHomeScreen()

        // 1. Wait for app list to load and get first app
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("app_list")
                    .onChildren()
                    .onFirst()
                    .fetchSemanticsNode()
                true
            } catch (e: Exception) {
                false
            }
        }

        val firstAppNode = composeTestRule.onNodeWithTag("app_list")
            .onChildren()
            .onFirst()

        // 2. Long-press the first app
        firstAppNode.performTouchInput { longClick() }

        // 3. An "App Action Dialog" should appear
        composeTestRule.onNodeWithTag("app_action_dialog").assertIsDisplayed()

        // 4. Click the "Hide app" button
        composeTestRule.onNodeWithTag("hide_app_button").performClick()

        // 5. Verification: Assert that the dialog is dismissed
        composeTestRule.onNodeWithTag("app_action_dialog").assertDoesNotExist()
    }

    @Test
    fun frictionDialogForDistractingApps() {
        Log.d("DialogsAndPermissionsTest", "Running frictionDialogForDistractingApps test")
        ensureOnHomeScreen()

        // 1. Wait for app list to load and get first app
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithTag("app_list")
                    .onChildren()
                    .onFirst()
                    .fetchSemanticsNode()
                true
            } catch (e: Exception) {
                false
            }
        }

        // Get the first app's package name for settings
        val firstAppNode = composeTestRule.onNodeWithTag("app_list")
            .onChildren()
            .onFirst()

        // 2. Navigate to settings and mark the first app as distracting
        composeTestRule.onNodeWithTag("launcher_navigation_pager").performTouchInput { swipeRight() }
        composeTestRule.onNodeWithTag("settings_tab_Distracting Apps").performClick()

        // Wait for settings to load and mark first available app as distracting
        composeTestRule.waitForIdle()

        // Find any app checkbox and click it (using Settings app as fallback)
        composeTestRule.onNodeWithTag("app_selection_checkbox_com.android.settings").performClick()

        composeTestRule.onNodeWithTag("launcher_navigation_pager").performTouchInput { swipeLeft() }

        // 3. On the HomeScreen, click the distracting app (Settings)
        composeTestRule.onNodeWithText("Settings", substring = true).performClick()

        // 4. The "Mindful Usage" dialog should appear
        composeTestRule.onNodeWithTag("friction_dialog").assertIsDisplayed()

        // 5. Type a reason in the text field and click "Continue"
        composeTestRule.onNodeWithTag("friction_reason_input").performTextInput("Test reason")
        composeTestRule.onNodeWithTag("friction_continue_button").performClick()

        // 6. Verification: Assert that the app proceeds to launch
        Intents.init()
        Intents.intended(androidx.test.espresso.intent.matcher.IntentMatchers.hasAction(android.content.Intent.ACTION_MAIN))
        Intents.release()

        // 7. Relaunch the app, and this time click "Cancel"
        composeTestRule.onNodeWithText("Settings", substring = true).performClick()
        composeTestRule.onNodeWithTag("friction_cancel_button").performClick()

        // 8. Verification: Assert that the dialog closes and the app does not launch
        composeTestRule.onNodeWithTag("friction_dialog").assertDoesNotExist()
    }

    @Test
    fun contactsPermissionFlow() {
        Log.d("DialogsAndPermissionsTest", "Running contactsPermissionFlow test")
        ensureOnHomeScreen()

        // 1. Ensure contacts permission is revoked.
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val uiDevice = UiDevice.getInstance(instrumentation)
        uiDevice.executeShellCommand("pm revoke ${instrumentation.targetContext.packageName} android.permission.READ_CONTACTS")

        // 2. In the HomeScreen search bar, type a contact's name
        composeTestRule.onNodeWithTag("search_field").performClick()
        val contactName = "John Doe"
        composeTestRule.onNodeWithTag("search_field").performTextInput(contactName)

        // 3. A "Contacts Permission Missing" card should appear in the results.
        composeTestRule.onNodeWithTag("contact_permission_callout").assertIsDisplayed()

        // 4. Click the "Grant" button.
        composeTestRule.onNodeWithTag("grant_contacts_permission_button").performClick()

        // 5. Verification: Assert that the system permission dialog for contacts is displayed.
        val permissionDialog = uiDevice.findObject(By.textContains("contacts"))
        assert(permissionDialog != null)
    }
}
