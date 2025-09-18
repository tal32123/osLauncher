@echo off
echo ======================================
echo Math Challenge Overlay E2E Tests
echo ======================================

echo.
echo [1/6] Building test APK...
call gradlew assembleDebugAndroidTest
if %errorlevel% neq 0 (
    echo ERROR: Test build failed
    pause
    exit /b 1
)

echo.
echo [2/6] Building main APK...
call gradlew assembleDebug
if %errorlevel% neq 0 (
    echo ERROR: Main build failed
    pause
    exit /b 1
)

echo.
echo [3/6] Checking device connection...
adb devices
echo.
echo Make sure your device/emulator is connected and ready.
echo Press any key to continue...
pause > nul

echo.
echo [4/6] Installing APKs...
adb install -r app\build\outputs\apk\debug\app-debug.apk
if %errorlevel% neq 0 (
    echo ERROR: Main APK installation failed
    pause
    exit /b 1
)

adb install -r app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
if %errorlevel% neq 0 (
    echo ERROR: Test APK installation failed
    pause
    exit /b 1
)

echo.
echo [5/6] Granting permissions...
echo Granting overlay permission...
adb shell appops set com.talauncher SYSTEM_ALERT_WINDOW allow

echo Granting notification permission...
adb shell pm grant com.talauncher android.permission.POST_NOTIFICATIONS

echo.
echo [6/6] Running Overlay Service Tests...
echo Running direct overlay service tests first...
adb shell am instrument -w -e class com.talauncher.OverlayServiceTest com.talauncher.test/androidx.test.runner.AndroidJUnitRunner

echo.
echo Test Results Summary:
echo ===================
echo The above tests verify that:
echo 1. Math challenge overlay can be displayed
echo 2. Countdown overlay can be displayed
echo 3. Decision overlay can be displayed
echo 4. Overlays are properly visible over other apps
echo.

echo.
echo Optional: Running Full E2E Test (requires manual interaction)...
echo This test will guide through the full user flow including settings configuration
echo Press Y to run full E2E test, or any other key to skip...
set /p choice=
if /i "%choice%"=="y" (
    echo Running full E2E test...
    adb shell am instrument -w -e class com.talauncher.MathChallengeOverlayE2ETest com.talauncher.test/androidx.test.runner.AndroidJUnitRunner
)

echo.
echo Testing complete!
echo.
echo To manually verify overlay functionality:
echo 1. Open the launcher app
echo 2. Go to Settings and enable Math Challenge
echo 3. Add YouTube as distracting app with 1-minute timer
echo 4. Launch YouTube and wait for overlay to appear
echo 5. Verify overlay blocks interaction with YouTube
echo 6. Test math challenge solving
echo.
pause