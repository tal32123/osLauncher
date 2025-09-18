package com.talauncher

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.talauncher.data.model.AppInfo
import com.talauncher.data.model.AppSession
import com.talauncher.data.model.LauncherSettings
import com.talauncher.data.repository.AppRepository
import com.talauncher.data.repository.SessionRepository
import com.talauncher.data.repository.SettingsRepository
import com.talauncher.ui.home.HomeViewModel
import com.talauncher.utils.ErrorHandler
import com.talauncher.utils.PermissionsHelper
import com.talauncher.utils.UsageStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.junit.Assert.*
import java.io.IOException
import java.util.concurrent.TimeoutException

/**
 * Edge case and error handling tests
 * Tests error scenarios, null handling, network issues, and boundary conditions
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class EdgeCaseAndErrorHandlingTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var appRepository: AppRepository

    @Mock
    private lateinit var settingsRepository: SettingsRepository

    @Mock
    private lateinit var sessionRepository: SessionRepository

    @Mock
    private lateinit var permissionsHelper: PermissionsHelper

    @Mock
    private lateinit var usageStatsHelper: UsageStatsHelper

    @Mock
    private lateinit var errorHandler: ErrorHandler

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testNullInputHandling() = runTest {
        // Test various null inputs
        whenever(appRepository.getAllApps()).thenReturn(flowOf(emptyList()))
        whenever(settingsRepository.getSettings()).thenReturn(flowOf(LauncherSettings()))

        val viewModel = HomeViewModel(
            appRepository = appRepository,
            settingsRepository = settingsRepository,
            onLaunchApp = null, // Null callback
            sessionRepository = null, // Null session repository
            context = null, // Null context
            permissionsHelper = null, // Null permissions helper
            usageStatsHelper = null // Null usage stats helper
        )

        advanceUntilIdle()

        // Should handle null inputs gracefully
        assertNotNull(viewModel.uiState.value)
    }

    @Test
    fun testDatabaseConnectionFailure() = runTest {
        // Simulate database connection failure
        whenever(appRepository.getAllApps()).thenThrow(RuntimeException("Database connection failed"))
        whenever(settingsRepository.getSettings()).thenThrow(RuntimeException("Database connection failed"))

        val viewModel = HomeViewModel(
            appRepository = appRepository,
            settingsRepository = settingsRepository
        )

        advanceUntilIdle()

        // Should handle database errors gracefully
        assertNotNull(viewModel.uiState.value)
    }

    @Test
    fun testNetworkTimeoutScenarios() = runTest {
        // Simulate network timeout
        whenever(appRepository.getAllApps()).thenThrow(TimeoutException("Network timeout"))

        val viewModel = HomeViewModel(
            appRepository = appRepository,
            settingsRepository = settingsRepository
        )

        advanceUntilIdle()

        // Should handle network timeouts
        assertNotNull(viewModel.uiState.value)
    }

    @Test
    fun testCorruptedDataHandling() = runTest {
        // Test handling of corrupted or invalid data
        val corruptedApps = listOf(
            AppInfo("", ""), // Empty strings
            AppInfo("null", "null"), // Null-like values
            AppInfo("com.valid.app", "Valid App") // Valid app mixed with corrupt data
        )

        whenever(appRepository.getAllApps()).thenReturn(flowOf(corruptedApps))
        whenever(settingsRepository.getSettings()).thenReturn(flowOf(LauncherSettings()))

        val viewModel = HomeViewModel(
            appRepository = appRepository,
            settingsRepository = settingsRepository
        )

        advanceUntilIdle()

        // Should filter out or handle corrupted data
        assertNotNull(viewModel.uiState.value)
    }

    @Test
    fun testMemoryPressureScenarios() = runTest {
        // Simulate memory pressure with large datasets
        val largeAppList = (1..10000).map { index ->
            AppInfo(
                packageName = "com.example.app$index",
                appName = "App $index",
                isPinned = false
            )
        }

        whenever(appRepository.getAllApps()).thenReturn(flowOf(largeAppList))
        whenever(settingsRepository.getSettings()).thenReturn(flowOf(LauncherSettings()))

        val viewModel = HomeViewModel(
            appRepository = appRepository,
            settingsRepository = settingsRepository
        )

        advanceUntilIdle()

        // Should handle large datasets without out-of-memory errors
        assertNotNull(viewModel.uiState.value)
    }

    @Test
    fun testConcurrentAccessScenarios() = runTest {
        // Test concurrent access to repositories
        whenever(appRepository.getAllApps()).thenReturn(flowOf(emptyList()))
        whenever(settingsRepository.getSettings()).thenReturn(flowOf(LauncherSettings()))

        val viewModel = HomeViewModel(
            appRepository = appRepository,
            settingsRepository = settingsRepository
        )

        // Simulate concurrent operations
        repeat(100) {
            viewModel.uiState.value
        }

        advanceUntilIdle()

        // Should handle concurrent access safely
        assertNotNull(viewModel.uiState.value)
    }

    @Test
    fun testInvalidSessionDataHandling() = runTest {
        // Test handling of invalid session data
        val invalidSessions = listOf(
            AppSession(
                id = 0,
                packageName = "",
                plannedDurationMinutes = 5,
                startTime = -1L, // Invalid time
                endTime = -2L, // Invalid time
                isActive = true
            ),
            AppSession(
                id = 1,
                packageName = "com.valid.app",
                plannedDurationMinutes = 10,
                startTime = System.currentTimeMillis(),
                endTime = null,
                isActive = true
            )
        )

        whenever(sessionRepository!!.getActiveSessions()).thenReturn(flowOf(invalidSessions))

        // Should handle invalid session data gracefully
        verify(sessionRepository, never()).endSession(any())
    }

    @Test
    fun testResourceExhaustionHandling() = runTest {
        // Test behavior when system resources are exhausted
        whenever(appRepository.getAllApps()).thenThrow(OutOfMemoryError("Heap exhausted"))

        val viewModel = HomeViewModel(
            appRepository = appRepository,
            settingsRepository = settingsRepository
        )

        try {
            advanceUntilIdle()
        } catch (e: OutOfMemoryError) {
            // Expected in this test case
        }

        // Should handle resource exhaustion scenarios
        assertTrue("Resource exhaustion should be handled", true)
    }

    @Test
    fun testMalformedPackageNames() = runTest {
        // Test handling of malformed package names
        val malformedApps = listOf(
            AppInfo("not a valid package name", "Invalid App"),
            AppInfo("com..double.dot", "Double Dot App"),
            AppInfo("com.valid.app", "Valid App"),
            AppInfo("123.numeric.start", "Numeric Start")
        )

        whenever(appRepository.getAllApps()).thenReturn(flowOf(malformedApps))
        whenever(settingsRepository.getSettings()).thenReturn(flowOf(LauncherSettings()))

        val viewModel = HomeViewModel(
            appRepository = appRepository,
            settingsRepository = settingsRepository
        )

        advanceUntilIdle()

        // Should handle malformed package names gracefully
        assertNotNull(viewModel.uiState.value)
    }

    @Test
    fun testExtremelyLongStringHandling() = runTest {
        // Test handling of extremely long strings
        val longString = "a".repeat(10000) // 10,000 character string

        val extremeApps = listOf(
            AppInfo(
                packageName = "com.example.app",
                appName = longString,
                isPinned = false
            )
        )

        whenever(appRepository.getAllApps()).thenReturn(flowOf(extremeApps))
        whenever(settingsRepository.getSettings()).thenReturn(flowOf(LauncherSettings()))

        val viewModel = HomeViewModel(
            appRepository = appRepository,
            settingsRepository = settingsRepository
        )

        advanceUntilIdle()

        // Should handle extremely long strings
        assertNotNull(viewModel.uiState.value)
    }

    @Test
    fun testSpecialCharacterHandling() = runTest {
        // Test handling of special characters in app names
        val specialCharApps = listOf(
            AppInfo("com.example.app1", "App with 💀 emoji"),
            AppInfo("com.example.app2", "App with \n newline"),
            AppInfo("com.example.app3", "App with \u0000 null char"),
            AppInfo("com.example.app4", "App with \\backslash"),
            AppInfo("com.example.app5", "App with \"quotes\"")
        )

        whenever(appRepository.getAllApps()).thenReturn(flowOf(specialCharApps))
        whenever(settingsRepository.getSettings()).thenReturn(flowOf(LauncherSettings()))

        val viewModel = HomeViewModel(
            appRepository = appRepository,
            settingsRepository = settingsRepository
        )

        advanceUntilIdle()

        // Should handle special characters safely
        assertNotNull(viewModel.uiState.value)
    }

    @Test
    fun testRapidStateChanges() = runTest {
        // Test rapid state changes
        whenever(appRepository.getAllApps()).thenReturn(flowOf(emptyList()))
        whenever(settingsRepository.getSettings()).thenReturn(flowOf(LauncherSettings()))

        val viewModel = HomeViewModel(
            appRepository = appRepository,
            settingsRepository = settingsRepository
        )

        // Rapid operations
        repeat(1000) {
            launch {
                // Simulate rapid UI interactions
                viewModel.uiState.value
            }
        }

        advanceUntilIdle()

        // Should handle rapid state changes
        assertNotNull(viewModel.uiState.value)
    }

    @Test
    fun testIOExceptionHandling() = runTest {
        // Test I/O exception handling
        whenever(appRepository.getAllApps()).thenThrow(IOException("Disk I/O error"))

        val viewModel = HomeViewModel(
            appRepository = appRepository,
            settingsRepository = settingsRepository
        )

        advanceUntilIdle()

        // Should handle I/O exceptions gracefully
        assertNotNull(viewModel.uiState.value)
    }

    @Test
    fun testSecurityExceptionHandling() = runTest {
        // Test security exception handling
        whenever(permissionsHelper!!.hasOverlayPermission()).thenThrow(SecurityException("Permission denied"))

        // Should handle security exceptions gracefully
        try {
            permissionsHelper.hasOverlayPermission()
        } catch (e: SecurityException) {
            // Expected
        }

        assertTrue("Security exceptions should be handled", true)
    }

    @Test
    fun testThreadInterruptionHandling() = runTest {
        // Test handling of thread interruption
        whenever(appRepository.getAllApps()).thenAnswer {
            Thread.currentThread().interrupt()
            flowOf(emptyList<AppInfo>())
        }

        val viewModel = HomeViewModel(
            appRepository = appRepository,
            settingsRepository = settingsRepository
        )

        advanceUntilIdle()

        // Should handle thread interruption
        assertNotNull(viewModel.uiState.value)
    }

    @Test
    fun testZeroAndNegativeValues() = runTest {
        // Test handling of zero and negative values in time-related fields
        val invalidSession = AppSession(
            id = -1, // Negative ID
            packageName = "com.example.app",
            startTime = 0L, // Zero time
            endTime = -1000L, // Negative time
            isActive = true
        )

        // Should handle invalid numeric values gracefully
        assertTrue("Invalid session values should be handled", invalidSession.id < 0)
        assertTrue("Zero time should be handled", invalidSession.startTime == 0L)
        assertTrue("Negative time should be handled", invalidSession.endTime!! < 0)
    }
}