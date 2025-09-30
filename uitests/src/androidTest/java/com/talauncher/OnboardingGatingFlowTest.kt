package com.talauncher

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
            onDefaultLauncherRequest = {
                usageStatsHelper.setDefaultLauncher(true)
            }
        }
        val settingsRepository = SettingsRepository(OnboardingFakeSettingsDao())
        val onboardingViewModel = OnboardingViewModel(settingsRepository)

        composeTestRule.setContent {
            TALauncherTheme {
                OnboardingScreen(
                    onOnboardingComplete = {},
                    viewModel = onboardingViewModel,
                    permissionsHelper = permissionsHelper,
                    usageStatsHelper = usageStatsHelper
                )
            }
        }

        // Initial state
        composeTestRule.onNodeWithTag("onboarding_success_card").assertDoesNotExist()
        composeTestRule.onNodeWithTag("onboarding_incomplete_message").assertExists()
<<<<<<< HEAD
=======
        Log.d("OnboardingGatingFlowTest", "Initial state verified correctly")
>>>>>>> master

        // Usage stats
        composeTestRule.onNodeWithTag("onboarding_step_usage_stats_button").assertIsEnabled()
        composeTestRule.onNodeWithTag("onboarding_step_usage_stats_button").performClick()
        composeTestRule.onNodeWithTag("onboarding_step_usage_stats_button").assertIsNotEnabled()
<<<<<<< HEAD
=======
        Log.d("OnboardingGatingFlowTest", "Usage stats button disabled after click")
>>>>>>> master

        // Notifications (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
<<<<<<< HEAD
            (permissionsHelper as FakePermissionsHelper).setNotificationsGranted(true)
            composeTestRule.waitUntil(timeoutMillis = 5_000) {
                permissionsHelper.permissionState.value.hasNotifications
            }
        }

        // Overlay
        (permissionsHelper as FakePermissionsHelper).setOverlayGranted(true)
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            permissionsHelper.permissionState.value.hasSystemAlertWindow
        }

        // Contacts
        (permissionsHelper as FakePermissionsHelper).setContactsGranted(true)
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            permissionsHelper.permissionState.value.hasContacts
        }

        // Location
        (permissionsHelper as FakePermissionsHelper).setLocationGranted(true)
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            permissionsHelper.permissionState.value.hasLocation
        }

        // Not complete until default launcher is set
=======
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
                !currentState || composeTestRule.onAllNodesWithTag("onboarding_step_notifications_button").fetchSemanticsNodes().firstOrNull()?.config?.getOrNull(SemanticsProperties.Disabled) != null
            }
            composeTestRule.waitForIdle()
            Log.d("OnboardingGatingFlowTest", "Notifications button disabled after click")
        }

        // Grant overlay permission
        composeTestRule.onNodeWithTag("onboarding_step_overlay_button").performClick()
        composeTestRule.waitForIdle()

        // Grant contacts permission
        composeTestRule.onNodeWithTag("onboarding_step_contacts_button").performClick()
        composeTestRule.waitForIdle()

        // Grant location permission
        composeTestRule.onNodeWithTag("onboarding_step_location_button").performClick()
        composeTestRule.waitForIdle()

        // Success card should not be visible until default launcher is set
>>>>>>> master
        composeTestRule.onNodeWithTag("onboarding_success_card").assertDoesNotExist()
        composeTestRule.onNodeWithTag("onboarding_incomplete_message").assertExists()

        // Default launcher
        composeTestRule.onNodeWithTag("onboarding_step_default_launcher_button").performClick()
        usageStatsHelper.setDefaultLauncher(true)

        // Verify completion
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            permissionsHelper.permissionState.value.allOnboardingPermissionsGranted &&
                usageStatsHelper.isDefaultLauncher()
        }
    }
}

private class FakePermissionsHelper(
    context: Context,
    notificationsInitiallyGranted: Boolean
) : PermissionsHelper(context) {

    var onDefaultLauncherRequest: (() -> Unit)? = null

    init {
        val initialState = PermissionState(
            hasUsageStats = false,
            hasSystemAlertWindow = false,
            hasNotifications = notificationsInitiallyGranted,
            hasContacts = false,
            hasCallPhone = false,
            hasLocation = false
        )
        overridePermissionState(initialState)
    }

    override fun checkAllPermissions() {
        // Do nothing, state is controlled manually in the test
    }

    override fun requestPermission(activity: Activity, type: PermissionType) {
        when (type) {
            PermissionType.USAGE_STATS -> setUsageStatsGranted(true)
            PermissionType.SYSTEM_ALERT_WINDOW -> setOverlayGranted(true)
            PermissionType.NOTIFICATIONS -> setNotificationsGranted(true)
            PermissionType.DEFAULT_LAUNCHER -> onDefaultLauncherRequest?.invoke()
            PermissionType.CONTACTS -> setContactsGranted(true)
            PermissionType.LOCATION -> setLocationGranted(true)
            PermissionType.CALL_PHONE -> Unit
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

    fun setContactsGranted(granted: Boolean) {
        updateState { it.copy(hasContacts = granted) }
    }

    fun setLocationGranted(granted: Boolean) {
        updateState { it.copy(hasLocation = granted) }
    }

    private fun updateState(transform: (PermissionState) -> PermissionState) {
        overridePermissionState(transform(permissionState.value))
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

