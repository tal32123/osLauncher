package com.talauncher

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherPagerNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun pagerRespondsToNavigationAndBackPress() {
        // Verify starts on home page
        composeRule.onNodeWithTag("launcher_home_page").assertExists()

        // Swipe to settings page (page 0)
        composeRule.onNodeWithTag("launcher_navigation_pager")
            .performTouchInput { swipeRight() }
        composeRule.onNodeWithTag("launcher_settings_page").assertExists()

        // Back should return to home page (page 1)
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.onNodeWithTag("launcher_home_page").assertExists()
    }
}

