# TALauncher Testing Guide

## Overview

This document outlines the comprehensive testing strategy for TALauncher, including unit tests, integration tests, UI tests, and performance tests. The testing suite covers basic functionality, edge cases, error handling, and API compatibility across different Android versions.

## Test Structure

### Unit Tests (`app/src/test/`)

**Core Component Tests:**
- `MainViewModelTest.kt` - Tests main app initialization and onboarding flow
- `HomeViewModelTest.kt` - Tests home screen functionality and app launching
- `AppDrawerViewModelTest.kt` - Tests app filtering, searching, and launching
- `OnboardingViewModelTest.kt` - Tests permission requests and setup flow

**Repository Tests:**
- `SessionRepositoryTest.kt` - Tests session management and time tracking
- `SettingsRepositoryTest.kt` - Tests settings persistence and retrieval
- `AppRepositoryTest.kt` - Tests app data management

**Utility Tests:**
- `PermissionsHelperTest.kt` - Tests permission handling utilities
- `UsageStatsHelperTest.kt` - Tests usage statistics functionality

**Edge Case Tests:**
- `EdgeCaseAndErrorHandlingTest.kt` - Tests error scenarios, null handling, and boundary conditions

### Integration Tests (`app/src/androidTest/`)

**Navigation and UI Flow Tests:**
- `LauncherNavigationIntegrationTest.kt` - Tests page navigation and back button handling
- `AppDrawerIntegrationTest.kt` - Tests app search, filtering, and launch functionality
- `LauncherUITest.kt` - Tests basic UI interactions using Espresso

**API Compatibility Tests:**
- `APICompatibilityTest.kt` - Tests compatibility across Android API levels 24-35

**Stress and Performance Tests:**
- `StressAndPerformanceTest.kt` - Tests app behavior under heavy load and rapid interactions

**Existing Overlay Tests:**
- Various overlay service and session management tests

## Test Categories

### 1. Basic Functionality Tests
- App launch and initialization
- Navigation between screens
- App search and filtering
- Settings management
- Permission handling

### 2. User Flow Tests
- Onboarding completion flow
- App launching from home and drawer
- Search and clear operations
- Navigation gestures and button interactions

### 3. Edge Case Tests
- Null input handling
- Invalid data scenarios
- Memory pressure situations
- Corrupted data handling
- Network timeout scenarios

### 4. API Level Compatibility Tests
- Android 7.0+ (API 24) - Usage stats permission
- Android 6.0+ (API 23) - Overlay permission
- Android 13+ (API 33) - Notification permission
- Android 11+ (API 30) - Package visibility
- Android 8.0+ (API 26) - Foreground services
- Android 14+ (API 34) - New foreground service types

### 5. Performance Tests
- Rapid navigation stress testing
- Memory pressure handling
- Concurrent UI interactions
- Long-running operations
- Device rotation handling

## Running Tests

### Run All Tests
```bash
run_all_tests.bat
```

### Run Unit Tests Only
```bash
run_unit_tests.bat
```

### Run Android Instrumentation Tests Only
```bash
run_android_tests.bat
```

### Run Specific Test Suites

**Unit Test Suite:**
```bash
gradlew test --tests "com.talauncher.UnitTestSuite"
```

**Android Test Suite:**
```bash
gradlew connectedAndroidTest --tests "com.talauncher.ComprehensiveTestSuite"
```

## Test Environment Setup

### Prerequisites
1. Android device or emulator with API level 24+
2. USB debugging enabled
3. All required permissions granted for testing

### Device Preparation
1. Install the app on the test device
2. Grant necessary permissions:
   - Usage access permission
   - Display over other apps permission
   - Notification permission (API 33+)
3. Set TALauncher as default launcher (for comprehensive testing)

## Test Coverage Areas

### Core Features
- ✅ App launching and management
- ✅ Navigation between screens
- ✅ Search functionality
- ✅ Settings management
- ✅ Session tracking
- ✅ Overlay service functionality

### Error Handling
- ✅ Database connection failures
- ✅ Network timeouts
- ✅ Permission denied scenarios
- ✅ Corrupted data handling
- ✅ Memory pressure situations

### Performance
- ✅ Rapid user interactions
- ✅ Memory usage optimization
- ✅ UI responsiveness
- ✅ Background/foreground cycling
- ✅ Device rotation handling

### Compatibility
- ✅ Android API 24-35 compatibility
- ✅ Different screen sizes and orientations
- ✅ Various Android manufacturers
- ✅ Different permission models

## Best Practices

### Test Design
1. **Isolation**: Each test is independent and can run in any order
2. **Mocking**: External dependencies are properly mocked
3. **Assertions**: Clear and meaningful assertions
4. **Cleanup**: Proper cleanup after each test

### Test Data
1. **Realistic Data**: Use realistic test data that mirrors production scenarios
2. **Edge Cases**: Include boundary values and edge cases
3. **Invalid Data**: Test with malformed and invalid inputs

### Maintenance
1. **Regular Updates**: Keep tests updated with code changes
2. **Refactoring**: Refactor tests when underlying code changes
3. **Documentation**: Keep test documentation up to date

## Continuous Integration

The test suite is designed to work with CI/CD pipelines:

```yaml
# Example CI configuration
- name: Run Unit Tests
  run: ./gradlew test

- name: Run Android Tests
  run: ./gradlew connectedAndroidTest

- name: Generate Coverage Report
  run: ./gradlew jacocoTestReport
```

## Test Reports

After running tests, reports are available at:
- **Unit Tests**: `app/build/reports/tests/testDebugUnitTest/index.html`
- **Android Tests**: `app/build/reports/androidTests/connected/index.html`
- **Coverage**: `app/build/reports/jacoco/jacocoTestReport/html/index.html`

## Troubleshooting

### Common Issues

**Tests fail with permission errors:**
- Ensure all required permissions are granted
- Check that overlay permission is enabled
- Verify usage access permission is granted

**UI tests fail intermittently:**
- Increase wait times for slow devices
- Check device orientation and state
- Ensure device is not in power saving mode

**Memory-related test failures:**
- Run tests on devices with sufficient RAM
- Close other apps before testing
- Consider reducing test data size for low-end devices

## Future Enhancements

### Planned Test Additions
1. **Accessibility Tests**: Verify compliance with accessibility standards
2. **Localization Tests**: Test app behavior with different languages
3. **Security Tests**: Validate security measures and data protection
4. **Battery Optimization Tests**: Test impact on device battery life

### Test Automation Improvements
1. **Visual Regression Testing**: Automated screenshot comparison
2. **Performance Monitoring**: Automated performance benchmarking
3. **Cross-Device Testing**: Automated testing across device matrix