@echo off
echo Testing Background Overlay Implementation
echo ========================================

echo.
echo Step 1: Launching Settings app...
adb shell am start -a android.settings.SETTINGS
timeout /t 3 >nul

echo.
echo Step 2: Checking overlay permission...
adb shell appops get com.talauncher SYSTEM_ALERT_WINDOW

echo.
echo Step 3: Sending math challenge broadcast to show overlay over Settings...
adb shell am broadcast -a com.talauncher.SESSION_EXPIRY_MATH_CHALLENGE --es package_name com.android.settings

echo.
echo Step 4: Waiting 5 seconds to observe overlay...
timeout /t 5 >nul

echo.
echo Step 5: Checking logcat for overlay manager activity...
adb logcat -d | grep -E "(BackgroundOverlayManager|OverlayService|HomeViewModel)" | tail -20

echo.
echo Test complete! Check if overlay appeared over Settings app.
echo Press any key to return to launcher...
pause >nul
adb shell am start com.talauncher/.MainActivity