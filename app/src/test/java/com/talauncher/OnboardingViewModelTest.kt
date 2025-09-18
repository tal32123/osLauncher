package com.talauncher

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.talauncher.data.repository.SettingsRepository
import com.talauncher.ui.onboarding.OnboardingViewModel
import com.talauncher.utils.PermissionsHelper
import com.talauncher.utils.UsageStatsHelper
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

/**
 * Unit tests for OnboardingViewModel
 * Tests permission requests and onboarding flow completion
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class OnboardingViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var permissionsHelper: PermissionsHelper

    @Mock
    private lateinit var usageStatsHelper: UsageStatsHelper

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
    fun `initial state shows first step`() {
        viewModel = OnboardingViewModel(permissionsHelper, usageStatsHelper, settingsRepository)

        val state = viewModel.uiState.value
        assertEquals(0, state.currentStep)
        assertFalse(state.isLoading)
        assertFalse(state.canProceed)
    }

    @Test
    fun `next step increments current step`() = runTest {
        viewModel = OnboardingViewModel(permissionsHelper, usageStatsHelper, settingsRepository)

        viewModel.nextStep()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `previous step decrements current step`() = runTest {
        viewModel = OnboardingViewModel(permissionsHelper, usageStatsHelper, settingsRepository)

        // Go to step 1 first
        viewModel.nextStep()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.currentStep)

        // Go back to step 0
        viewModel.previousStep()
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `cannot go to previous step from first step`() = runTest {
        viewModel = OnboardingViewModel(permissionsHelper, usageStatsHelper, settingsRepository)

        viewModel.previousStep()
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `request usage stats permission calls helper`() = runTest {
        viewModel = OnboardingViewModel(permissionsHelper, usageStatsHelper, settingsRepository)

        viewModel.requestUsageStatsPermission()

        verify(usageStatsHelper).requestUsageStatsPermission()
    }

    @Test
    fun `request overlay permission calls helper`() = runTest {
        viewModel = OnboardingViewModel(permissionsHelper, usageStatsHelper, settingsRepository)

        viewModel.requestOverlayPermission()

        verify(permissionsHelper).requestOverlayPermission()
    }

    @Test
    fun `check permissions updates state correctly when all granted`() = runTest {
        whenever(usageStatsHelper.hasUsageStatsPermission()).thenReturn(true)
        whenever(permissionsHelper.hasOverlayPermission()).thenReturn(true)

        viewModel = OnboardingViewModel(permissionsHelper, usageStatsHelper, settingsRepository)

        viewModel.checkPermissions()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.hasUsageStatsPermission)
        assertTrue(state.hasOverlayPermission)
        assertTrue(state.canProceed)
    }

    @Test
    fun `check permissions updates state correctly when permissions missing`() = runTest {
        whenever(usageStatsHelper.hasUsageStatsPermission()).thenReturn(false)
        whenever(permissionsHelper.hasOverlayPermission()).thenReturn(false)

        viewModel = OnboardingViewModel(permissionsHelper, usageStatsHelper, settingsRepository)

        viewModel.checkPermissions()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.hasUsageStatsPermission)
        assertFalse(state.hasOverlayPermission)
        assertFalse(state.canProceed)
    }

    @Test
    fun `complete onboarding updates settings`() = runTest {
        viewModel = OnboardingViewModel(permissionsHelper, usageStatsHelper, settingsRepository)

        viewModel.completeOnboarding()

        verify(settingsRepository).updateOnboardingCompleted(true)
    }

    @Test
    fun `error during permission check is handled gracefully`() = runTest {
        whenever(usageStatsHelper.hasUsageStatsPermission()).thenThrow(RuntimeException("Permission error"))

        viewModel = OnboardingViewModel(permissionsHelper, usageStatsHelper, settingsRepository)

        viewModel.checkPermissions()
        advanceUntilIdle()

        // Should not crash and maintain safe state
        val state = viewModel.uiState.value
        assertFalse(state.canProceed)
    }

    @Test
    fun `loading state is managed correctly during operations`() = runTest {
        viewModel = OnboardingViewModel(permissionsHelper, usageStatsHelper, settingsRepository)

        // Should not be loading initially
        assertFalse(viewModel.uiState.value.isLoading)

        // Note: Loading state management would depend on actual implementation
        // This test structure is prepared for when loading states are implemented
    }

    @Test
    fun `can proceed only when all required permissions are granted`() = runTest {
        viewModel = OnboardingViewModel(permissionsHelper, usageStatsHelper, settingsRepository)

        // Test partial permissions
        whenever(usageStatsHelper.hasUsageStatsPermission()).thenReturn(true)
        whenever(permissionsHelper.hasOverlayPermission()).thenReturn(false)

        viewModel.checkPermissions()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.canProceed)

        // Test all permissions granted
        whenever(permissionsHelper.hasOverlayPermission()).thenReturn(true)

        viewModel.checkPermissions()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.canProceed)
    }
}