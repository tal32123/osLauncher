@echo off
echo ========================================
echo Running Unit Tests for TALauncher
echo ========================================

echo.
echo Running all unit tests...
call gradlew test --info

echo.
echo Running specific test suite...
call gradlew test --tests "com.talauncher.UnitTestSuite" --info

echo.
echo Generating test report...
call gradlew jacocoTestReport

echo.
echo ========================================
echo Unit Test Execution Complete!
echo ========================================
echo.
echo Test report available at: app\build\reports\tests\testDebugUnitTest\index.html
echo Coverage report available at: app\build\reports\jacoco\jacocoTestReport\html\index.html
echo.