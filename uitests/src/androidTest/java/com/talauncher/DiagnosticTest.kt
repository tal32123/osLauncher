package com.talauncher

import android.util.Log
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
class DiagnosticTest {

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

    @Test
    fun diagnosticTest_identifyCurrentUIState() {
        Log.d("DiagnosticTest", "=== STARTING DIAGNOSTIC TEST ===")

        // Wait for UI to stabilize
        composeTestRule.waitForIdle()

        Log.d("DiagnosticTest", "Checking what UI elements are currently visible...")

        // Check for onboarding screen elements
        try {
            composeTestRule.onNodeWithText("Set as default launcher").assertExists()
            Log.d("DiagnosticTest", "✓ FOUND: Onboarding screen - 'Set as default launcher' button")
        } catch (e: AssertionError) {
            Log.d("DiagnosticTest", "✗ NOT FOUND: Onboarding screen")
        }

        try {
            composeTestRule.onNodeWithTag("onboarding_success_card").assertExists()
            Log.d("DiagnosticTest", "✓ FOUND: Onboarding success card")
        } catch (e: AssertionError) {
            Log.d("DiagnosticTest", "✗ NOT FOUND: Onboarding success card")
        }

        try {
            composeTestRule.onNodeWithTag("onboarding_incomplete_message").assertExists()
            Log.d("DiagnosticTest", "✓ FOUND: Onboarding incomplete message")
        } catch (e: AssertionError) {
            Log.d("DiagnosticTest", "✗ NOT FOUND: Onboarding incomplete message")
        }

        // Check for loading screen
        try {
            composeTestRule.onNodeWithText("TALauncher").assertExists()
            Log.d("DiagnosticTest", "✓ FOUND: Loading screen with app name")
        } catch (e: AssertionError) {
            Log.d("DiagnosticTest", "✗ NOT FOUND: Loading screen")
        }

        // Check for main navigation elements
        try {
            composeTestRule.onNodeWithTag("launcher_navigation_pager").assertExists()
            Log.d("DiagnosticTest", "✓ FOUND: Main navigation pager")
        } catch (e: AssertionError) {
            Log.d("DiagnosticTest", "✗ NOT FOUND: Main navigation pager")
        }

        try {
            composeTestRule.onNodeWithTag("launcher_home_page").assertExists()
            Log.d("DiagnosticTest", "✓ FOUND: Home page container")
        } catch (e: AssertionError) {
            Log.d("DiagnosticTest", "✗ NOT FOUND: Home page container")
        }

        try {
            composeTestRule.onNodeWithTag("launcher_settings_page").assertExists()
            Log.d("DiagnosticTest", "✓ FOUND: Settings page container")
        } catch (e: AssertionError) {
            Log.d("DiagnosticTest", "✗ NOT FOUND: Settings page container")
        }

        // Check for home screen elements
        try {
            composeTestRule.onNodeWithTag("search_field").assertExists()
            Log.d("DiagnosticTest", "✓ FOUND: Search field")
        } catch (e: AssertionError) {
            Log.d("DiagnosticTest", "✗ NOT FOUND: Search field")
        }

        try {
            composeTestRule.onNodeWithTag("app_list").assertExists()
            Log.d("DiagnosticTest", "✓ FOUND: App list")
        } catch (e: AssertionError) {
            Log.d("DiagnosticTest", "✗ NOT FOUND: App list")
        }

        try {
            composeTestRule.onNodeWithTag("alphabet_index").assertExists()
            Log.d("DiagnosticTest", "✓ FOUND: Alphabet index")
        } catch (e: AssertionError) {
            Log.d("DiagnosticTest", "✗ NOT FOUND: Alphabet index")
        }

        // Print semantic tree for debugging
        Log.d("DiagnosticTest", "=== PRINTING SEMANTIC TREE ===")
        try {
            composeTestRule.onRoot().printToLog("DIAGNOSTIC_TREE")
        } catch (e: Exception) {
            Log.e("DiagnosticTest", "Failed to print semantic tree: ${e.message}")
        }

        Log.d("DiagnosticTest", "=== DIAGNOSTIC TEST COMPLETED ===")

        // This test should always pass - it's just for diagnostics
        assert(true)
    }
}