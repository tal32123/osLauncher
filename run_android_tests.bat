@echo off
echo ========================================
echo Running Android Instrumentation Tests
echo ========================================

echo.
echo Checking connected devices...
call adb devices

echo.
echo Running all Android tests...
call gradlew connectedAndroidTest --info

echo.
echo Running comprehensive test suite...
call gradlew connectedAndroidTest --tests "com.talauncher.ComprehensiveTestSuite" --info

echo.
echo ========================================
echo Android Test Execution Complete!
echo ========================================
echo.
echo Test report available at: app\build\reports\androidTests\connected\index.html
echo.