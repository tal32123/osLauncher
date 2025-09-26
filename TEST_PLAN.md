# Espresso UI Test Plan

This document outlines the recommended Espresso UI test coverage for TALauncher. Each flow focuses on verifying key user journeys and edge cases across onboarding, the launcher experience, personalization, and distraction-management features.

## 1. Onboarding to Launcher Transition
- Complete onboarding and verify that the activity transitions to the launcher UI.
- Ensure `MainViewModel` persists the completion state so returning to the app skips onboarding.
- Resume the activity from the background and confirm the pager snaps back to the home page.

## 2. Pager Navigation and Back Handling
- Validate the horizontal pager defaults to the home screen.
- Swipe to the settings page and confirm navigation indicators update.
- Launch an app and observe that the pager animates to the rightmost page.
- Use the system back button from various pages and ensure the custom handler always returns to the home page instead of exiting.

## 3. Home Search and Unified Results
- Enter queries in the search field to surface app and contact results.
- Trigger Google-search actions and confirm appropriate intents fire.
- Exercise contact action buttons (call, message, WhatsApp, open contact) and verify success flows and permission gating prompts when contacts access is missing.

## 4. App List Browsing
- Inspect the recent-apps header and ensure it populates correctly.
- Scroll through the full app list, testing alphabet index scrubbing behaviour.
- Long-press list entries to open the action dialog and validate hide, app info, and uninstall flows.

## 5. Launching Distracting Apps
- Launch apps flagged as distracting to trigger time-limit prompts or friction dialogs.
- Provide reasons or custom durations when prompted and confirm the selected values apply.
- Cancel prompts and ensure overlays dismiss cleanly without side effects.

## 6. Session Expiry Lifecycle
- Simulate session expiry to activate countdown overlays and decision dialogs.
- Verify extend and close choices behave correctly, including math-challenge fallback scenarios.
- Follow the overlay-permission request path and confirm the experience recovers when permission is granted or denied.

## 7. Home Status Elements
- Toggle time/date and weather options in settings to confirm UI updates on the home screen.
- Validate clock and weather widgets respond to state changes and display error messaging when location permission is absent.

## 8. Settings Tab Navigation
- Navigate between General, UI & Theme, Distracting Apps, and Usage Insights tabs.
- Check that tab selection state updates accurately and search fields reset when leaving the app list tab.
- Launch the embedded Insights screen and verify view-model wiring.

## 9. General Settings Controls
- Toggle and adjust controls for time-limit prompts, math challenges, countdown durations, recent-apps limits, contact actions, weather display, temperature units, and build info visibility.
- Confirm persisted preferences reflect UI changes and that invalid inputs are rejected.

## 10. UI and Theme Customization
- Select palette chips, toggle wallpaper, and adjust blur/opacity sliders to ensure immediate visual feedback.
- Test custom wallpaper picker and clear actions, along with glassmorphism, UI density, and animation switches.

## 11. Distracting App Curation
- Use the search/filter field to locate apps and adjust the default time-limit slider.
- Select and deselect apps from the distracting list, confirming UI and data updates.
- Edit per-app overrides via the time-limit dialog and validate input handling.

## 12. Usage Insights Permission and Data
- Walk through the usage-access permission workflow and verify messaging for granted/denied states.
- Trigger data refresh and observe populated and empty usage list presentations in the embedded Insights screen.

---

These scenarios provide broad coverage of TALauncher’s critical user journeys and edge cases. Each test should assert on both visual state and underlying behaviour (view-model state, intents, and permissions) to ensure regressions are caught early.
