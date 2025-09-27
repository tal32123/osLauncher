package com.talauncher

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.semantics.get
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun changeColorPalette() {
        // 1. From the HomeScreen, swipe right to navigate to the SettingsScreen.
        composeTestRule.onNodeWithTag("launcher_navigation_pager").performTouchInput { swipeRight() }

        // 2. Click on the "UI & Theme" tab.
        composeTestRule.onNodeWithTag("settings_tab_UI & Theme").performClick()

        // 3. Locate the "Color Palette" section and click a new palette option (e.g., "Warm").
        composeTestRule.onNodeWithTag("color_palette_WARM").performClick()

        // 4. Verification: Assert that the color of a known element (like a button or header) changes to the expected color from the new palette.
        val expectedColor = Color(0xFF422006)
        composeTestRule.onNodeWithTag("settings_title").assertTextColor(expectedColor)
    }
}

fun SemanticsNodeInteraction.assertTextColor(expectedColor: Color): SemanticsNodeInteraction {
    return assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.TextColor, expectedColor))
}
