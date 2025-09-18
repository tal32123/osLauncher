package com.talauncher

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.talauncher.data.model.AppInfo
import com.talauncher.data.model.AppSession
import com.talauncher.data.model.LauncherSettings
import com.talauncher.data.repository.AppRepository
import com.talauncher.data.repository.SessionRepository
import com.talauncher.data.repository.SettingsRepository
import com.talauncher.ui.home.HomeViewModel
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

/**
 * Unit tests for HomeViewModel
 * Tests UI state management, app launching, and user interactions
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class HomeViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

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

    private lateinit var viewModel: HomeViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // Setup default mock returns
        whenever(settingsRepository.getSettings()).thenReturn(flowOf(createDefaultSettings()))
        whenever(appRepository.getPinnedApps()).thenReturn(flowOf(emptyList()))
        whenever(sessionRepository.getAllSessions()).thenReturn(flowOf(emptyList()))

        viewModel = HomeViewModel(
            appRepository = appRepository,
            settingsRepository = settingsRepository,
            sessionRepository = sessionRepository,
            permissionsHelper = permissionsHelper,
            usageStatsHelper = usageStatsHelper
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading`() {
        // Given fresh ViewModel
        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.isLoading)
        assertTrue(state.pinnedApps.isEmpty())
        assertTrue(state.recentSessions.isEmpty())
    }

    @Test
    fun `settings updates reflect in UI state`() = runTest {
        // Given
        val newSettings = createDefaultSettings().copy(
            showRecentApps = false,
            gridSize = 6
        )
        whenever(settingsRepository.getSettings()).thenReturn(flowOf(newSettings))

        // When
        viewModel.refreshData()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.showRecentApps)
        assertEquals(6, state.gridSize)
    }

    @Test
    fun `pinned apps load correctly`() = runTest {
        // Given
        val pinnedApps = listOf(
            createMockApp("com.test.app1", "App 1", isPinned = true, pinnedOrder = 1),
            createMockApp("com.test.app2", "App 2", isPinned = true, pinnedOrder = 2)
        )
        whenever(appRepository.getPinnedApps()).thenReturn(flowOf(pinnedApps))

        // When
        viewModel.refreshData()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(2, state.pinnedApps.size)
        assertEquals("App 1", state.pinnedApps[0].appName)
        assertEquals("App 2", state.pinnedApps[1].appName)
        assertFalse(state.isLoading)
    }

    @Test
    fun `recent sessions load when enabled in settings`() = runTest {
        // Given
        val settings = createDefaultSettings().copy(showRecentApps = true, recentAppsLimit = 5)
        val recentSessions = listOf(
            createMockSession("com.test.app1", "App 1"),
            createMockSession("com.test.app2", "App 2")
        )

        whenever(settingsRepository.getSettings()).thenReturn(flowOf(settings))
        whenever(sessionRepository.getRecentSessions(5)).thenReturn(flowOf(recentSessions))

        // When
        viewModel.refreshData()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.showRecentApps)
        assertEquals(2, state.recentSessions.size)
    }

    @Test
    fun `recent sessions hidden when disabled in settings`() = runTest {
        // Given
        val settings = createDefaultSettings().copy(showRecentApps = false)
        whenever(settingsRepository.getSettings()).thenReturn(flowOf(settings))

        // When
        viewModel.refreshData()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.showRecentApps)
        assertTrue(state.recentSessions.isEmpty())
    }

    @Test
    fun `launchApp calls repository with correct parameters`() = runTest {
        // Given
        val packageName = "com.test.app"
        whenever(appRepository.launchApp(packageName, null, false)).thenReturn(true)

        // When
        val result = viewModel.launchApp(packageName)

        // Then
        assertTrue(result)
        verify(appRepository).launchApp(packageName, null, false)
    }

    @Test
    fun `launchApp with planned duration starts session`() = runTest {
        // Given
        val packageName = "com.test.app"
        val plannedDuration = 30
        whenever(appRepository.launchApp(packageName, plannedDuration, false)).thenReturn(true)

        // When
        val result = viewModel.launchApp(packageName, plannedDuration)

        // Then
        assertTrue(result)
        verify(appRepository).launchApp(packageName, plannedDuration, false)
    }

    @Test
    fun `pinApp updates repository and refreshes data`() = runTest {
        // Given
        val packageName = "com.test.app"

        // When
        viewModel.pinApp(packageName)
        advanceUntilIdle()

        // Then
        verify(appRepository).pinApp(packageName)
        verify(appRepository, atLeastOnce()).getPinnedApps()
    }

    @Test
    fun `unpinApp updates repository and refreshes data`() = runTest {
        // Given
        val packageName = "com.test.app"

        // When
        viewModel.unpinApp(packageName)
        advanceUntilIdle()

        // Then
        verify(appRepository).unpinApp(packageName)
        verify(appRepository, atLeastOnce()).getPinnedApps()
    }

    @Test
    fun `syncInstalledApps triggers repository sync and refreshes data`() = runTest {
        // When
        viewModel.syncInstalledApps()
        advanceUntilIdle()

        // Then
        verify(appRepository).syncInstalledApps()
        verify(appRepository, atLeastOnce()).getPinnedApps()
    }

    @Test
    fun `hasUsageStatsPermission delegates to permissionsHelper`() {
        // Given
        whenever(permissionsHelper.hasUsageStatsPermission()).thenReturn(true)

        // When
        val result = viewModel.hasUsageStatsPermission()

        // Then
        assertTrue(result)
        verify(permissionsHelper).hasUsageStatsPermission()
    }

    @Test
    fun `requestUsageStatsPermission delegates to permissionsHelper`() {
        // When
        viewModel.requestUsageStatsPermission()

        // Then
        verify(permissionsHelper).requestUsageStatsPermission()
    }

    @Test
    fun `error state is set when repository operations fail`() = runTest {
        // Given
        whenever(appRepository.getPinnedApps()).thenThrow(RuntimeException("Database error"))

        // When
        viewModel.refreshData()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Database error"))
        assertFalse(state.isLoading)
    }

    @Test
    fun `clearError resets error state`() = runTest {
        // Given - Set up error state
        whenever(appRepository.getPinnedApps()).thenThrow(RuntimeException("Test error"))
        viewModel.refreshData()
        advanceUntilIdle()

        // Verify error is set
        assertNotNull(viewModel.uiState.value.errorMessage)

        // When
        viewModel.clearError()

        // Then
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `loading state is managed correctly during refresh`() = runTest {
        // Given
        var loadingStates = mutableListOf<Boolean>()

        // Collect loading states
        val collectJob = backgroundScope.launch {
            viewModel.uiState.collect { state ->
                loadingStates.add(state.isLoading)
            }
        }

        // When
        viewModel.refreshData()
        advanceUntilIdle()

        collectJob.cancel()

        // Then
        assertTrue("Should start with loading true", loadingStates.first())
        assertFalse("Should end with loading false", loadingStates.last())
    }

    @Test
    fun `active session is tracked correctly`() = runTest {
        // Given
        val activeSession = createMockSession("com.test.app", "Test App", isActive = true)
        whenever(sessionRepository.getAllSessions()).thenReturn(flowOf(listOf(activeSession)))

        // When
        viewModel.refreshData()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(1, state.recentSessions.size)
        assertTrue(state.recentSessions[0].isActive)
    }

    @Test
    fun `grid size changes are reflected in UI state`() = runTest {
        // Given
        val settingsWithDifferentGridSize = createDefaultSettings().copy(gridSize = 8)
        whenever(settingsRepository.getSettings()).thenReturn(flowOf(settingsWithDifferentGridSize))

        // When
        viewModel.refreshData()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(8, state.gridSize)
    }

    // Helper methods
    private fun createDefaultSettings() = LauncherSettings(
        id = 1,
        darkMode = false,
        showAppCounts = true,
        gridSize = 4,
        autoHideNavigationBar = false,
        showRecentApps = true,
        enableTimeLimitPrompt = false,
        enableMathChallenge = false,
        mathDifficulty = "easy",
        sessionExpiryCountdownSeconds = 5,
        recentAppsLimit = 10
    )

    private fun createMockApp(
        packageName: String,
        appName: String,
        isHidden: Boolean = false,
        isPinned: Boolean = false,
        pinnedOrder: Int = 0,
        isDistracting: Boolean = false
    ) = AppInfo(
        packageName = packageName,
        appName = appName,
        isHidden = isHidden,
        isPinned = isPinned,
        pinnedOrder = pinnedOrder,
        isDistracting = isDistracting
    )

    private fun createMockSession(
        packageName: String,
        appName: String,
        isActive: Boolean = false
    ) = AppSession(
        id = 1L,
        packageName = packageName,
        startTime = System.currentTimeMillis() - 3600000,
        plannedDurationMs = 1800000,
        isActive = isActive,
        endTime = if (isActive) null else System.currentTimeMillis() - 1800000
    )
}