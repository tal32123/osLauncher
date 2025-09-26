package com.talauncher

import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Comprehensive test suite that runs all Android instrumentation tests
 * Provides a single entry point for running all UI and integration tests
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    // Navigation and basic functionality
    LauncherNavigationIntegrationTest::class,
    LauncherUITest::class,
    HomeNavigationFlowTest::class,
    LauncherPagerNavigationTest::class,
    OnboardingGatingFlowTest::class,

    // App drawer functionality
    AppDrawerIntegrationTest::class,
    AppDrawerFlowTest::class,

    // API compatibility tests
    APICompatibilityTest::class,

    // Overlay permission and flow tests
    OverlayPermissionFlowTest::class,

    // Settings and integration tests
    SettingsIntegrationTest::class,
    UserFlowIntegrationTest::class,

    // Stress and performance tests
    StressAndPerformanceTest::class,
    PerformanceStressTest::class
)
class ComprehensiveTestSuite {
    // This class serves as a test suite runner
    // All tests are specified in the @Suite.SuiteClasses annotation
}