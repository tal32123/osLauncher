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
}
