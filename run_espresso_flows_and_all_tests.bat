@echo off
setlocal ENABLEEXTENSIONS ENABLEDELAYEDEXPANSION

echo ========================================
echo Running targeted Espresso UI flows
echo ========================================

echo.
echo Checking for connected Android devices or emulators...

set FAILURES=0
set EXIT_CODE=0

call :ensure_adb
if NOT "%EXIT_CODE%"=="0" goto WAIT_FOR_INPUT

call :ensure_device
if NOT "%EXIT_CODE%"=="0" goto WAIT_FOR_INPUT

call :run_test_class com.talauncher.OnboardingGatingFlowTest
call :run_test_class com.talauncher.LauncherPagerNavigationTest
call :run_test_class com.talauncher.ComprehensiveTestSuite

echo ========================================
echo Running complete unit and instrumentation suites
echo ========================================
call .\gradlew.bat test connectedAndroidTest --info
if errorlevel 1 (
    echo !!! Failure detected while running the full test suite
    set /a FAILURES+=1
)

echo.
if %FAILURES% NEQ 0 (
    echo ========================================
    echo Test execution completed with %FAILURES% failure(s)
    echo ========================================
    set EXIT_CODE=1
) else (
    echo ========================================
    echo All Espresso flows and full test suite passed!
    echo ========================================
    set EXIT_CODE=0
)

goto WAIT_FOR_INPUT

:ensure_adb
where adb >nul 2>&1
if errorlevel 1 (
    echo !!! The adb tool is not available on PATH.
    echo !!! Ensure Android SDK platform-tools are installed and adb is accessible, then rerun this script.
    set EXIT_CODE=1
)
exit /b

:ensure_device
set DEVICE_FOUND=
for /f "skip=1 tokens=1" %%D in ('adb devices') do (
    if NOT "%%D"=="" (
        set DEVICE_FOUND=1
        goto :device_check_complete
    )
)
:device_check_complete
if NOT defined DEVICE_FOUND (
    echo !!! No connected devices or emulators detected.
    echo !!! Start an emulator (e.g. Medium_Phone_API_36.0) or plug in a device and run again.
    set EXIT_CODE=1
)
exit /b

:run_test_class
if NOT "%EXIT_CODE%"=="0" exit /b
set CLASS=%~1
if "%CLASS%"=="" exit /b

echo ----------------------------------------
echo Running Espresso flow: %CLASS%
echo ----------------------------------------
call .\gradlew.bat connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=%CLASS% --info
if errorlevel 1 (
    echo !!! Failure detected in %CLASS%
    set /a FAILURES+=1
)
echo.
exit /b

:WAIT_FOR_INPUT
echo.
echo Press any key to close this window after reviewing the logs.
pause >nul
exit /b %EXIT_CODE%
