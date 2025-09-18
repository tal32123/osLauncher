package com.talauncher

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.talauncher.data.model.LauncherSettings
import com.talauncher.data.repository.AppRepository
import com.talauncher.data.repository.SettingsRepository
import com.talauncher.ui.main.MainViewModel
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
 * Unit tests for MainViewModel
 * Tests initialization, onboarding flow, and loading states
 */
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

        // Setup default mocks
        whenever(settingsRepository.getSettings()).thenReturn(
            flowOf(LauncherSettings(isOnboardingCompleted = false))
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading`() {
        viewModel = MainViewModel(settingsRepository, appRepository)

        assertTrue(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isOnboardingCompleted)
    }

    @Test
    fun `loading completes when settings are loaded`() = runTest {
        whenever(settingsRepository.getSettings()).thenReturn(
            flowOf(LauncherSettings(isOnboardingCompleted = true))
        )

        viewModel = MainViewModel(settingsRepository, appRepository)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.isOnboardingCompleted)
    }

    @Test
    fun `onboarding not completed shows onboarding flow`() = runTest {
        whenever(settingsRepository.getSettings()).thenReturn(
            flowOf(LauncherSettings(isOnboardingCompleted = false))
        )

        viewModel = MainViewModel(settingsRepository, appRepository)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isOnboardingCompleted)
    }

    @Test
    fun `onOnboardingCompleted updates settings`() = runTest {
        viewModel = MainViewModel(settingsRepository, appRepository)

        viewModel.onOnboardingCompleted()

        verify(settingsRepository).updateOnboardingCompleted(true)
    }

    @Test
    fun `settings changes are reflected in UI state`() = runTest {
        val settingsFlow = flowOf(
            LauncherSettings(isOnboardingCompleted = false),
            LauncherSettings(isOnboardingCompleted = true)
        )
        whenever(settingsRepository.getSettings()).thenReturn(settingsFlow)

        viewModel = MainViewModel(settingsRepository, appRepository)
        advanceUntilIdle()

        // Should reflect the latest settings
        assertTrue(viewModel.uiState.value.isOnboardingCompleted)
    }

    @Test
    fun `error in settings repository is handled gracefully`() = runTest {
        whenever(settingsRepository.getSettings()).thenThrow(RuntimeException("Database error"))

        viewModel = MainViewModel(settingsRepository, appRepository)
        advanceUntilIdle()

        // Should not crash and maintain loading state
        assertTrue(viewModel.uiState.value.isLoading)
    }
}