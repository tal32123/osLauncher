package com.talauncher

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.talauncher.data.model.LauncherSettings
import com.talauncher.data.repository.AppRepository
import com.talauncher.data.repository.SettingsRepository
import com.talauncher.ui.main.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
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
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MainViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var settingsRepository: SettingsRepository

    @Mock
    private lateinit var appRepository: AppRepository

    private lateinit var viewModel: MainViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        runBlocking { whenever(appRepository.syncInstalledApps()).thenReturn(Unit) }
        whenever(settingsRepository.getSettings()).thenReturn(flowOf(LauncherSettings(isOnboardingCompleted = false)))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading`() {
        println("Running initial state is loading test")
        viewModel = MainViewModel(settingsRepository, appRepository)

        assertTrue(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isOnboardingCompleted)
    }

    @Test
    fun `loading completes when settings are loaded`() = runTest {
        println("Running loading completes when settings are loaded test")
        whenever(settingsRepository.getSettings()).thenReturn(flowOf(LauncherSettings(isOnboardingCompleted = true)))

        viewModel = MainViewModel(settingsRepository, appRepository)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.isOnboardingCompleted)
    }

    @Test
    fun `onboarding not completed shows onboarding flow`() = runTest {
        println("Running onboarding not completed shows onboarding flow test")
        whenever(settingsRepository.getSettings()).thenReturn(flowOf(LauncherSettings(isOnboardingCompleted = false)))

        viewModel = MainViewModel(settingsRepository, appRepository)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isOnboardingCompleted)
    }

    @Test
    fun `onOnboardingCompleted updates ui state`() = runTest {
        println("Running onOnboardingCompleted updates ui state test")
        viewModel = MainViewModel(settingsRepository, appRepository)

        viewModel.onOnboardingCompleted()

        assertTrue(viewModel.uiState.value.isOnboardingCompleted)
    }
}
