package com.talauncher

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.espresso.IdlingRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.talauncher.utils.EspressoIdlingResource
import com.talauncher.utils.markOnboardingComplete
import com.talauncher.utils.resetOnboardingState
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.getIdlingResource())
        resetOnboardingState()
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
    }

    @After
    fun tearDown() {
        markOnboardingComplete()
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.getIdlingResource())
    }

    @Test
    fun completeOnboardingFlowDisplaysHomeScreen() {
        waitForOnboardingToAppear()

        clickStepIfPresent("onboarding_step_usage_stats_button")
        clickStepIfPresent("onboarding_step_notifications_button")
        clickStepIfPresent("onboarding_step_overlay_button")
        clickStepIfPresent("onboarding_step_default_launcher_button")

        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithTag("launcher_navigation_pager").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("launcher_navigation_pager").assertExists()
    }

    private fun waitForOnboardingToAppear() {
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithTag("onboarding_step_default_launcher_button").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun clickStepIfPresent(tag: String) {
        val nodes = composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes()
        if (nodes.isNotEmpty()) {
            composeTestRule.onNodeWithTag(tag).performClick()
            composeTestRule.waitForIdle()
        }
    }
}
