package com.talauncher

import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performLongClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DialogsAndPermissionsTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.espresso.intent.Intents

    @Test
    fun appActionDialog_HideApp() {
        Log.d("DialogsAndPermissionsTest", "Running appActionDialog_HideApp test")
        val appName = "Calculator"

        // 1. On the HomeScreen, long-press an app.
        composeTestRule.onNodeWithText(appName).performLongClick()

        // 2. An "App Action Dialog" should appear.
        composeTestRule.onNodeWithTag("app_action_dialog").assertIsDisplayed()

        // 3. Click the "Hide app" button.
        composeTestRule.onNodeWithTag("hide_app_button").performClick()

        // 4. Verification: Assert that the app is no longer visible in the main list.
        composeTestRule.onNodeWithText(appName).assertDoesNotExist()
    }
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice

    @Test
    fun frictionDialogForDistractingApps() {
        Log.d("DialogsAndPermissionsTest", "Running frictionDialogForDistractingApps test")
        val appName = "Calculator"

        // 1. First, mark an app as "distracting" in settings.
        composeTestRule.onNodeWithTag("launcher_navigation_pager").performTouchInput { swipeRight() }
        composeTestRule.onNodeWithTag("settings_tab_Distracting Apps").performClick()
        composeTestRule.onNodeWithTag("app_selection_checkbox_$appName").performClick()
        composeTestRule.onNodeWithTag("launcher_navigation_pager").performTouchInput { swipeLeft() }

        // 2. On the HomeScreen, click the distracting app.
        composeTestRule.onNodeWithText(appName).performClick()

        // 3. The "Mindful Usage" dialog should appear.
        composeTestRule.onNodeWithTag("friction_dialog").assertIsDisplayed()

        // 4. Type a reason in the text field and click "Continue".
        composeTestRule.onNodeWithTag("friction_reason_input").performTextInput("Test reason")
        composeTestRule.onNodeWithTag("friction_continue_button").performClick()

        // 5. Verification: Assert that the app proceeds to launch.
        Intents.init()
        Intents.intended(androidx.test.espresso.intent.matcher.IntentMatchers.hasAction(android.content.Intent.ACTION_MAIN))
        Intents.release()

        // 6. Relaunch the app, and this time click "Cancel".
        composeTestRule.onNodeWithText(appName).performClick()
        composeTestRule.onNodeWithTag("friction_cancel_button").performClick()

        // 7. Verification: Assert that the dialog closes and the app does not launch.
        composeTestRule.onNodeWithTag("friction_dialog").assertDoesNotExist()
    }

    @Test
    fun contactsPermissionFlow() {
        Log.d("DialogsAndPermissionsTest", "Running contactsPermissionFlow test")
        // 1. Ensure contacts permission is revoked.
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val uiDevice = UiDevice.getInstance(instrumentation)
        uiDevice.executeShellCommand("pm revoke ${instrumentation.targetContext.packageName} android.permission.READ_CONTACTS")

        // 2. In the HomeScreen search bar, type a contact's name.
        val searchBarPlaceholder = "Search apps, contacts, and web..."
        composeTestRule.onNodeWithText(searchBarPlaceholder).performClick()
        val contactName = "John Doe"
        composeTestRule.onNodeWithText(searchBarPlaceholder).performTextInput(contactName)

        // 3. A "Contacts Permission Missing" card should appear in the results.
        composeTestRule.onNodeWithTag("contact_permission_callout").assertIsDisplayed()

        // 4. Click the "Grant" button.
        composeTestRule.onNodeWithTag("grant_contacts_permission_button").performClick()

        // 5. Verification: Assert that the system permission dialog for contacts is displayed.
        val permissionDialog = uiDevice.findObject(By.textContains("contacts"))
        assert(permissionDialog != null)
    }
}
