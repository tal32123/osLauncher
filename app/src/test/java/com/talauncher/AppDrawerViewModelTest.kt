package com.talauncher

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.talauncher.data.model.AppInfo
import com.talauncher.data.model.LauncherSettings
import com.talauncher.data.repository.AppRepository
import com.talauncher.data.repository.SettingsRepository
import com.talauncher.ui.appdrawer.AppDrawerViewModel
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
 * Unit tests for AppDrawerViewModel
 * Tests app management, launching functionality, and UI state
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AppDrawerViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var appRepository: AppRepository

    @Mock
    private lateinit var settingsRepository: SettingsRepository

    @Mock
    private lateinit var usageStatsHelper: UsageStatsHelper

    @Mock
    private lateinit var permissionsHelper: PermissionsHelper

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var onLaunchApp: (String, Int?) -> Unit

    private lateinit var viewModel: AppDrawerViewModel
    private val testDispatcher = StandardTestDispatcher()

    private val testApps = listOf(
        AppInfo("com.example.calculator", "Calculator", isPinned = false),
        AppInfo("com.example.browser", "Browser", isPinned = false),
        AppInfo("com.example.camera", "Camera", isPinned = false),
        AppInfo("com.example.maps", "Maps", isPinned = false)
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // Setup default mocks
        whenever(appRepository.getAllVisibleApps()).thenReturn(flowOf(testApps))
        whenever(appRepository.getHiddenApps()).thenReturn(flowOf(emptyList()))
        whenever(settingsRepository.getSettings()).thenReturn(
            flowOf(LauncherSettings())
        )
        whenever(usageStatsHelper.hasUsageStatsPermission()).thenReturn(false)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads all apps`() = runTest {
        viewModel = AppDrawerViewModel(
            appRepository, settingsRepository, usageStatsHelper,
            permissionsHelper, context, onLaunchApp
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(testApps.size, state.allApps.size)
        assertFalse(state.isLoading)
    }

    @Test
    fun `launch app calls repository with correct package name`() = runTest {
        whenever(appRepository.shouldShowTimeLimitPrompt(any())).thenReturn(false)
        whenever(appRepository.launchApp(any())).thenReturn(true)

        viewModel = AppDrawerViewModel(
            appRepository, settingsRepository, usageStatsHelper,
            permissionsHelper, context, onLaunchApp
        )
        advanceUntilIdle()

        val testApp = testApps.first()
        viewModel.launchApp(testApp.packageName)
        advanceUntilIdle()

        verify(appRepository).launchApp(testApp.packageName)
        verify(onLaunchApp).invoke(testApp.packageName, null)
    }

    @Test
    fun `launch app shows time limit dialog when needed`() = runTest {
        whenever(appRepository.shouldShowTimeLimitPrompt(any())).thenReturn(true)

        viewModel = AppDrawerViewModel(
            appRepository, settingsRepository, usageStatsHelper,
            permissionsHelper, context, onLaunchApp
        )
        advanceUntilIdle()

        val testApp = testApps.first()
        viewModel.launchApp(testApp.packageName)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.showTimeLimitDialog)
        assertEquals(testApp.packageName, state.selectedAppForTimeLimit)
    }

    @Test
    fun `launch app shows friction dialog when launch fails`() = runTest {
        whenever(appRepository.shouldShowTimeLimitPrompt(any())).thenReturn(false)
        whenever(appRepository.launchApp(any())).thenReturn(false)

        viewModel = AppDrawerViewModel(
            appRepository, settingsRepository, usageStatsHelper,
            permissionsHelper, context, onLaunchApp
        )
        advanceUntilIdle()

        val testApp = testApps.first()
        viewModel.launchApp(testApp.packageName)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.showFrictionDialog)
        assertEquals(testApp.packageName, state.selectedAppForFriction)
    }

    @Test
    fun `apps are loaded from repository correctly`() = runTest {
        val sortedApps = listOf(
            AppInfo("com.example.zzzlast", "ZZZ Last", isPinned = false),
            AppInfo("com.example.aaafirst", "AAA First", isPinned = false),
            AppInfo("com.example.middle", "Middle", isPinned = false)
        )
        whenever(appRepository.getAllVisibleApps()).thenReturn(flowOf(sortedApps))

        viewModel = AppDrawerViewModel(
            appRepository, settingsRepository, usageStatsHelper,
            permissionsHelper, context, onLaunchApp
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(sortedApps.size, state.allApps.size)
        assertEquals(sortedApps, state.allApps)
    }

    @Test
    fun `recent apps are loaded when usage stats permission granted`() = runTest {
        whenever(usageStatsHelper.hasUsageStatsPermission()).thenReturn(true)
        whenever(usageStatsHelper.getTopUsedApps(any())).thenReturn(
            listOf(
                com.talauncher.data.model.AppUsage("com.example.calculator", 1000L),
                com.talauncher.data.model.AppUsage("com.example.browser", 800L)
            )
        )

        viewModel = AppDrawerViewModel(
            appRepository, settingsRepository, usageStatsHelper,
            permissionsHelper, context, onLaunchApp
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.recentApps.isNotEmpty())
    }

    @Test
    fun `recent apps are empty when usage stats permission not granted`() = runTest {
        whenever(usageStatsHelper.hasUsageStatsPermission()).thenReturn(false)

        viewModel = AppDrawerViewModel(
            appRepository, settingsRepository, usageStatsHelper,
            permissionsHelper, context, onLaunchApp
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.recentApps.isEmpty())
    }

    @Test
    fun `hidden apps are loaded separately`() = runTest {
        val hiddenApps = listOf(
            AppInfo("com.example.hidden1", "Hidden App 1", isPinned = false),
            AppInfo("com.example.hidden2", "Hidden App 2", isPinned = false)
        )
        whenever(appRepository.getHiddenApps()).thenReturn(flowOf(hiddenApps))

        viewModel = AppDrawerViewModel(
            appRepository, settingsRepository, usageStatsHelper,
            permissionsHelper, context, onLaunchApp
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(hiddenApps.size, state.hiddenApps.size)
        assertEquals(hiddenApps, state.hiddenApps)
    }

    @Test
    fun `error loading apps is handled gracefully`() = runTest {
        whenever(appRepository.getAllVisibleApps()).thenThrow(RuntimeException("Database error"))

        viewModel = AppDrawerViewModel(
            appRepository, settingsRepository, usageStatsHelper,
            permissionsHelper, context, onLaunchApp
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.allApps.isEmpty())
    }

    @Test
    fun `perform google search uses context correctly`() = runTest {
        viewModel = AppDrawerViewModel(
            appRepository, settingsRepository, usageStatsHelper,
            permissionsHelper, context, onLaunchApp
        )

        viewModel.performGoogleSearch("test query")

        // Verify that context was used (startActivity should be called)
        verify(context, atLeastOnce()).startActivity(any())
    }

    @Test
    fun `math challenge difficulty is set from settings`() = runTest {
        val customSettings = LauncherSettings(mathDifficulty = "hard")
        whenever(settingsRepository.getSettings()).thenReturn(flowOf(customSettings))

        viewModel = AppDrawerViewModel(
            appRepository, settingsRepository, usageStatsHelper,
            permissionsHelper, context, onLaunchApp
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("hard", state.mathChallengeDifficulty)
    }

    @Test
    fun `recent apps limit is respected from settings`() = runTest {
        val customSettings = LauncherSettings(recentAppsLimit = 10)
        whenever(settingsRepository.getSettings()).thenReturn(flowOf(customSettings))

        viewModel = AppDrawerViewModel(
            appRepository, settingsRepository, usageStatsHelper,
            permissionsHelper, context, onLaunchApp
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(10, state.recentAppsLimit)
    }
}