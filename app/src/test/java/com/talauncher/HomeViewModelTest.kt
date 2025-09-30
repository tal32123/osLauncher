package com.talauncher

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.talauncher.data.model.AppInfo
import com.talauncher.data.model.LauncherSettings
import com.talauncher.data.repository.AppRepository
import com.talauncher.data.repository.SettingsRepository
import com.talauncher.ui.home.HomeViewModel
import com.talauncher.utils.ErrorHandler
import com.talauncher.utils.PermissionState
import com.talauncher.utils.PermissionsHelper
import com.talauncher.utils.UsageStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.junit.Assert.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class HomeViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var appRepository: AppRepository

    @Mock
    private lateinit var settingsRepository: SettingsRepository

    @Mock
    private lateinit var permissionsHelper: PermissionsHelper

    @Mock
    private lateinit var usageStatsHelper: UsageStatsHelper

    @Mock
    private lateinit var errorHandler: ErrorHandler

    private lateinit var viewModel: HomeViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        whenever(settingsRepository.getSettings()).thenReturn(flowOf(LauncherSettings()))
        whenever(appRepository.getAllVisibleApps()).thenReturn(flowOf(emptyList()))
        whenever(appRepository.getHiddenApps()).thenReturn(flowOf(emptyList()))
        whenever(permissionsHelper.permissionState).thenReturn(MutableStateFlow(PermissionState()))
        runBlocking { whenever(usageStatsHelper.getPast48HoursUsageStats(any())).thenReturn(emptyList()) }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `all visible apps load correctly`() = runTest {
        println("Running all visible apps load correctly test")
        val visibleApps = listOf(
            AppInfo("com.test.app1", "App 1"),
            AppInfo("com.test.app2", "App 2")
        )
        whenever(appRepository.getAllVisibleApps()).thenReturn(flowOf(visibleApps))

        val appContext = ApplicationProvider.getApplicationContext<Context>()
        viewModel = HomeViewModel(
            appRepository = appRepository,
            settingsRepository = settingsRepository,
            appContext = appContext,
            permissionsHelper = permissionsHelper,
            usageStatsHelper = usageStatsHelper,
            errorHandler = errorHandler
        )
        advanceUntilIdle()

        val state = viewModel.uiState.first { it.allVisibleApps.size == visibleApps.size }
        assertEquals(visibleApps, state.allVisibleApps)
    }
}
