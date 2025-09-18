@echo off
echo ========================================
echo Running All Tests for TALauncher
echo ========================================

echo.
echo [1/3] Running Unit Tests...
echo ========================================
call gradlew test --info

echo.
echo [2/3] Running Android Instrumentation Tests...
echo ========================================
call gradlew connectedAndroidTest --info

echo.
echo [3/3] Generating Test Reports...
echo ========================================
call gradlew jacocoTestReport

echo.
echo ========================================
echo Test Execution Complete!
echo ========================================
echo.
echo Test reports available at:
echo - Unit Tests: app\build\reports\tests\testDebugUnitTest\index.html
echo - Android Tests: app\build\reports\androidTests\connected\index.html
echo - Coverage: app\build\reports\jacoco\jacocoTestReport\html\index.html
echo.