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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.junit.Assert.assertEquals

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
        whenever(context.applicationContext).thenReturn(context)
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
        println("Running getAllVisibleApps returns filtered apps test")
        val mockApps = listOf(
            createMockApp("com.test.app1", "Test App 1"),
            createMockApp("com.test.app2", "Test App 2")
        )
        whenever(appDao.getAllVisibleApps()).thenReturn(flowOf(mockApps))

        val result = repository.getAllVisibleApps().first()

        assertEquals(2, result.size)
    }

    @Test
    fun `hideApp marks existing app as hidden`() = runTest {
        println("Running hideApp marks existing app as hidden test")
        val packageName = "com.test.existing"
        val existingApp = createMockApp(packageName, "Existing App")

        whenever(appDao.getApp(packageName)).thenReturn(existingApp)

        repository.hideApp(packageName)

        verify(appDao).updateHiddenStatus(packageName, true)
        verify(appDao, never()).insertApp(any())
    }

    private fun createMockApp(
        packageName: String,
        appName: String,
        isHidden: Boolean = false,
        isDistracting: Boolean = false,
        timeLimitMinutes: Int? = null
    ) = AppInfo(
        packageName = packageName,
        appName = appName,
        isHidden = isHidden,
        isDistracting = isDistracting,
        timeLimitMinutes = timeLimitMinutes
    )

    private fun createMockResolveInfo(packageName: String, label: String): ResolveInfo {
        val mockActivityInfo = ActivityInfo()
        mockActivityInfo.packageName = packageName
        mockActivityInfo.name = label
        val mockAppInfo = ApplicationInfo().apply {
            this.packageName = packageName
        }
        return ResolveInfo().apply {
            activityInfo = mockActivityInfo.apply {
                applicationInfo = mockAppInfo
            }
        }
    }
}
