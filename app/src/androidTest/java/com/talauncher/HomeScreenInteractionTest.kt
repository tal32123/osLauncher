package com.talauncher

import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.times
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.IdlingRegistry
import com.talauncher.utils.EspressoIdlingResource
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenInteractionTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        Intents.init()
        IdlingRegistry.getInstance().register(EspressoIdlingResource.getIdlingResource())
        ensureOnboardingCompleted()
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.getIdlingResource())
        Intents.release()
    }

    private fun ensureOnboardingCompleted() {
        try {
            // Check if we're on onboarding screen
            composeTestRule.onNodeWithTag("onboarding_step_default_launcher_button").assertExists()

            // Complete all onboarding steps
            composeTestRule.onNodeWithTag("onboarding_step_usage_stats_button").performClick()
            composeTestRule.waitForIdle()

            // Grant notifications permission if required (Android 13+)
            try {
                composeTestRule.onNodeWithTag("onboarding_step_notifications_button").performClick()
                composeTestRule.waitForIdle()
            } catch (e: Exception) {
                // Notifications permission not required on this API level
            }

            composeTestRule.onNodeWithTag("onboarding_step_overlay_button").performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("onboarding_step_default_launcher_button").performClick()
            composeTestRule.waitForIdle()

            // Wait for onboarding completion and navigation to main app
            composeTestRule.waitUntil(timeoutMillis = 10_000) {
                try {
                    composeTestRule.onNodeWithTag("launcher_navigation_pager").assertExists()
                    true
                } catch (e: AssertionError) {
                    false
                }
            }
        } catch (e: Exception) {
            // Onboarding might already be completed, or we're already on the main app
        }
    }

    @Test
    fun launchAppFromAllAppsList() {
        Log.d("HomeScreenInteractionTest", "Running launchAppFromAllAppsList test")

        // Scenario: Launch an app from the "All Apps" list
        // 1. Wait for the app list to load and find the first available app
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

        // 2. Get the first app from the list
        val firstAppNode = composeTestRule.onNodeWithTag("app_list")
            .onChildren()
            .onFirst()

        firstAppNode.assertExists()

        // 3. Click on the first app item
        firstAppNode.performClick()

        // 4. Verification: Assert that an intent to launch an app was sent
        Intents.intended(IntentMatchers.hasAction(android.content.Intent.ACTION_MAIN))
    }

    @Test
    fun launchRecentApp() {
        Log.d("HomeScreenInteractionTest", "Running launchRecentApp test")

        // Scenario: Launch a "Recent App"
        // 1. Wait for app list and launch the first app to make it "recent"
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

        firstAppNode.performClick()
        Intents.intended(IntentMatchers.hasAction(android.content.Intent.ACTION_MAIN))

        // 2. Return to the launcher
        composeTestRule.activity.onBackPressedDispatcher.onBackPressed()

        // 3. Wait a moment for the recent app to appear
        composeTestRule.waitForIdle()

        // 4. Find and click the same app (should now be in recent apps)
        firstAppNode.performClick()

        // 5. Verification: Assert that the app launches successfully
        // We expect two intents for the same action, since we launched the app twice
        Intents.intended(IntentMatchers.hasAction(android.content.Intent.ACTION_MAIN), times(2))
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

        val nodeWidth = alphabetBounds.width
        val nodeHeight = alphabetBounds.height

        alphabetIndexNode.performTouchInput {
            val touchX = nodeWidth / 2f
            val touchY = relativeY.coerceIn(0f, nodeHeight)
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
            .assert(
                hasText(targetLetter, substring = true)
            )
    }

    @Test
    fun searchAndLaunchApp() {
        Log.d("HomeScreenInteractionTest", "Running searchAndLaunchApp test")

        // Scenario: Perform a search and launch an app
        // 1. Tap the search bar at the top
        composeTestRule.onNodeWithTag("search_field").performClick()

        // 2. Type the name of a common system app (Settings)
        val appNameToSearch = "Settings"
        composeTestRule.onNodeWithTag("search_field").performTextInput(appNameToSearch)

        // 3. Wait for search results
        composeTestRule.waitForIdle()

        // 4. In the search results, click on the first result or the Settings app
        composeTestRule.onNodeWithText(appNameToSearch, substring = true).performClick()

        // 5. Verification: Assert that the app launches successfully
        Intents.intended(IntentMatchers.hasAction(android.content.Intent.ACTION_MAIN))
    }

    @Test
    fun searchAndLaunchContactAction() {
        Log.d("HomeScreenInteractionTest", "Running searchAndLaunchContactAction test")

        // Scenario: Perform a search and launch a contact action
        // 1. Tap the search bar
        composeTestRule.onNodeWithTag("search_field").performClick()

        // 2. Type the name of a known contact
        val contactNameToSearch = "John Doe"
        composeTestRule.onNodeWithTag("search_field").performTextInput(contactNameToSearch)

        // 3. Wait for search results
        composeTestRule.waitForIdle()

        // 4. In the search results, find the contact item and click the "call" icon
        composeTestRule.onNodeWithText("Call").performClick()

        // 5. Verification: Assert that an INTENT to ACTION_DIAL was initiated
        Intents.intended(IntentMatchers.hasAction(android.content.Intent.ACTION_DIAL))

        // 6. In the search results, find the contact item and click the "message" icon
        composeTestRule.onNodeWithText("Message").performClick()

        // 7. Verification: Assert that an INTENT to ACTION_SENDTO was initiated
        Intents.intended(IntentMatchers.hasAction(android.content.Intent.ACTION_SENDTO))
    }
}
