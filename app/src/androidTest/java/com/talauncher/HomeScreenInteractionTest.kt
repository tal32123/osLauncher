package com.talauncher

import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertTextStartsWith
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.waitUntil
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.times
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
        Log.d("HomeScreenInteractionTest", "Running launchAppFromAllAppsList test")
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

    @Test
    fun launchRecentApp() {
        Log.d("HomeScreenInteractionTest", "Running launchRecentApp test")
        // Scenario: Launch a "Recent App"
        // 1. Launch an app to make it "recent".
        val appNameToLaunch = "Calculator"
        composeTestRule.onNodeWithText(appNameToLaunch).performClick()
        Intents.intended(IntentMatchers.hasAction(android.content.Intent.ACTION_MAIN))

        // 2. Return to the launcher.
        composeTestRule.activity.onBackPressedDispatcher.onBackPressed()

        // 3. Find the same app in the "Recent Apps" section at the top.
        // Assuming the recent app appears on the home screen without needing to scroll.
        // We are looking for the same app, so we can use the same name.
        composeTestRule.onNodeWithText(appNameToLaunch).assertExists()

        // 4. Click on it.
        composeTestRule.onNodeWithText(appNameToLaunch).performClick()

        // 5. Verification: Assert that the app launches successfully.
        // We expect two intents for the same action, since we launched the app twice.
        Intents.intended(IntentMatchers.hasAction(android.content.Intent.ACTION_MAIN), Intents.times(2))
    }

    @Test
    fun useAlphabeticalIndexScrubber() {
        Log.d("HomeScreenInteractionTest", "Running useAlphabeticalIndexScrubber test")
        val targetLetter = "C"
        val alphabetIndexTag = "alphabet_index"
        val targetEntryTag = "alphabet_index_entry_${targetLetter}"

        val alphabetIndexNode = composeTestRule.onNodeWithTag(alphabetIndexTag)
        val alphabetBounds = alphabetIndexNode.fetchSemanticsNode().boundsInRoot

        val targetEntryNode = composeTestRule.onNodeWithTag(targetEntryTag, useUnmergedTree = true)
        targetEntryNode.assertExists()
        val entryBounds = targetEntryNode.fetchSemanticsNode().boundsInRoot

        val relativeY = ((entryBounds.top + entryBounds.bottom) / 2f) - alphabetBounds.top

        alphabetIndexNode.performTouchInput {
            val touchX = size.width / 2f
            val touchY = relativeY.coerceIn(0f, size.height.toFloat())
            down(Offset(touchX, touchY))
            advanceEventTime(100L)
            up()
        }

        composeTestRule.waitUntil {
            val semanticsNode = runCatching {
                composeTestRule.onNodeWithTag("app_list", useUnmergedTree = true)
                    .onChildren()
                    .onFirst()
                    .fetchSemanticsNode()
            }.getOrNull() ?: return@waitUntil false

            val text = semanticsNode.config.getOrNull(SemanticsProperties.Text)
                ?.firstOrNull()
                ?.text
                .orEmpty()
            text.startsWith(targetLetter)
        }

        composeTestRule.onNodeWithTag("app_list", useUnmergedTree = true)
            .onChildren()
            .onFirst()
            .assertTextStartsWith(targetLetter)
    }

    @Test
    fun searchAndLaunchApp() {
        Log.d("HomeScreenInteractionTest", "Running searchAndLaunchApp test")
        // Scenario: Perform a search and launch an app
        // 1. Tap the search bar at the top.
        val searchBarPlaceholder = "Search apps, contacts, and web..."
        composeTestRule.onNodeWithText(searchBarPlaceholder).performClick()

        // 2. Type the name of a known app (e.g., "Clock").
        val appNameToSearch = "Clock"
        composeTestRule.onNodeWithText(searchBarPlaceholder).performTextInput(appNameToSearch)

        // 3. In the search results, click on the app item.
        composeTestRule.onNodeWithText(appNameToSearch).performClick()

        // 4. Verification: Assert that the app launches successfully.
        Intents.intended(IntentMatchers.hasAction(android.content.Intent.ACTION_MAIN))
    }

    @Test
    fun searchAndLaunchContactAction() {
        Log.d("HomeScreenInteractionTest", "Running searchAndLaunchContactAction test")
        // Scenario: Perform a search and launch a contact action
        // 1. Tap the search bar.
        val searchBarPlaceholder = "Search apps, contacts, and web..."
        composeTestRule.onNodeWithText(searchBarPlaceholder).performClick()

        // 2. Type the name of a known contact.
        val contactNameToSearch = "John Doe"
        composeTestRule.onNodeWithText(searchBarPlaceholder).performTextInput(contactNameToSearch)

        // 3. In the search results, find the contact item and click the "call" icon.
        composeTestRule.onNodeWithText("Call").performClick()

        // 4. Verification: Assert that an INTENT to ACTION_DIAL was initiated.
        Intents.intended(IntentMatchers.hasAction(android.content.Intent.ACTION_DIAL))

        // 5. In the search results, find the contact item and click the "message" icon.
        composeTestRule.onNodeWithText("Message").performClick()

        // 6. Verification: Assert that an INTENT to ACTION_SENDTO was initiated.
        Intents.intended(IntentMatchers.hasAction(android.content.Intent.ACTION_SENDTO))
    }
}
