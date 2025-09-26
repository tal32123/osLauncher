@echo off
setlocal ENABLEEXTENSIONS ENABLEDELAYEDEXPANSION


echo ========================================
echo Running targeted Espresso UI flows
echo ========================================

set CLASSES=com.talauncher.OnboardingGatingFlowTest com.talauncher.LauncherPagerNavigationTest com.talauncher.ComprehensiveTestSuite

set FAILURES=0
set EXIT_CODE=0

echo.
echo Checking for connected Android devices or emulators...

where adb >nul 2>&1
if errorlevel 1 (
    echo !!! The adb tool is not available on PATH.
    echo !!! Ensure Android SDK platform-tools are installed and adb is accessible, then rerun this script.
    set EXIT_CODE=1
    goto WAIT_FOR_INPUT
)

set DEVICE_FOUND=
for /f "skip=1 tokens=1" %%D in ('adb devices') do (
    if NOT "%%D"=="" (
        set DEVICE_FOUND=1
        goto DEVICE_CHECK_COMPLETE
    )
)
:DEVICE_CHECK_COMPLETE
if not defined DEVICE_FOUND (
    echo !!! No connected devices or emulators detected.
    echo !!! Start an emulator (e.g. Medium_Phone_API_36.0) or plug in a device and run again.
    set EXIT_CODE=1
    goto WAIT_FOR_INPUT
)

for %%C in (%CLASSES%) do (
    echo ----------------------------------------
    echo Running Espresso flow: %%C
    echo ----------------------------------------
    call .\gradlew.bat connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=%%C --info
    if errorlevel 1 (
        echo !!! Failure detected in %%C
        set /a FAILURES+=1
    )
    echo.
)

echo ========================================
echo Running complete unit and instrumentation suites
echo ========================================
call .\gradlew.bat test connectedAndroidTest --info
if errorlevel 1 (
    echo !!! Failure detected while running the full test suite
    set /a FAILURES+=1
)

echo.
if !FAILURES! NEQ 0 (
    echo ========================================
    echo Test execution completed with !FAILURES! failure^(s^)
    echo ========================================
    set EXIT_CODE=1
) else (
    echo ========================================
    echo All Espresso flows and full test suite passed!
    echo ========================================
    set EXIT_CODE=0
)

goto WAIT_FOR_INPUT

:WAIT_FOR_INPUT
echo.
echo Press any key to close this window after reviewing the logs.
pause >nul
exit /b !EXIT_CODE!
