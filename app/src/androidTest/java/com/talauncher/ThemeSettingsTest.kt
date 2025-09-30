package com.talauncher

import android.util.Log
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.IdlingRegistry
import com.talauncher.data.model.ColorPaletteOption
import com.talauncher.data.model.ThemeModeOption
import com.talauncher.utils.EspressoIdlingResource
import com.talauncher.utils.skipOnboardingIfNeeded
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive UI tests for theme functionality following Android testing best practices.
 * Tests dark/light mode toggle, color palette selection, and theme persistence.
 */
@RunWith(AndroidJUnit4::class)
class ThemeSettingsTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.getIdlingResource())
        composeTestRule.skipOnboardingIfNeeded()
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.getIdlingResource())
    }

    private fun navigateToThemeSettings() {
        // Navigate to Settings screen
        composeTestRule.onNodeWithTag("launcher_navigation_pager").performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()

        // Click on UI & Theme tab
        composeTestRule.onNodeWithTag("settings_tab_UI & Theme").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun testThemeModeSelection() {
        Log.d("ThemeSettingsTest", "Running testThemeModeSelection")
        navigateToThemeSettings()

        // Test System theme mode (default)
        composeTestRule.onNodeWithTag("theme_mode_SYSTEM")
            .assertExists()
            .assertIsSelectable()

        // Test Light theme mode selection
        composeTestRule.onNodeWithTag("theme_mode_LIGHT")
            .assertExists()
            .assertIsSelectable()
            .performClick()
        composeTestRule.waitForIdle()

        // Verify Light mode is selected
        composeTestRule.onNodeWithTag("theme_mode_LIGHT")
            .assertIsSelected()

        // Test Dark theme mode selection
        composeTestRule.onNodeWithTag("theme_mode_DARK")
            .assertExists()
            .assertIsSelectable()
            .performClick()
        composeTestRule.waitForIdle()

        // Verify Dark mode is selected and Light mode is deselected
        composeTestRule.onNodeWithTag("theme_mode_DARK")
            .assertIsSelected()
        composeTestRule.onNodeWithTag("theme_mode_LIGHT")
            .assertIsNotSelected()

        // Return to System mode
        composeTestRule.onNodeWithTag("theme_mode_SYSTEM")
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("theme_mode_SYSTEM")
            .assertIsSelected()
    }

    @Test
    fun testColorPaletteSelection() {
        Log.d("ThemeSettingsTest", "Running testColorPaletteSelection")
        navigateToThemeSettings()

        // Test each color palette option
        val palettes = listOf(
            ColorPaletteOption.DEFAULT,
            ColorPaletteOption.WARM,
            ColorPaletteOption.COOL,
            ColorPaletteOption.MONOCHROME,
            ColorPaletteOption.NATURE,
            ColorPaletteOption.OCEANIC,
            ColorPaletteOption.SUNSET,
            ColorPaletteOption.LAVENDER,
            ColorPaletteOption.CHERRY
        )

        palettes.forEach { palette ->
            // Click on palette
            composeTestRule.onNodeWithTag("color_palette_${palette.name}")
                .assertExists()
                .assertIsSelectable()
                .performClick()
            composeTestRule.waitForIdle()

            // Verify palette is selected
            composeTestRule.onNodeWithTag("color_palette_${palette.name}")
                .assertIsSelected()

            // Verify other palettes are not selected (check a few)
            if (palette != ColorPaletteOption.DEFAULT) {
                composeTestRule.onNodeWithTag("color_palette_DEFAULT")
                    .assertIsNotSelected()
            }
        }
    }

    @Test
    fun testColorPalettePreview() {
        Log.d("ThemeSettingsTest", "Running testColorPalettePreview")
        navigateToThemeSettings()

        // Test that each palette card has a preview
        ColorPaletteOption.entries.forEach { palette ->
            composeTestRule.onNodeWithTag("color_palette_${palette.name}")
                .assertExists()
                .assertHasClickAction()

            // Verify palette preview exists by checking content description
            composeTestRule.onNode(
                hasContentDescription("Theme preview showing primary, secondary and background colors")
                    .and(hasAnyAncestor(hasTestTag("color_palette_${palette.name}")))
            ).assertExists()
        }
    }

    @Test
    fun testThemeModePersistence() {
        Log.d("ThemeSettingsTest", "Running testThemeModePersistence")
        navigateToThemeSettings()

        // Select Dark mode
        composeTestRule.onNodeWithTag("theme_mode_DARK").performClick()
        composeTestRule.waitForIdle()

        // Navigate away from settings and back
        composeTestRule.onNodeWithTag("launcher_navigation_pager").performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("launcher_navigation_pager").performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_tab_UI & Theme").performClick()
        composeTestRule.waitForIdle()

        // Verify Dark mode is still selected
        composeTestRule.onNodeWithTag("theme_mode_DARK")
            .assertIsSelected()
    }

    @Test
    fun testColorPalettePersistence() {
        Log.d("ThemeSettingsTest", "Running testColorPalettePersistence")
        navigateToThemeSettings()

        // Select Nature palette
        composeTestRule.onNodeWithTag("color_palette_NATURE").performClick()
        composeTestRule.waitForIdle()

        // Navigate away from settings and back
        composeTestRule.onNodeWithTag("launcher_navigation_pager").performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("launcher_navigation_pager").performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_tab_UI & Theme").performClick()
        composeTestRule.waitForIdle()

        // Verify Nature palette is still selected
        composeTestRule.onNodeWithTag("color_palette_NATURE")
            .assertIsSelected()
    }

    @Test
    fun testThemeComponentsAccessibility() {
        Log.d("ThemeSettingsTest", "Running testThemeComponentsAccessibility")
        navigateToThemeSettings()

        // Test theme mode selector accessibility
        ThemeModeOption.entries.forEach { mode ->
            composeTestRule.onNodeWithTag("theme_mode_${mode.name}")
                .assertExists()
                .assertIsSelectable()
                .onSiblings()
                .filterToOne(hasContentDescription("Select ${mode.label} theme mode"))
                .assertExists()
        }

        // Test color palette accessibility
        ColorPaletteOption.entries.forEach { palette ->
            composeTestRule.onNodeWithTag("color_palette_${palette.name}")
                .assertExists()
                .assertIsSelectable()
                .onSiblings()
                .filterToOne(hasContentDescription("Select ${palette.label} color palette"))
                .assertExists()
        }
    }

    @Test
    fun testThemeUIVisualElements() {
        Log.d("ThemeSettingsTest", "Running testThemeUIVisualElements")
        navigateToThemeSettings()

        // Verify theme mode selector exists and has expected content
        composeTestRule.onNodeWithText("Theme Mode")
            .assertExists()
        composeTestRule.onNodeWithText("Choose between light, dark, or system theme")
            .assertExists()

        // Verify color palette selector exists and has expected content
        composeTestRule.onNodeWithText("Color Palette")
            .assertExists()
        composeTestRule.onNodeWithText("Choose your preferred color scheme")
            .assertExists()

        // Verify all theme mode options are visible
        composeTestRule.onNodeWithText("System").assertExists()
        composeTestRule.onNodeWithText("Light").assertExists()
        composeTestRule.onNodeWithText("Dark").assertExists()

        // Verify color palette options are visible
        ColorPaletteOption.entries.forEach { palette ->
            composeTestRule.onNodeWithText(palette.label).assertExists()
        }
    }

    @Test
    fun testCombinedThemeAndPaletteSelection() {
        Log.d("ThemeSettingsTest", "Running testCombinedThemeAndPaletteSelection")
        navigateToThemeSettings()

        // Select Dark mode and Warm palette
        composeTestRule.onNodeWithTag("theme_mode_DARK").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("color_palette_WARM").performClick()
        composeTestRule.waitForIdle()

        // Verify both are selected
        composeTestRule.onNodeWithTag("theme_mode_DARK").assertIsSelected()
        composeTestRule.onNodeWithTag("color_palette_WARM").assertIsSelected()

        // Change to Light mode with Cool palette
        composeTestRule.onNodeWithTag("theme_mode_LIGHT").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("color_palette_COOL").performClick()
        composeTestRule.waitForIdle()

        // Verify new selection
        composeTestRule.onNodeWithTag("theme_mode_LIGHT").assertIsSelected()
        composeTestRule.onNodeWithTag("color_palette_COOL").assertIsSelected()

        // Verify previous selections are deselected
        composeTestRule.onNodeWithTag("theme_mode_DARK").assertIsNotSelected()
        composeTestRule.onNodeWithTag("color_palette_WARM").assertIsNotSelected()
    }

    @Test
    fun testThemeSettingsCardLayout() {
        Log.d("ThemeSettingsTest", "Running testThemeSettingsCardLayout")
        navigateToThemeSettings()

        // Verify both theme mode and color palette selectors are displayed as cards
        // This tests the visual structure without being too brittle
        composeTestRule.onNodeWithText("Theme Mode")
            .assertExists()
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("Color Palette")
            .assertExists()
            .assertIsDisplayed()

        // Verify the cards are scrollable (by checking parent scrollable component exists)
        composeTestRule.onNodeWithTag("launcher_navigation_pager")
            .assertExists()
    }
}