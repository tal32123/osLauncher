@echo off
setlocal ENABLEEXTENSIONS ENABLEDELAYEDEXPANSION

echo ========================================
echo Running targeted Espresso UI flows
echo ========================================

set CLASSES=com.talauncher.OnboardingGatingFlowTest com.talauncher.LauncherPagerNavigationTest com.talauncher.HomeNavigationFlowTest com.talauncher.AppDrawerFlowTest com.talauncher.OverlayPermissionFlowTest com.talauncher.SettingsIntegrationTest com.talauncher.LauncherUITest com.talauncher.AppDrawerIntegrationTest com.talauncher.LauncherNavigationIntegrationTest com.talauncher.UserFlowIntegrationTest com.talauncher.ComprehensiveTestSuite com.talauncher.APICompatibilityTest com.talauncher.PerformanceStressTest com.talauncher.StressAndPerformanceTest

set FAILURES=0

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
    exit /b 1
)

echo ========================================
echo All Espresso flows and full test suite passed!
echo ========================================
exit /b 0
