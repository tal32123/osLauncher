package com.talauncher

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

    @Test
    fun frictionDialogForDistractingApps() {
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
}
