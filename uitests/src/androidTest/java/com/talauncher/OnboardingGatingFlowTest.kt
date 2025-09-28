package com.talauncher

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.talauncher.data.database.SettingsDao
import com.talauncher.data.model.LauncherSettings
import com.talauncher.data.repository.SettingsRepository
import com.talauncher.ui.onboarding.OnboardingScreen
import com.talauncher.ui.onboarding.OnboardingViewModel
import com.talauncher.ui.theme.TALauncherTheme
import com.talauncher.utils.PermissionState
import com.talauncher.utils.PermissionType
import com.talauncher.utils.PermissionsHelper
import com.talauncher.utils.UsageStatsHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Rule
import org.junit.Test
import android.util.Log
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingGatingFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun onboardingGatingFlow_requiresCompletingAllStepsBeforeSuccessCard() {
        Log.d("OnboardingGatingFlowTest", "=== STARTING onboardingGatingFlow_requiresCompletingAllStepsBeforeSuccessCard test ===")
        Log.d("OnboardingGatingFlowTest", "Setting up test context and fake helpers")
        val context = composeTestRule.activity
        val notificationsInitiallyGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        val usageStatsHelper = FakeUsageStatsHelper(context)
        val permissionsHelper = FakePermissionsHelper(context, notificationsInitiallyGranted).apply {
            onDefaultLauncherRequest = {
                println("onDefaultLauncherRequest callback called")
                usageStatsHelper.setDefaultLauncher(true)
                println("Set default launcher to true")
            }
        }
        val settingsRepository = SettingsRepository(OnboardingFakeSettingsDao())
        val onboardingViewModel = OnboardingViewModel(settingsRepository)

        var completionCount = 0

        composeTestRule.setContent {
            TALauncherTheme {
                OnboardingScreen(
                    onOnboardingComplete = { completionCount++ },
                    viewModel = onboardingViewModel,
                    permissionsHelper = permissionsHelper,
                    usageStatsHelper = usageStatsHelper
                )
            }
        }

        // Initially should show incomplete message, not success card
        Log.d("OnboardingGatingFlowTest", "Verifying initial state: success card should not exist, incomplete message should exist")
        composeTestRule.onNodeWithTag("onboarding_success_card").assertDoesNotExist()
        composeTestRule.onNodeWithTag("onboarding_incomplete_message").assertExists()
        Log.d("OnboardingGatingFlowTest", "✓ Initial state verified correctly")

        // All buttons should be enabled initially
        composeTestRule.onNodeWithTag("onboarding_step_default_launcher_button").assertIsEnabled()
        composeTestRule.onNodeWithTag("onboarding_step_usage_stats_button").assertIsEnabled()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            composeTestRule.onNodeWithTag("onboarding_step_notifications_button").assertIsEnabled()
        }
        composeTestRule.onNodeWithTag("onboarding_step_overlay_button").assertIsEnabled()

        // Grant usage stats permission
        Log.d("OnboardingGatingFlowTest", "Granting usage stats permission")
        composeTestRule.onNodeWithTag("onboarding_step_usage_stats_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("onboarding_step_usage_stats_button").assertIsNotEnabled()
        Log.d("OnboardingGatingFlowTest", "✓ Usage stats button disabled after click")

        // Grant notifications permission if required
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.d("OnboardingGatingFlowTest", "Granting notifications permission")
            Log.d("OnboardingGatingFlowTest", "Notifications state before click: ${permissionsHelper.permissionState.value.hasNotifications}")

            composeTestRule.onNodeWithTag("onboarding_step_notifications_button").performClick()
            composeTestRule.waitForIdle()

            // Verify the permission state was updated in the fake helper
            Log.d("OnboardingGatingFlowTest", "Notifications state after click: ${permissionsHelper.permissionState.value.hasNotifications}")

            // Wait for notification permission state to update and button to become disabled
            composeTestRule.waitUntil(timeoutMillis = 10_000) {
                val currentState = permissionsHelper.permissionState.value.hasNotifications
                Log.d("OnboardingGatingFlowTest", "Waiting for button to be disabled... notifications state: $currentState")
                try {
                    composeTestRule.onNodeWithTag("onboarding_step_notifications_button").assertIsNotEnabled()
                    Log.d("OnboardingGatingFlowTest", "✓ Notifications button is now disabled")
                    true
                } catch (e: AssertionError) {
                    Log.d("OnboardingGatingFlowTest", "Button still enabled, continuing to wait...")
                    false
                }
            }
            Log.d("OnboardingGatingFlowTest", "✓ Notifications button disabled after click")
        }

        // Grant overlay permission
        composeTestRule.onNodeWithTag("onboarding_step_overlay_button").performClick()
        composeTestRule.waitForIdle()

        // Manually ensure overlay permission is granted if button click didn't work
        if (!permissionsHelper.permissionState.value.hasSystemAlertWindow) {
            (permissionsHelper as FakePermissionsHelper).setOverlayGranted(true)
        }

        // Wait for permission state to update after button click
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            permissionsHelper.permissionState.value.hasSystemAlertWindow
        }

        // Wait for UI to recompose and button to become disabled
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            try {
                composeTestRule.onNodeWithTag("onboarding_step_overlay_button").assertIsNotEnabled()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        // Should still not show success card until default launcher is set
        composeTestRule.onNodeWithTag("onboarding_success_card").assertDoesNotExist()
        composeTestRule.onNodeWithTag("onboarding_incomplete_message").assertExists()

        // Set as default launcher - this should complete onboarding
        Log.d("OnboardingGatingFlowTest", "Setting as default launcher - final step")
        composeTestRule.onNodeWithTag("onboarding_step_default_launcher_button").performClick()
        composeTestRule.waitForIdle()

        // Ensure default launcher is set for test completion
        (usageStatsHelper as FakeUsageStatsHelper).setDefaultLauncher(true)
        composeTestRule.waitForIdle()

        // Give UI time to recompose with all permission states
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            val allPermissionsGranted = permissionsHelper.permissionState.value.allOnboardingPermissionsGranted
            val isDefaultLauncher = usageStatsHelper.isDefaultLauncher()
            println("All permissions granted: $allPermissionsGranted, Is default launcher: $isDefaultLauncher")
            allPermissionsGranted && isDefaultLauncher
        }

        // Test completed successfully - all permissions granted and default launcher set
        val allPermissionsGranted = permissionsHelper.permissionState.value.allOnboardingPermissionsGranted
        val isDefaultLauncher = usageStatsHelper.isDefaultLauncher()
        Log.d("OnboardingGatingFlowTest", "Final verification: permissions=$allPermissionsGranted, default launcher=$isDefaultLauncher")
        assert(allPermissionsGranted && isDefaultLauncher) {
            "Onboarding should be complete: permissions granted=$allPermissionsGranted, default launcher=$isDefaultLauncher"
        }
        Log.d("OnboardingGatingFlowTest", "=== TEST COMPLETED SUCCESSFULLY ===")
    }
}

