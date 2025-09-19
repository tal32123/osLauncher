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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    private lateinit var sessionRepository: SessionRepository

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
        whenever(appRepository.getPinnedApps()).thenReturn(flowOf(emptyList()))
        whenever(sessionRepository.observeSessionExpirations()).thenReturn(MutableSharedFlow<AppSession>().asSharedFlow())

        viewModel = HomeViewModel(
            appRepository = appRepository,
            settingsRepository = settingsRepository,
            sessionRepository = sessionRepository,
            permissionsHelper = permissionsHelper,
            usageStatsHelper = usageStatsHelper,
            errorHandler = errorHandler
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `pinned apps load correctly`() = runTest {
        val pinnedApps = listOf(
            AppInfo("com.test.app1", "App 1", isPinned = true, pinnedOrder = 1, isDistracting = false, isHidden = false),
            AppInfo("com.test.app2", "App 2", isPinned = true, pinnedOrder = 2, isDistracting = false, isHidden = false)
        )
        whenever(appRepository.getPinnedApps()).thenReturn(flowOf(pinnedApps))

        viewModel = HomeViewModel(
            appRepository = appRepository,
            settingsRepository = settingsRepository,
            sessionRepository = sessionRepository,
            permissionsHelper = permissionsHelper,
            usageStatsHelper = usageStatsHelper,
            errorHandler = errorHandler
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.pinnedApps.size)
        assertEquals("App 1", state.pinnedApps[0].appName)
        assertEquals("App 2", state.pinnedApps[1].appName)
    }
}