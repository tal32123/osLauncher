@echo off
setlocal enableextensions enabledelayedexpansion

echo ========================================
echo Running TALauncher Test Suite
echo ========================================

rem Check for quiet mode parameter (UI mode is now default)
set "UI_MODE=--info"
if /i "%~1"=="--quiet" set "UI_MODE="
if /i "%~1"=="-quiet" set "UI_MODE="
if /i "%~1"=="quiet" set "UI_MODE="
if /i "%~1"=="-q" set "UI_MODE="

if defined UI_MODE (
    echo [UI MODE] Running with detailed output enabled ^(default^)
) else (
    echo [QUIET MODE] Running in quiet mode ^(use --quiet to disable detailed output^)
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

echo ========================================
echo Running Unit Tests (matching CI)
echo ========================================
echo Running unit tests with gradlew test...
if defined UI_MODE (
    call .\gradlew.bat test --no-daemon --stacktrace --info
) else (
    call .\gradlew.bat test --no-daemon --stacktrace
)
if errorlevel 1 (
    echo !!! Unit test failures detected
    set /a FAILURES+=1
    set "EXIT_CODE=1"
)
echo Unit tests completed.
echo.

echo ========================================
echo Running Android Instrumentation Tests (matching CI)
echo ========================================
echo Running instrumentation tests with gradlew connectedAndroidTest...
if defined UI_MODE (
    call .\gradlew.bat connectedAndroidTest --no-daemon --stacktrace --info
) else (
    call .\gradlew.bat connectedAndroidTest --no-daemon --stacktrace
)
if errorlevel 1 (
    echo !!! Instrumentation test failures detected
    set /a FAILURES+=1
    set "EXIT_CODE=1"
)

echo.
if !FAILURES! neq 0 (
    echo ========================================
    echo Test execution completed with !FAILURES! failure^(s^)
    echo ========================================
    set "EXIT_CODE=1"
) else (
    echo ========================================
    echo All unit tests and instrumentation tests passed!
    echo ========================================
    set "EXIT_CODE=0"
)

:WAIT_FOR_INPUT
echo.
echo Press any key to close this window after reviewing the logs.
pause >nul
exit /b %EXIT_CODE%