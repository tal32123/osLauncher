# Comprehensive UI Test Plan

This document outlines a comprehensive set of UI tests using Espresso to ensure full user flow coverage and prevent regressions during refactoring. The tests are organized by feature, following Android testing best practices.

---

## 1. `HomeScreenInteractionTest.kt`

This suite focuses on the core functionality of the home screen.

### Test Scenarios:

#### Scenario: Launch an app from the "All Apps" list [COMPLETED]
- **Goal:** Verify that a user can find and launch a standard application.
- **Steps:**
    1. Scroll down to the "All Apps" list.
    2. Find an app with a known name (e.g., "Calculator").
    3. Click on the app item.
    4. **Verification:** Assert that the launcher is no longer in the foreground (e.g., by checking for a UI element from the launched app, though this can be complex) or that an intent to launch the app was sent.

#### Scenario: Launch a "Recent App" [COMPLETED]
- **Goal:** Ensure the "Recent Apps" section is functional.
- **Steps:**
    1. Launch an app to make it "recent".
    2. Return to the launcher.
    3. Find the same app in the "Recent Apps" section at the top.
    4. Click on it.
    5. **Verification:** Assert that the app launches successfully.

#### Scenario: Use the Alphabetical Index Scrubber [COMPLETED]
- **Note:** The current test implementation is basic. It verifies that the scrubber responds to a swipe gesture but does not assert that the list scrolls to the correct letter due to the imprecision of swipe gestures in tests. A more robust test would require a more sophisticated way to interact with the scrubber.
- **Goal:** Test the fast-scrolling functionality.
- **Steps:**
    1. On the right side of the screen, press and drag the alphabetical index.
    2. Drag to a specific letter (e.g., "C").
    3. **Verification:** Assert that the app list scrolls and the first visible app starts with "C".

#### Scenario: Perform a search and launch an app [COMPLETED]
- **Goal:** Verify the search functionality and app launching from search results.
- **Steps:**
    1. Tap the search bar at the top.
    2. Type the name of a known app (e.g., "Clock").
    3. In the search results, click on the app item.
    4. **Verification:** Assert that the app launches successfully.

#### Scenario: Perform a search and launch a contact action
- **Goal:** Verify that contact search and actions (call, message) work.
- **Steps:**
    1. Tap the search bar.
    2. Type the name of a known contact.
    3. In the search results, find the contact item.
    4. Click the "call" or "message" icon.
    5. **Verification:** Assert that an `INTENT` to either `ACTION_DIAL` or `ACTION_SENDTO` was initiated. Use `Intents.intended(...)`.

---

## 2. `SettingsFlowTest.kt`

This suite tests the various options within the multi-tabbed settings screen.

### Test Scenarios:

#### Scenario: Navigate to Settings and change the color palette
- **Goal:** Ensure UI theme settings are applied correctly.
- **Steps:**
    1. From the `HomeScreen`, swipe right to navigate to the `SettingsScreen`.
    2. Click on the "UI & Theme" tab.
    3. Locate the "Color Palette" section.
    4. Click a new palette option (e.g., "Warm").
    5. **Verification:** Assert that the color of a known element (like a button or header) changes to the expected color from the new palette.

#### Scenario: Enable/Disable wallpaper and change blur
- **Goal:** Test the wallpaper and background effects settings.
- **Steps:**
    1. Navigate to the "UI & Theme" tab in Settings.
    2. Toggle the "Show Device Wallpaper" switch off.
    3. **Verification:** Assert that the background is now a solid color.
    4. Toggle the switch back on.
    5. Move the "Wallpaper Blur" slider.
    6. **Verification:** This is hard to verify visually, but you can check that the value is saved and propagated to the `HomeViewModel`.

#### Scenario: Add and configure a "Distracting App"
- **Goal:** Test the core friction-barrier functionality.
- **Steps:**
    1. Navigate to the "Distracting Apps" tab in Settings.
    2. Find a specific app in the list and check the box next to it.
    3. Click the "Edit" icon next to the newly added app.
    4. Set a custom time limit (e.g., 15 minutes).
    5. Click "Save".
    6. **Verification:** Go back to the `HomeScreen`, find the same app, and click to launch it. Assert that the "Friction Dialog" or "Time Limit Dialog" appears.

---

## 3. `DialogsAndPermissionsTest.kt`

This suite focuses on verifying the behavior of various dialogs and permission requests.

### Test Scenarios:

#### Scenario: App action dialog (Hide/Rename)
- **Goal:** Test the long-press context menu on apps.
- **Steps:**
    1. On the `HomeScreen`, long-press an app.
    2. An "App Action Dialog" should appear.
    3. Click the "Hide app" button.
    4. **Verification:** Assert that the app is no longer visible in the main list. You may need to check a "Show hidden apps" section if one exists.
    5. Long-press another app and choose "Rename".
    6. Enter a new name and save.
    7. **Verification:** Assert the app's display name is updated in the list.

#### Scenario: Friction dialog for distracting apps
- **Goal:** Ensure the friction barrier works as intended.
- **Steps:**
    1. First, mark an app as "distracting" in settings.
    2. On the `HomeScreen`, click the distracting app.
    3. The "Mindful Usage" dialog should appear.
    4. Type a reason in the text field and click "Continue".
    5. **Verification:** Assert that the app proceeds to launch.
    6. Relaunch the app, and this time click "Cancel".
    7. **Verification:** Assert that the dialog closes and the app does not launch.

#### Scenario: Contacts permission flow
- **Goal:** Test the graceful handling of missing permissions for the contacts feature.
- **Steps:**
    1. Ensure contacts permission is revoked.
    2. In the `HomeScreen` search bar, type a contact's name.
    3. A "Contacts Permission Missing" card should appear in the results.
    4. Click the "Grant" button.
    5. **Verification:** Assert that the system permission dialog for contacts is displayed.

