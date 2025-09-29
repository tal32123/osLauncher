package com.talauncher

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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
            onDefaultLauncherRequest = {
                setDefaultLauncher(true)
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

        // Usage stats
        composeTestRule.onNodeWithTag("onboarding_step_usage_stats_button").assertIsEnabled()
        composeTestRule.onNodeWithTag("onboarding_step_usage_stats_button").performClick()
        composeTestRule.onNodeWithTag("onboarding_step_usage_stats_button").assertIsNotEnabled()

        // Notifications (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            composeTestRule.onNodeWithTag("onboarding_step_notifications_button").assertIsEnabled()
            composeTestRule.onNodeWithTag("onboarding_step_notifications_button").performClick()
            // Wait for permission state to update
            composeTestRule.waitUntil(timeoutMillis = 10_000) {
                permissionsHelper.permissionState.value.hasNotifications
            }
        }

        // Overlay
        composeTestRule.onNodeWithTag("onboarding_step_overlay_button").performClick()
        if (!permissionsHelper.permissionState.value.hasSystemAlertWindow) {
            (permissionsHelper as FakePermissionsHelper).setOverlayGranted(true)
        }
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            permissionsHelper.permissionState.value.hasSystemAlertWindow
        }

        // Contacts
        composeTestRule.onNodeWithTag("onboarding_step_contacts_button").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            permissionsHelper.permissionState.value.hasContacts
        }

        // Location
        composeTestRule.onNodeWithTag("onboarding_step_location_button").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            permissionsHelper.permissionState.value.hasLocation
        }

        // Not complete until default launcher is set
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

