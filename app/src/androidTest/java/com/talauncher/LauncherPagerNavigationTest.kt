package com.talauncher

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.talauncher.data.database.AppDao
import com.talauncher.data.database.AppSessionDao
import com.talauncher.data.database.SettingsDao
import com.talauncher.data.model.AppInfo
import com.talauncher.data.model.AppSession
import com.talauncher.data.model.LauncherSettings
import com.talauncher.data.repository.AppRepository
import com.talauncher.data.repository.SessionRepository
import com.talauncher.data.repository.SettingsRepository
import com.talauncher.ui.theme.TALauncherTheme
import com.talauncher.utils.ContactHelper
import com.talauncher.utils.PermissionState
import com.talauncher.utils.PermissionsHelper
import com.talauncher.utils.UsageStatsHelper
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import kotlin.requireNotNull

@RunWith(AndroidJUnit4::class)
class LauncherPagerNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun pagerNavigation_backHandling_and_launchFlow() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val settingsRepository = SettingsRepository(LauncherFakeSettingsDao())
        val sessionRepository = SessionRepository(FakeAppSessionDao())
        val appRepository = AppRepository(FakeAppDao(), context, settingsRepository, sessionRepository)
        val permissionsHelper = TestPermissionsHelper(context)
        val usageStatsHelper = TestUsageStatsHelper(context)
        val contactHelper = ContactHelper(context, permissionsHelper)

        val recordedAnimations = mutableListOf<Int>()
        var pagerState: androidx.compose.foundation.pager.PagerState? = null

        composeRule.setContent {
            TALauncherTheme {
                LauncherNavigationPager(
                    appRepository = appRepository,
                    settingsRepository = settingsRepository,
                    permissionsHelper = permissionsHelper,
                    usageStatsHelper = usageStatsHelper,
                    sessionRepository = sessionRepository,
                    contactHelper = contactHelper,
                    settingsPageContent = { SettingsPageStub() },
                    homePageContent = { navigateToSettings, launchApp ->
                        HomePageStub(navigateToSettings, launchApp)
                    },
                    pagerStateListener = { pagerState = it },
                    onPageAnimation = { recordedAnimations += it }
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(1, pagerState?.currentPage)
        }
        composeRule.onNodeWithTag("home_page_content").assertExists()

        composeRule.onNodeWithTag("navigate_to_settings_button").performClick()
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertEquals(0, pagerState?.currentPage)
        }
        assertTrue(recordedAnimations.contains(0))

        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertEquals(1, pagerState?.currentPage)
        }
        assertTrue(recordedAnimations.contains(1))

        composeRule.onNodeWithTag("launch_app_button").performClick()
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            val state = requireNotNull(pagerState)
            assertEquals(state.pageCount - 1, state.currentPage)
        }
        assertTrue(recordedAnimations.contains(2))
    }
}

@Composable
private fun SettingsPageStub() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_page_content")
    )
}

@Composable
private fun HomePageStub(
    navigateToSettings: () -> Unit,
    launchApp: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_page_content")
    ) {
        Button(
            onClick = navigateToSettings,
            modifier = Modifier.testTag("navigate_to_settings_button")
        ) {
            Text("Settings")
        }
        Button(
            onClick = launchApp,
            modifier = Modifier.testTag("launch_app_button")
        ) {
            Text("Launch")
        }
    }
}

private class FakeAppDao : AppDao {
    private val appsFlow = MutableStateFlow<List<AppInfo>>(emptyList())
    private val hiddenAppsFlow = MutableStateFlow<List<AppInfo>>(emptyList())
    private val distractingAppsFlow = MutableStateFlow<List<AppInfo>>(emptyList())

    override fun getAllVisibleApps(): Flow<List<AppInfo>> = appsFlow.asStateFlow()

    override fun getHiddenApps(): Flow<List<AppInfo>> = hiddenAppsFlow.asStateFlow()

    override fun getDistractingApps(): Flow<List<AppInfo>> = distractingAppsFlow.asStateFlow()

    override suspend fun getApp(packageName: String): AppInfo? = null

    override suspend fun insertApp(app: AppInfo) {}

    override suspend fun insertApps(apps: List<AppInfo>) {}

    override suspend fun updateApp(app: AppInfo) {}

    override suspend fun deleteApp(app: AppInfo) {}

    override suspend fun deleteApps(apps: List<AppInfo>) {}

    override suspend fun updateHiddenStatus(packageName: String, isHidden: Boolean) {}

    override suspend fun updateDistractingStatus(packageName: String, isDistracting: Boolean) {}

    override suspend fun updateTimeLimit(packageName: String, timeLimit: Int?) {}

    override fun getAllApps(): Flow<List<AppInfo>> = appsFlow.asStateFlow()

    override suspend fun getAllAppsSync(): List<AppInfo> = emptyList()
}

internal class LauncherFakeSettingsDao : SettingsDao {
    private var settings = LauncherSettings()
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

private class FakeAppSessionDao : AppSessionDao {
    private val sessions = MutableStateFlow<List<AppSession>>(emptyList())
    private val idCounter = AtomicLong(0)

    override fun getActiveSessions(): Flow<List<AppSession>> = sessions.asStateFlow()

    override suspend fun getActiveSessionForApp(packageName: String): AppSession? =
        sessions.value.firstOrNull { it.packageName == packageName && it.isActive }

    override suspend fun insertSession(session: AppSession): Long {
        val id = idCounter.incrementAndGet()
        val newSession = session.copy(id = id)
        sessions.value = sessions.value + newSession
        return id
    }

    override suspend fun updateSession(session: AppSession) {
        sessions.value = sessions.value.map { if (it.id == session.id) session else it }
    }

    override suspend fun endSession(sessionId: Long, endTime: Long) {
        sessions.value = sessions.value.map {
            if (it.id == sessionId) {
                it.copy(isActive = false, endTime = endTime)
            } else {
                it
            }
        }
    }

    override suspend fun deleteOldSessions(cutoffTime: Long) {
        sessions.value = sessions.value.filter { it.endTime == null || it.endTime!! >= cutoffTime }
    }
}

private class TestPermissionsHelper(context: Context) : PermissionsHelper(context) {
    override fun checkAllPermissions() {
        overridePermissionState(
            PermissionState(
                hasUsageStats = true,
                hasSystemAlertWindow = true,
                hasNotifications = true,
                hasContacts = true,
                hasCallPhone = true,
                hasLocation = true
            )
        )
    }
}

private class TestUsageStatsHelper(context: Context) : UsageStatsHelper(context) {
    override fun isDefaultLauncher(): Boolean = true
}
