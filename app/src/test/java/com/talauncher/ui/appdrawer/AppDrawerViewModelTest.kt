package com.talauncher.ui.appdrawer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.talauncher.data.model.AppInfo
import com.talauncher.data.model.LauncherSettings
import com.talauncher.data.repository.AppRepository
import com.talauncher.data.repository.SettingsRepository
import com.talauncher.utils.PermissionsHelper
import com.talauncher.utils.UsageStatsHelper
import io.mockk.Runs
import io.mockk.any
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class AppDrawerViewModelTest {

    private val appRepository: AppRepository = mockk()
    private val settingsRepository: SettingsRepository = mockk()
    private val usageStatsHelper: UsageStatsHelper = mockk()
    private val permissionsHelper: PermissionsHelper = mockk()

    private val visibleApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val hiddenApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val settingsFlow = MutableStateFlow<LauncherSettings?>(LauncherSettings())

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { appRepository.getAllVisibleApps() } returns visibleApps
        every { appRepository.getHiddenApps() } returns hiddenApps
        every { settingsRepository.getSettings() } returns settingsFlow
        every { usageStatsHelper.hasUsageStatsPermission() } returns false
        coEvery { usageStatsHelper.getTopUsedApps(any()) } returns emptyList()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uninstallApp opens settings when uninstall permission is denied`() = runTest(testDispatcher) {
        every { permissionsHelper.canUninstallOtherApps() } returns false
        val context: Context = mockk()
        val intentSlot = slot<Intent>()
        every { context.startActivity(capture(intentSlot)) } just Runs

        val viewModel = createViewModel()
        advanceUntilIdle()

        val packageName = "com.example.app"
        viewModel.uninstallApp(context, packageName)

        val intent = intentSlot.captured
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
        assertEquals(Uri.parse("package:$packageName"), intent.data)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    private fun createViewModel(): AppDrawerViewModel {
        return AppDrawerViewModel(
            appRepository = appRepository,
            settingsRepository = settingsRepository,
            usageStatsHelper = usageStatsHelper,
            permissionsHelper = permissionsHelper,
            onLaunchApp = null
        )
    }
}
