package com.talauncher

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.talauncher.data.model.AppInfo
import com.talauncher.data.model.AppSession
import com.talauncher.data.model.LauncherSettings
import com.talauncher.data.repository.AppRepository
import com.talauncher.data.repository.SessionRepository
import com.talauncher.data.repository.SettingsRepository
import com.talauncher.ui.home.HomeViewModel
import com.talauncher.utils.ErrorHandler
import com.talauncher.utils.PermissionState
import com.talauncher.utils.PermissionsHelper
import com.talauncher.utils.UsageStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
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
    fun `home view model falls back to empty state when data sources return nothing`() = runTest {
        println("Running home view model falls back to empty state when data sources return nothing test")
        whenever(appRepository.getAllVisibleApps()).thenReturn(flowOf(emptyList()))
        whenever(appRepository.getHiddenApps()).thenReturn(flowOf(emptyList()))
        whenever(settingsRepository.getSettings()).thenReturn(flowOf(null))
        whenever(sessionRepository.observeSessionExpirations()).thenReturn(MutableSharedFlow<AppSession>().asSharedFlow())
        whenever(permissionsHelper.permissionState).thenReturn(MutableStateFlow(PermissionState()))
        whenever(usageStatsHelper.getPast48HoursUsageStats(any())).thenReturn(emptyList())

        val appContext = ApplicationProvider.getApplicationContext<Context>()
        val viewModel = HomeViewModel(
            appRepository = appRepository,
            settingsRepository = settingsRepository,
            sessionRepository = sessionRepository,
            appContext = appContext,
            permissionsHelper = permissionsHelper,
            usageStatsHelper = usageStatsHelper,
            errorHandler = errorHandler
        )
        advanceUntilIdle()

        val state = viewModel.uiState.first { !it.isLoading }
        assertEquals(emptyList<AppInfo>(), state.allVisibleApps)
        assertEquals(emptyList<AppInfo>(), state.recentApps)
    }
}
