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
    fun testDatabaseConnectionFailure() = runTest {
        whenever(appRepository.getAllVisibleApps()).thenThrow(RuntimeException("Database connection failed"))
        whenever(settingsRepository.getSettings()).thenThrow(RuntimeException("Database connection failed"))

        val viewModel = HomeViewModel(
            appRepository = appRepository,
            settingsRepository = settingsRepository,
            sessionRepository = sessionRepository,
            permissionsHelper = permissionsHelper,
            usageStatsHelper = usageStatsHelper,
            errorHandler = errorHandler
        )

        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value)
    }
}
