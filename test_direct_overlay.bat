@echo off
echo Testing Direct Overlay Implementation
echo =====================================

echo.
echo Step 1: Starting TALauncher...
adb shell am start com.talauncher/.MainActivity
sleep 3

echo.
echo Step 2: Launching Calculator (target app)...
adb shell am start -n com.android.calculator2/com.android.calculator2.Calculator
sleep 2

echo.
echo Step 3: Using adb to test BackgroundOverlayManager directly...
adb shell am start-service com.talauncher/.service.OverlayService -a com.talauncher.service.OverlayService.ACTION_SHOW_MATH_CHALLENGE --es com.talauncher.service.OverlayService.EXTRA_APP_NAME "Calculator" --es com.talauncher.service.OverlayService.EXTRA_PACKAGE_NAME "com.android.calculator2" --es com.talauncher.service.OverlayService.EXTRA_DIFFICULTY "easy"

echo.
echo Overlay should now appear over Calculator!
echo Check the screen and press any key to continue...
pause

echo.
echo Returning to launcher...
adb shell am start com.talauncher/.MainActivity