package com.talauncher

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.talauncher.data.model.AppInfo
import com.talauncher.data.model.AppSession
import com.talauncher.data.model.LauncherSettings
import com.talauncher.data.model.WeatherDisplayOption
import com.talauncher.data.repository.AppRepository
import com.talauncher.data.repository.SessionRepository
import com.talauncher.data.repository.SettingsRepository
import com.talauncher.service.WeatherService
import com.talauncher.ui.home.HomeViewModel
import com.talauncher.utils.ContactHelper
import com.talauncher.utils.ErrorHandler
import com.talauncher.utils.PermissionState
import com.talauncher.utils.PermissionsHelper
import com.talauncher.utils.UsageStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.junit.Assert.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var appRepository: AppRepository

    @Mock
    private lateinit var settingsRepository: SettingsRepository

    @Mock
    private lateinit var sessionRepository: SessionRepository

    private lateinit var permissionsHelper: PermissionsHelper

    @Mock
    private lateinit var usageStatsHelper: UsageStatsHelper

    @Mock
    private lateinit var errorHandler: ErrorHandler

    @Mock
    private lateinit var appContext: Context

    @Mock
    private lateinit var contactHelper: ContactHelper

    @Mock
    private lateinit var weatherService: WeatherService

    private lateinit var viewModel: HomeViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        whenever(settingsRepository.getSettings()).thenReturn(
            flowOf(LauncherSettings(weatherDisplay = WeatherDisplayOption.OFF))
        )
        whenever(appRepository.getAllVisibleApps()).thenReturn(flowOf(emptyList()))
        whenever(appRepository.getHiddenApps()).thenReturn(flowOf(emptyList()))
        whenever(sessionRepository.observeSessionExpirations()).thenReturn(MutableSharedFlow<AppSession>().asSharedFlow())
        runBlocking { whenever(sessionRepository.emitExpiredSessions()).thenReturn(Unit) }
        runBlocking { whenever(usageStatsHelper.getPast48HoursUsageStats(any())).thenReturn(emptyList()) }
        whenever(appContext.applicationContext).thenReturn(appContext)
        runBlocking { whenever(contactHelper.isWhatsAppInstalled()).thenReturn(false) }

        permissionsHelper = object : PermissionsHelper(appContext) {
            override fun checkAllPermissions() {
                // No-op for tests to avoid accessing Android services.
            }
        }.apply {
            overridePermissionState(PermissionState())
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `all visible apps load correctly`() = runTest {
        val visibleApps = listOf(
            AppInfo("com.test.app1", "App 1"),
            AppInfo("com.test.app2", "App 2")
        )
        whenever(appRepository.getAllVisibleApps()).thenReturn(flowOf(visibleApps))

        viewModel = HomeViewModel(
            appRepository = appRepository,
            settingsRepository = settingsRepository,
            sessionRepository = sessionRepository,
            appContext = appContext,
            initialContactHelper = contactHelper,
            permissionsHelper = permissionsHelper,
            usageStatsHelper = usageStatsHelper,
            errorHandler = errorHandler,
            weatherService = weatherService
        )
        advanceUntilIdle()

        val state = viewModel.uiState.first { it.allVisibleApps.size == visibleApps.size }
        assertEquals(visibleApps, state.allVisibleApps)
    }
}
