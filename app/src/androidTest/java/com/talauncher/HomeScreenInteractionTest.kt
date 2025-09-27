package com.talauncher

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenInteractionTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val intentsTestRule = IntentsTestRule(MainActivity::class.java)

    @Before
    fun setUp() {
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun launchAppFromAllAppsList() {
        // Scenario: Launch an app from the "All Apps" list
        // 1. Scroll down to the "All Apps" list.
        // Note: Scrolling is not explicitly implemented, assuming the app is visible.
        // 2. Find an app with a known name (e.g., "Calculator").
        val appNameToLaunch = "Calculator"
        composeTestRule.onNodeWithText(appNameToLaunch).assertExists()

        // 3. Click on the app item.
        composeTestRule.onNodeWithText(appNameToLaunch).performClick()

        // 4. Verification: Assert that an intent to launch an app was sent.
        Intents.intended(IntentMatchers.hasAction(android.content.Intent.ACTION_MAIN))
    }
}
