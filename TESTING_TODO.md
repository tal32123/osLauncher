# Testing TODO - Future Test Coverage

This document outlines test coverage that should be implemented in the future. These tests existed but have compilation issues due to API changes and need to be updated.

## Priority Tests (Currently Working)
✅ **LauncherPagerNavigationTest.kt** - From commit e005739 (Your priority)
✅ **OnboardingGatingFlowTest.kt** - From commit e005739 (Your priority)

## Tests That Should Be Fixed/Reimplemented

### Core Functionality Tests
1. **APICompatibilityTest.kt** - Tests API compatibility across Android versions
   - Usage stats permissions
   - Overlay permissions
   - BuildConfig access
   - **Issues**: Constructor mismatches, missing references
   - **Priority**: High (ensures app works across Android versions)

2. **LauncherUITest.kt** - Tests main launcher UI interactions
   - Volume button handling
   - UI component interactions
   - **Issues**: Missing volume button methods
   - **Priority**: High (core launcher functionality)

### Navigation & Flow Tests
3. **HomeNavigationFlowTest.kt** - Tests home screen navigation flows
   - Swipe navigation consistency
   - Home button navigation
   - Google search interaction
   - Back button behavior
   - **Issues**: Likely minor import/API issues
   - **Priority**: Medium (important user experience)

4. **LauncherNavigationIntegrationTest.kt** - Tests launcher navigation integration
   - **Issues**: Need to analyze specific compilation errors
   - **Priority**: Medium

### App Drawer Tests
5. **AppDrawerIntegrationTest.kt** - Tests app drawer functionality
   - **Issues**: Parameter count mismatch in assertCountEquals
   - **Priority**: Medium (important feature)

6. **AppDrawerFlowTest.kt** - Tests app drawer user flows
   - **Priority**: Medium

### User Experience Tests
7. **UserFlowIntegrationTest.kt** - Comprehensive user flow testing
   - First time user flow
   - App discovery and launch
   - Settings configuration
   - Time limit flow
   - Search flow
   - Multitasking flow
   - Rotation handling
   - Error recovery
   - Permission handling
   - **Priority**: High (covers entire user experience)

8. **OverlayPermissionFlowTest.kt** - Tests overlay permission flows
   - **Priority**: Medium

### Settings Tests
9. **SettingsIntegrationTest.kt** - Tests settings functionality
   - **Issues**: Break/continue syntax error
   - **Priority**: Medium

### Performance Tests
10. **StressAndPerformanceTest.kt** - Performance testing
    - **Priority**: Low (nice to have)

11. **PerformanceStressTest.kt** - Stress testing
    - **Issues**: Extensive - repository method mismatches, coroutine issues, model parameter problems
    - **Priority**: Low (many issues to fix)

## Deleted Test Files (No longer relevant)
These were removed in commit 1ccc5bd during major cleanup:
- OverlayServiceTest.kt (167 lines)
- ComprehensiveOverlayE2ETest.kt (691 lines)
- MathChallengeOverlayE2ETest.kt (502 lines)
- TimeLimitOverlayBasicFlowTest.kt (357 lines)
- OverlayInteractionBlockingTest.kt (403 lines)
- TimeLimitExpiryRealWorldTest.kt (501 lines)

## Recommended Implementation Order
1. **Fix priority tests first** (LauncherPagerNavigationTest, OnboardingGatingFlowTest)
2. **APICompatibilityTest** - Critical for multi-Android support
3. **UserFlowIntegrationTest** - Comprehensive coverage
4. **LauncherUITest** - Core functionality
5. **Navigation tests** - User experience
6. **App drawer tests** - Feature completeness
7. **Performance tests** - Optimization

## Notes
- Many compilation errors are due to API changes in recent refactoring
- Some tests reference methods that no longer exist or have changed signatures
- Missing imports for test frameworks and assertions
- Model parameter mismatches (e.g., isPinned, pinnedOrder fields)
- Repository method name changes (e.g., getAllSessionsSync no longer exists)