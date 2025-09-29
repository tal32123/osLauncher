package com.talauncher

import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Comprehensive unit test suite that runs all unit tests
 * Provides a single entry point for running all unit tests
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    // ViewModel tests
    MainViewModelTest::class,
    HomeViewModelTest::class,
    AppDrawerViewModelTest::class,
    OnboardingViewModelTest::class,

    // Repository tests
    SessionRepositoryTest::class,
    SettingsRepositoryTest::class,
    AppRepositoryTest::class,

    // Utility tests
    PermissionsHelperTest::class,
    UsageStatsHelperTest::class,

    // Component tests

    // Helper tests
    AppSectioningHelperTest::class,
    EnhancedSearchServiceTest::class,

    // Edge case and error handling tests
    EdgeCaseAndErrorHandlingTest::class
)
class UnitTestSuite {
    // This class serves as a unit test suite runner
    // All unit tests are specified in the @Suite.SuiteClasses annotation
}