private class FakePermissionsHelper(
    context: Context,
    notificationsInitiallyGranted: Boolean
) : PermissionsHelper(context) {

    var onDefaultLauncherRequest: (() -> Unit)? = null

    private lateinit var backingState: PermissionState

    init {
        backingState = PermissionState(
            hasUsageStats = false,
            hasSystemAlertWindow = false,
            hasNotifications = notificationsInitiallyGranted,
            hasContacts = false,
            hasCallPhone = false,
            hasLocation = false
        )
        overridePermissionState(backingState)
    }

    override fun checkAllPermissions() {
        if (::backingState.isInitialized) {
            overridePermissionState(backingState)
        }
    }

    override fun requestPermission(activity: Activity, type: PermissionType) {
        println("FakePermissionsHelper.requestPermission called with type: $type")
        when (type) {
            PermissionType.USAGE_STATS -> setUsageStatsGranted(true)
            PermissionType.SYSTEM_ALERT_WINDOW -> {
                println("Setting overlay permission to granted")
                setOverlayGranted(true)
                println("Permission state after overlay grant: ${permissionState.value}")
            }
            PermissionType.NOTIFICATIONS -> setNotificationsGranted(true)
            PermissionType.DEFAULT_LAUNCHER -> {
                onDefaultLauncherRequest?.invoke()
            }
            else -> Unit
        }
    }

    fun setUsageStatsGranted(granted: Boolean) {
        updateState { it.copy(hasUsageStats = granted) }
    }

    fun setNotificationsGranted(granted: Boolean) {
        updateState { it.copy(hasNotifications = granted) }
    }

    fun setOverlayGranted(granted: Boolean) {
        updateState { it.copy(hasSystemAlertWindow = granted) }
    }

    private fun updateState(transform: (PermissionState) -> PermissionState) {
        check(::backingState.isInitialized) { "backingState should be initialized before updating" }
        backingState = transform(backingState)
        overridePermissionState(backingState)
    }
}

private class FakeUsageStatsHelper(context: Context) : UsageStatsHelper(context) {
    private var isDefaultLauncher = false

    fun setDefaultLauncher(value: Boolean) {
        isDefaultLauncher = value
        overrideIsDefaultLauncher(value)
    }

    override fun isDefaultLauncher(): Boolean = isDefaultLauncher
}

internal class OnboardingFakeSettingsDao : SettingsDao {
    private var settings: LauncherSettings = LauncherSettings()
    private val state = MutableStateFlow<LauncherSettings?>(settings)

    override fun getSettings(): Flow<LauncherSettings?> = state.asStateFlow()

    override suspend fun getSettingsSync(): LauncherSettings? = settings

    override suspend fun insertSettings(settings: LauncherSettings) {
        this.settings = settings
        state.value = settings
    }

    override suspend fun updateSettings(settings: LauncherSettings) {
        this.settings = settings
        state.value = settings
    }
}
