package com.talauncher

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.talauncher.data.repository.SettingsRepository
import com.talauncher.ui.onboarding.OnboardingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class OnboardingViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var settingsRepository: SettingsRepository

    private lateinit var viewModel: OnboardingViewModel
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
    fun `complete onboarding updates settings`() = runTest {
        viewModel = OnboardingViewModel(settingsRepository)

        viewModel.completeOnboarding()
        advanceUntilIdle()

        verify(settingsRepository).completeOnboarding()
        assertTrue(viewModel.uiState.value.isOnboardingCompleted)
    }
}
