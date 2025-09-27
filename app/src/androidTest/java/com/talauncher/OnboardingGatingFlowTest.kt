package com.talauncher

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
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
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingGatingFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun onboardingGatingFlow_requiresCompletingAllStepsBeforeSuccessCard() {
        val context = composeTestRule.activity
        val notificationsInitiallyGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        val usageStatsHelper = FakeUsageStatsHelper(context)
        val permissionsHelper = FakePermissionsHelper(context, notificationsInitiallyGranted).apply {
            onDefaultLauncherRequest = { usageStatsHelper.setDefaultLauncher(true) }
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
        composeTestRule.onNodeWithTag("onboarding_success_card").assertDoesNotExist()
        composeTestRule.onNodeWithTag("onboarding_incomplete_message").assertExists()

        // All buttons should be enabled initially
        composeTestRule.onNodeWithTag("onboarding_step_default_launcher_button").assertIsEnabled()
        composeTestRule.onNodeWithTag("onboarding_step_usage_stats_button").assertIsEnabled()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            composeTestRule.onNodeWithTag("onboarding_step_notifications_button").assertIsEnabled()
        }
        composeTestRule.onNodeWithTag("onboarding_step_overlay_button").assertIsEnabled()

        // Grant usage stats permission
        composeTestRule.onNodeWithTag("onboarding_step_usage_stats_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("onboarding_step_usage_stats_button").assertIsNotEnabled()

        // Grant notifications permission if required
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            composeTestRule.onNodeWithTag("onboarding_step_notifications_button").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("onboarding_step_notifications_button").assertIsNotEnabled()
        }

        // Grant overlay permission
        println("About to click overlay button, checking if enabled...")
        composeTestRule.onNodeWithTag("onboarding_step_overlay_button").assertIsEnabled()
        println("Overlay button is enabled, clicking now...")
        try {
            composeTestRule.onNodeWithTag("onboarding_step_overlay_button").performClick()
            println("Clicked overlay button successfully")
        } catch (e: Exception) {
            println("Error clicking overlay button: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
        }
        composeTestRule.waitForIdle()

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
        composeTestRule.onNodeWithTag("onboarding_step_default_launcher_button").performClick()
        composeTestRule.waitForIdle()

        // Now should show success card and complete onboarding
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onNodeWithTag("onboarding_success_card").assertExists()
            true
        }
        composeTestRule.onNodeWithTag("onboarding_incomplete_message").assertDoesNotExist()

        // Completion callback should have been called
        composeTestRule.waitUntil(timeoutMillis = 5_000) { completionCount == 1 }
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
