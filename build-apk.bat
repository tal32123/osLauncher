@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"
title TALauncher APK Builder

REM Set default timeout for commands to avoid hanging
set GRADLE_OPTS=-Dorg.gradle.daemon=false

echo.
echo 🚀 TALauncher APK Builder with Testing
echo ======================================
echo.
echo Usage: build-apk.bat [build_type] [--skip-tests]
echo   build_type: debug, release, both (optional, will prompt if not provided)
echo   --skip-tests: Skip running tests before building (optional)
echo.
echo Examples:
echo   build-apk.bat debug              - Build debug APK with tests
echo   build-apk.bat release --skip-tests - Build release APK without tests
echo   build-apk.bat --skip-tests both    - Build both APKs without tests
echo.

REM Check if gradlew.bat exists
if not exist "%~dp0gradlew.bat" (
    echo ❌ Error: gradlew.bat not found in current directory
    echo Make sure you're running this from the project root directory
    pause
    exit /b 1
)

REM Parse command line arguments
set BUILD_TYPE=%1
set SKIP_TESTS=%2

REM Check for --skip-tests flag in any position
if /i "%1"=="--skip-tests" (
    set SKIP_TESTS=true
    set BUILD_TYPE=%2
)
if /i "%2"=="--skip-tests" set SKIP_TESTS=true
if /i "%3"=="--skip-tests" set SKIP_TESTS=true

REM Get build type from argument or prompt user
if "%BUILD_TYPE%"=="" (
    echo Select build type:
    echo 1. Debug APK (for testing, larger file size)
    echo 2. Release APK (optimized, smaller file size)
    echo 3. Both Debug and Release
    echo.
    set /p choice="Enter your choice (1-3): "

    if "!choice!"=="1" set BUILD_TYPE=debug
    if "!choice!"=="2" set BUILD_TYPE=release
    if "!choice!"=="3" set BUILD_TYPE=both

    if "%BUILD_TYPE%"=="" (
        echo Invalid choice. Using debug build.
        set BUILD_TYPE=debug
    )
)

REM Display current configuration
echo Configuration:
echo - Build Type: %BUILD_TYPE%
if "%SKIP_TESTS%"=="true" (
    echo - Tests: SKIPPED (--skip-tests flag detected)
) else (
    echo - Tests: ENABLED (will run before build)
)
echo.

REM Run tests unless explicitly skipped
if not "%SKIP_TESTS%"=="true" (
    echo 🧪 Running tests before building APK...
    echo =====================================
    call :run_tests
    if !ERRORLEVEL! neq 0 (
        echo.
        echo ❌ Tests failed! APK build cancelled.
        echo   Use --skip-tests flag to bypass testing if needed.
        goto build_failed
    )
    echo ✅ All tests passed! Proceeding with APK build...
    echo.
) else (
    echo 🧹 Cleaning the project...
    call "%~dp0gradlew.bat" clean --continue
    if %errorlevel% neq 0 (
        echo ⚠️  Warning: Project cleaning failed. Attempting to continue with build...
        echo 🔄 Trying to kill any lingering Gradle daemons...
        call "%~dp0gradlew.bat" --stop
        ping 127.0.0.1 -n 3 >nul
    )
    echo.
)

echo 🎯 Building %BUILD_TYPE% APK(s)...
echo.

REM Build based on type
if "%BUILD_TYPE%"=="debug" goto build_debug
if "%BUILD_TYPE%"=="release" goto build_release
if "%BUILD_TYPE%"=="both" goto build_both

:build_debug
echo 📦 Building Debug APK...
call "%~dp0gradlew.bat" assembleDebug
if %errorlevel% neq 0 goto build_failed
goto check_files

:build_release
echo 📦 Building Release APK...
call "%~dp0gradlew.bat" assembleRelease
if %errorlevel% neq 0 goto build_failed
goto check_files

:build_both
echo 📦 Building Debug APK...
call "%~dp0gradlew.bat" assembleDebug
if %errorlevel% neq 0 goto build_failed

echo.
echo 📦 Building Release APK...
call "%~dp0gradlew.bat" assembleRelease
if %errorlevel% neq 0 goto build_failed
goto check_files

:check_files
echo.
echo 🔍 Build Results:
echo ================

