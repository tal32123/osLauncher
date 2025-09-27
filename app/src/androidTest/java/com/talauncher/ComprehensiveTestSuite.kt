package com.talauncher

import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Comprehensive test suite that runs all Android instrumentation tests
 * Provides a single entry point for running all UI and integration tests
 *
 * This suite includes:
 * - LauncherPagerNavigationTest: Tests navigation between pages and back press handling
 * - OnboardingGatingFlowTest: Tests the complete onboarding permission flow
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    // Priority tests from commit e005739
    LauncherPagerNavigationTest::class,
    OnboardingGatingFlowTest::class
)
class ComprehensiveTestSuite {
    // This class serves as a test suite runner
    // All tests are specified in the @Suite.SuiteClasses annotation
}