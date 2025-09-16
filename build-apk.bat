@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"
title TALauncher APK Builder

REM Set default timeout for commands to avoid hanging
set GRADLE_OPTS=-Dorg.gradle.daemon=false

echo.
echo 🚀 TALauncher APK Builder
echo ========================
echo.

REM Check if gradlew.bat exists
if not exist "%~dp0gradlew.bat" (
    echo ❌ Error: gradlew.bat not found in current directory
    echo Make sure you're running this from the project root directory
    pause
    exit /b 1
)

REM Get build type from argument or prompt user
set BUILD_TYPE=%1
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

echo.
echo 🧹 Cleaning the project...
call "%~dp0gradlew.bat" clean --continue
if %errorlevel% neq 0 (
    echo ⚠️  Warning: Project cleaning failed. Attempting to continue with build...
    echo 🔄 Trying to kill any lingering Gradle daemons...
    call "%~dp0gradlew.bat" --stop
    ping 127.0.0.1 -n 3 >nul
)
echo.

echo.
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

:end
echo.
REM Only pause if not running with arguments (interactive mode)
if "%1"=="" pause