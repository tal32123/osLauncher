package com.talauncher

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import com.talauncher.data.database.AppDao
import com.talauncher.data.model.AppInfo
import com.talauncher.data.repository.AppRepository
import com.talauncher.data.repository.SessionRepository
import com.talauncher.data.repository.SettingsRepository
import com.talauncher.utils.ErrorHandler
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import kotlinx.coroutines.flow.first
import org.junit.Assert.*

@RunWith(RobolectricTestRunner::class)
class AppRepositoryTest {

    @Mock
    private lateinit var appDao: AppDao

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var packageManager: PackageManager

    @Mock
    private lateinit var settingsRepository: SettingsRepository

    @Mock
    private lateinit var sessionRepository: SessionRepository

    @Mock
    private lateinit var errorHandler: ErrorHandler

    private lateinit var repository: AppRepository

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        whenever(context.packageManager).thenReturn(packageManager)
        repository = AppRepository(
            appDao = appDao,
            context = context,
            settingsRepository = settingsRepository,
            sessionRepository = sessionRepository,
            errorHandler = errorHandler
        )
    }

    @Test
    fun `getAllVisibleApps returns filtered apps`() = runTest {
        val mockApps = listOf(
            createMockApp("com.test.app1", "Test App 1"),
            createMockApp("com.test.app2", "Test App 2")
        )
        whenever(appDao.getAllVisibleApps()).thenReturn(flowOf(mockApps))

        val result = repository.getAllVisibleApps().first()

        assertEquals(2, result.size)
    }

    @Test
    fun `pinApp updates existing app`() = runTest {
        val packageName = "com.test.existing"
        val existingApp = createMockApp(packageName, "Existing App", isPinned = false)

        whenever(appDao.getApp(packageName)).thenReturn(existingApp)
        whenever(appDao.getMaxPinnedOrder()).thenReturn(3)

        repository.pinApp(packageName)

        verify(appDao).updateApp(argThat { app ->
            app.packageName == packageName &&
            app.isPinned &&
            app.pinnedOrder == 4 &&
            !app.isHidden
        })
    }

    private fun createMockApp(
        packageName: String,
        appName: String,
        isHidden: Boolean = false,
        isPinned: Boolean = false,
        pinnedOrder: Int = 0,
        isDistracting: Boolean = false
    ) = AppInfo(
        packageName = packageName,
        appName = appName,
        isHidden = isHidden,
        isPinned = isPinned,
        pinnedOrder = pinnedOrder,
        isDistracting = isDistracting,
        customName = null
    )

    private fun createMockResolveInfo(packageName: String, label: String): ResolveInfo {
        val mockActivityInfo = mock<ActivityInfo>()
        mockActivityInfo.packageName = packageName
        val mockAppInfo = mock<ApplicationInfo>()
        whenever(mockAppInfo.loadLabel(any())).thenReturn(label)
        mockActivityInfo.applicationInfo = mockAppInfo

        return mock<ResolveInfo>().apply {
            activityInfo = mockActivityInfo
        }
    }
}
