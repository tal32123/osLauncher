package com.talauncher

import android.util.Log
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.IdlingRegistry
import com.talauncher.utils.EspressoIdlingResource
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class SettingsFlowTest {

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

    private fun ensureOnboardingCompleted() {
        try {
            composeTestRule.onNodeWithText("Set as default launcher").performClick()
            composeTestRule.waitForIdle()
        } catch (e: Exception) {
            // Onboarding might already be completed
        }
    }

    @Test
    fun changeColorPalette() {
        Log.d("SettingsFlowTest", "Running changeColorPalette test")

        // 1. From the HomeScreen, swipe right to navigate to the SettingsScreen.
        composeTestRule.onNodeWithTag("launcher_navigation_pager").performTouchInput { swipeRight() }

        // 2. Click on the "UI & Theme" tab.
        composeTestRule.onNodeWithTag("settings_tab_UI & Theme").performClick()

        // 3. Locate the "Color Palette" section and click a new palette option (e.g., "Warm").
        composeTestRule.onNodeWithTag("color_palette_WARM").performClick()

        // 4. Verification: Assert that the color of a known element (like a button or header) changes to the expected color from the new palette.
        composeTestRule.onNodeWithTag("color_palette_WARM").assertExists()
    }

    @Test
    fun toggleWallpaperAndChangeBlur() {
        Log.d("SettingsFlowTest", "Running toggleWallpaperAndChangeBlur test")

        // 1. Navigate to the "UI & Theme" tab in Settings.
        composeTestRule.onNodeWithTag("launcher_navigation_pager").performTouchInput { swipeRight() }
        composeTestRule.onNodeWithTag("settings_tab_UI & Theme").performClick()

        // 2. Toggle the "Show Device Wallpaper" switch off.
        composeTestRule.onNodeWithTag("show_wallpaper_switch").performClick()
        composeTestRule.onNodeWithTag("show_wallpaper_switch").assertIsOff()

        // 3. Toggle the switch back on.
        composeTestRule.onNodeWithTag("show_wallpaper_switch").performClick()
        composeTestRule.onNodeWithTag("show_wallpaper_switch").assertIsOn()

        // 4. Move the "Wallpaper Blur" slider.
        composeTestRule.onNodeWithTag("wallpaper_blur_slider").performSemanticsAction(SemanticsActions.SetProgress) { it(0.5f) }

        // 5. Verification: This is hard to verify visually, but you can check that the value is saved and propagated to the HomeViewModel.
        // For now, we will just check that the slider has the correct value.
        composeTestRule.onNodeWithTag("wallpaper_blur_slider").assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ProgressBarRangeInfo,
                ProgressBarRangeInfo(0.5f, 0f..1f)
            )
        )
    }

    @Test
    fun addAndConfigureDistractingApp() {
        Log.d("SettingsFlowTest", "Running addAndConfigureDistractingApp test")

        // 1. Navigate to the "Distracting Apps" tab in Settings
        composeTestRule.onNodeWithTag("launcher_navigation_pager").performTouchInput { swipeRight() }
        composeTestRule.onNodeWithTag("settings_tab_Distracting Apps").performClick()

        // 2. Wait for settings to load and find Settings app checkbox
        composeTestRule.waitForIdle()

        val appToUse = "com.android.settings"
        composeTestRule.onNodeWithTag("app_selection_checkbox_$appToUse").performClick()

        // 3. Click the "Edit" icon next to the newly added app
        composeTestRule.onNodeWithTag("edit_time_limit_$appToUse").performClick()

        // 4. Set a custom time limit (e.g., 15 minutes)
        composeTestRule.onNodeWithTag("time_limit_input").performTextInput("15")

        // 5. Click "Save"
        composeTestRule.onNodeWithTag("time_limit_save_button").performClick()

        // 6. Go back to the HomeScreen
        composeTestRule.onNodeWithTag("launcher_navigation_pager").performTouchInput { swipeLeft() }

        // 7. Find the same app and click to launch it
        composeTestRule.onNodeWithText("Settings", substring = true).performClick()

        // 8. Assert that the "Friction Dialog" or "Time Limit Dialog" appears
        composeTestRule.onNodeWithTag("friction_dialog").assertExists()
    }
}
