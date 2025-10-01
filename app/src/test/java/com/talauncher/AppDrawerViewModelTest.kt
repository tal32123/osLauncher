package com.talauncher

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.talauncher.data.model.AppInfo
import com.talauncher.data.model.LauncherSettings
import com.talauncher.data.repository.AppRepository
import com.talauncher.data.repository.SettingsRepository
import com.talauncher.ui.appdrawer.AppDrawerViewModel
import com.talauncher.utils.ContactHelper
import com.talauncher.utils.PermissionState
import com.talauncher.utils.PermissionsHelper
import com.talauncher.utils.UsageStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
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
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.junit.Assert.assertEquals

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
    private lateinit var contactHelper: ContactHelper

    @Mock
    private lateinit var context: Context

    private lateinit var viewModel: AppDrawerViewModel
    private val testDispatcher = StandardTestDispatcher()

    private val testApps = listOf(
        AppInfo("com.example.calculator", "Calculator"),
        AppInfo("com.example.browser", "Browser"),
        AppInfo("com.example.camera", "Camera"),
        AppInfo("com.example.maps", "Maps")
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        whenever(appRepository.getAllVisibleApps()).thenReturn(flowOf(testApps))
        whenever(appRepository.getHiddenApps()).thenReturn(flowOf(emptyList()))
        whenever(settingsRepository.getSettings()).thenReturn(flowOf(LauncherSettings()))
        whenever(permissionsHelper.permissionState).thenReturn(MutableStateFlow(PermissionState()))
        runBlocking {
            whenever(usageStatsHelper.getPast48HoursUsageStats(any())).thenReturn(emptyList())
            whenever(contactHelper.isWhatsAppInstalled()).thenReturn(false)
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads all apps`() = runTest {
        viewModel = AppDrawerViewModel(
            appRepository,
            settingsRepository,
            /* searchInteractionRepository = */ null,
            usageStatsHelper,
            permissionsHelper,
            contactHelper,
            context
        )

        advanceUntilIdle()
        val state = viewModel.uiState.first { it.allApps.isNotEmpty() }

        assertEquals(testApps, state.allApps)
    }

    @Test
    fun `launch app calls repository with correct package name`() = runTest {
        whenever(appRepository.shouldShowTimeLimitPrompt(any())).thenReturn(false)
        whenever(appRepository.launchApp(any(), eq(false), isNull())).thenReturn(true)

        viewModel = AppDrawerViewModel(
            appRepository,
            settingsRepository,
            /* searchInteractionRepository = */ null,
            usageStatsHelper,
            permissionsHelper,
            contactHelper,
            context
        )
        advanceUntilIdle()

        val testApp = testApps.first()
        viewModel.launchApp(testApp.packageName)
        advanceUntilIdle()

        verifyBlocking(appRepository) { launchApp(testApp.packageName, false, null) }
    }
}
