@echo off
setlocal enableextensions enabledelayedexpansion

echo ========================================
echo Running targeted Espresso UI flows
echo ========================================

rem Check for UI mode parameter
set "UI_MODE="
if /i "%~1"=="--ui" set "UI_MODE=--info"
if /i "%~1"=="-ui" set "UI_MODE=--info"
if /i "%~1"=="ui" set "UI_MODE=--info"

if defined UI_MODE (
    echo [UI MODE] Running with detailed output enabled
) else (
    echo [HEADLESS MODE] Running in quiet mode ^(use --ui for detailed output^)
)

echo.
echo Checking for connected Android devices or emulators...

set "FAILURES=0"
set "EXIT_CODE=0"

rem Check if adb is available
where adb >nul 2>&1
if errorlevel 1 (
    echo !!! The adb tool is not available on PATH.
    echo !!! Ensure Android SDK platform-tools are installed and adb is accessible, then rerun this script.
    set "EXIT_CODE=1"
    goto WAIT_FOR_INPUT
)

rem Check for connected devices
set "DEVICE_FOUND="
for /f "skip=1 tokens=1" %%D in ('adb devices 2^>nul') do (
    if not "%%D"=="" (
        set "DEVICE_FOUND=1"
        goto device_check_complete
    )
)
:device_check_complete
if not defined DEVICE_FOUND (
    echo !!! No connected devices or emulators detected.
    echo !!! Start an emulator ^(e.g. Medium_Phone_API_36.0^) or plug in a device and run again.
    set "EXIT_CODE=1"
    goto WAIT_FOR_INPUT
)

rem Run individual test classes
call :run_test_class "com.talauncher.OnboardingGatingFlowTest"
call :run_test_class "com.talauncher.LauncherPagerNavigationTest"
call :run_test_class "com.talauncher.ComprehensiveTestSuite"

echo ========================================
echo Running complete unit and instrumentation suites
echo ========================================
if defined UI_MODE (
    call .\gradlew.bat test connectedAndroidTest --info
) else (
    call .\gradlew.bat test connectedAndroidTest --quiet
)
if errorlevel 1 (
    echo !!! Failure detected while running the full test suite
    set /a FAILURES+=1
)

echo.
if !FAILURES! neq 0 (
    echo ========================================
    echo Test execution completed with !FAILURES! failure^(s^)
    echo ========================================
    set "EXIT_CODE=1"
) else (
    echo ========================================
    echo All Espresso flows and full test suite passed!
    echo ========================================
    set "EXIT_CODE=0"
)

goto WAIT_FOR_INPUT

:run_test_class
if not "%EXIT_CODE%"=="0" goto :eof
set "CLASS=%~1"
if "%CLASS%"=="" goto :eof

echo ----------------------------------------
echo Running Espresso flow: %CLASS%
echo ----------------------------------------
if defined UI_MODE (
    call .\gradlew.bat connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=%CLASS% --info
) else (
    call .\gradlew.bat connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=%CLASS% --quiet
)
if errorlevel 1 (
    echo !!! Failure detected in %CLASS%
    set /a FAILURES+=1
)
echo.
goto :eof

:WAIT_FOR_INPUT
echo.
echo Press any key to close this window after reviewing the logs.
pause >nul
exit /b %EXIT_CODE%