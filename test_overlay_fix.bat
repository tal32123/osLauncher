@echo off
echo ===================================
echo Math Challenge Overlay Fix Testing
echo ===================================

echo.
echo [1/6] Cleaning previous build...
call gradlew clean
if %errorlevel% neq 0 (
    echo ERROR: Clean failed
    pause
    exit /b 1
)

echo.
echo [2/6] Building debug APK...
call gradlew assembleDebug
if %errorlevel% neq 0 (
    echo ERROR: Build failed
    pause
    exit /b 1
)

echo.
echo [3/6] Checking for connected devices...
adb devices
echo.
echo Make sure your device is connected and authorized.
echo Press any key to continue with installation...
pause > nul

echo.
echo [4/6] Installing APK...
adb install -r app\build\outputs\apk\debug\app-debug.apk
if %errorlevel% neq 0 (
    echo ERROR: Installation failed
    pause
    exit /b 1
)

echo.
echo [5/6] Setting up testing environment...
echo Granting overlay permission...
adb shell appops set com.talauncher SYSTEM_ALERT_WINDOW allow

echo Granting notification permission (Android 13+)...
adb shell pm grant com.talauncher android.permission.POST_NOTIFICATIONS

echo Starting app...
adb shell am start -n com.talauncher/.MainActivity

echo.
echo [6/6] Testing setup complete!
echo.
echo MANUAL TESTING STEPS:
echo 1. Complete app onboarding if first time
echo 2. Go to Settings and enable "Math Challenge" feature
echo 3. Add a distracting app (like Instagram/TikTok) with 1-minute timer
echo 4. Launch the distracting app from the launcher
echo 5. Wait for countdown overlay to appear over the distracting app
echo 6. When decision dialog appears, select "Solve a math challenge"
echo 7. Verify math challenge overlay appears and blocks app interaction
echo 8. Test both correct and incorrect answers
echo.
echo MONITORING LOGS:
echo Run this in another terminal to monitor logs:
echo adb logcat -s OverlayService HomeViewModel
echo.
echo TROUBLESHOOTING:
echo - Check overlay permission: adb shell appops get com.talauncher SYSTEM_ALERT_WINDOW
echo - Check running services: adb shell ps ^| findstr talauncher
echo - Force stop app: adb shell am force-stop com.talauncher
echo.
pause