REM Check debug APK
if exist "app\build\outputs\apk\debug\app-debug.apk" (
    for %%I in ("app\build\outputs\apk\debug\app-debug.apk") do (
        set size=%%~zI
        set /a sizeMB=!size!/1024/1024
        echo ✅ Debug APK: !sizeMB! MB
        echo    📁 Location: app\build\outputs\apk\debug\app-debug.apk
    )
) else (
    if "%BUILD_TYPE%"=="debug" echo ❌ Debug APK: Not found
    if "%BUILD_TYPE%"=="both" echo ❌ Debug APK: Not found
)

REM Check release APK
if exist "app\build\outputs\apk\release\app-release-unsigned.apk" (
    for %%I in ("app\build\outputs\apk\release\app-release-unsigned.apk") do (
        set size=%%~zI
        set /a sizeMB=!size!/1024/1024
        echo ✅ Release APK: !sizeMB! MB (unsigned)
        echo    📁 Location: app\build\outputs\apk\release\app-release-unsigned.apk
    )
) else (
    if "%BUILD_TYPE%"=="release" echo ❌ Release APK: Not found
    if "%BUILD_TYPE%"=="both" echo ❌ Release APK: Not found
)

echo.
echo 🎉 Build completed successfully!
echo 📱 You can now install the APK on your Android device.
echo 💡 Tip: Use "adb install path\to\apk" to install via ADB
goto end

:build_failed
echo.
echo 💥 Build failed! Check the errors above.
echo.
REM Only pause if not running with arguments (interactive mode)
if "%1"=="" pause
exit /b 1

:run_tests
echo Checking for connected Android devices or emulators...
set "TEST_FAILURES=0"
set "TEST_EXIT_CODE=0"

REM Check if adb is available
where adb >nul 2>&1
if errorlevel 1 (
    echo !!! The adb tool is not available on PATH.
    echo !!! Ensure Android SDK platform-tools are installed and adb is accessible.
    set "TEST_EXIT_CODE=1"
    goto :eof
)

REM Check for connected devices
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
    echo !!! Start an emulator or plug in a device and run again.
    echo !!! Or use --skip-tests flag to bypass testing.
    set "TEST_EXIT_CODE=1"
    goto :eof
)

echo Device found! Running comprehensive test suite...
echo.

REM Validate test compilation first
echo Validating test code compilation...
call "%~dp0gradlew.bat" :uitests:compileDebugAndroidTestKotlin --quiet
if errorlevel 1 (
    echo !!! Test compilation failed! Cannot proceed with testing.
    echo !!! This usually indicates import errors, syntax issues, or missing dependencies.
    set "TEST_EXIT_CODE=1"
    goto :eof
)
echo Test compilation successful. Proceeding with tests...
echo.

REM Run individual test classes first for better debugging
echo Running targeted Espresso UI flows...
call :run_test_class "com.talauncher.OnboardingGatingFlowTest"
if !TEST_EXIT_CODE! neq 0 goto :eof

call :run_test_class "com.talauncher.LauncherPagerNavigationTest"
if !TEST_EXIT_CODE! neq 0 goto :eof

call :run_test_class "com.talauncher.ComprehensiveTestSuite"
if !TEST_EXIT_CODE! neq 0 goto :eof

echo Individual Espresso tests completed successfully!
echo.

REM Run full test suite (reuses already compiled code)
echo Running complete unit and instrumentation suites...
call "%~dp0gradlew.bat" test :uitests:connectedAndroidTest --quiet
if errorlevel 1 (
    echo !!! Failure detected in full test suite
    set /a TEST_FAILURES+=1
    set "TEST_EXIT_CODE=1"
)

if !TEST_FAILURES! neq 0 (
    echo Test execution completed with !TEST_FAILURES! failure^(s^)
    set "TEST_EXIT_CODE=1"
) else (
    echo All tests passed successfully!
    set "TEST_EXIT_CODE=0"
)

exit /b !TEST_EXIT_CODE!

:run_test_class
if not "%TEST_EXIT_CODE%"=="0" goto :eof
set "CLASS=%~1"
if "%CLASS%"=="" goto :eof

echo Running: %CLASS%
call "%~dp0gradlew.bat" :uitests:connectedAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=%CLASS%" --quiet
if errorlevel 1 (
    echo !!! FAILURE in %CLASS%
    set /a TEST_FAILURES+=1
    set "TEST_EXIT_CODE=1"
) else (
    echo SUCCESS: %CLASS%
)
echo.
goto :eof

:end
echo.
REM Only pause if not running with arguments (interactive mode)
if "%1"=="" pause