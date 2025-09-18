package com.talauncher

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.test.core.app.ApplicationProvider
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

/**
 * Unit tests for AppRepository
 * Tests core business logic for app management, filtering, and launching
 */
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
        // Given
        val mockApps = listOf(
            createMockApp("com.test.app1", "Test App 1", isHidden = false),
            createMockApp("com.test.app2", "Test App 2", isHidden = false)
        )
        whenever(appDao.getAllVisibleApps()).thenReturn(flowOf(mockApps))

        // When
        val result = repository.getAllVisibleApps().first()

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { !it.isHidden })
    }

    @Test
    fun `getPinnedApps returns only pinned apps`() = runTest {
        // Given
        val mockApps = listOf(
            createMockApp("com.test.app1", "Test App 1", isPinned = true, pinnedOrder = 1),
            createMockApp("com.test.app2", "Test App 2", isPinned = true, pinnedOrder = 2)
        )
        whenever(appDao.getPinnedApps()).thenReturn(flowOf(mockApps))

        // When
        val result = repository.getPinnedApps().first()

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.isPinned })
        assertEquals(1, result[0].pinnedOrder)
        assertEquals(2, result[1].pinnedOrder)
    }

    @Test
    fun `pinApp updates existing app`() = runTest {
        // Given
        val packageName = "com.test.existing"
        val existingApp = createMockApp(packageName, "Existing App", isPinned = false)

        whenever(appDao.getApp(packageName)).thenReturn(existingApp)
        whenever(appDao.getMaxPinnedOrder()).thenReturn(3)

        // When
        repository.pinApp(packageName)

        // Then
        verify(appDao).updateApp(argThat { app ->
            app.packageName == packageName &&
            app.isPinned &&
            app.pinnedOrder == 4 &&
            !app.isHidden
        })
    }

    @Test
    fun `unpinApp removes pin status`() = runTest {
        // Given
        val packageName = "com.test.pinned"

        // When
        repository.unpinApp(packageName)

        // Then
        verify(appDao).updatePinnedStatus(packageName, false, 0)
    }

    @Test
    fun `hideApp sets isHidden to true`() = runTest {
        // Given
        val packageName = "com.test.visible"

        // When
        repository.hideApp(packageName)

        // Then
        verify(appDao).updateHiddenStatus(packageName, true)
    }

    @Test
    fun `unhideApp sets isHidden to false`() = runTest {
        // Given
        val packageName = "com.test.hidden"

        // When
        repository.unhideApp(packageName)

        // Then
        verify(appDao).updateHiddenStatus(packageName, false)
    }

    @Test
    fun `updateDistractingStatus updates distracting field`() = runTest {
        // Given
        val packageName = "com.test.app"
        val isDistracting = true

        // When
        repository.updateDistractingStatus(packageName, isDistracting)

        // Then
        verify(appDao).updateDistractingStatus(packageName, isDistracting)
    }

    @Test
    fun `launchApp starts session when planned duration provided`() = runTest {
        // Given
        val packageName = "com.test.app"
        val plannedDuration = 30
        val mockIntent = mock<android.content.Intent>()

        whenever(packageManager.getLaunchIntentForPackage(packageName)).thenReturn(mockIntent)

        // When
        val result = repository.launchApp(packageName, plannedDuration = plannedDuration)

        // Then
        assertTrue(result)
        verify(sessionRepository).startSession(packageName, plannedDuration)
        verify(context).startActivity(mockIntent)
    }

    @Test
    fun `launchApp handles missing launch intent gracefully`() = runTest {
        // Given
        val packageName = "com.test.nolaunch"

        whenever(packageManager.getLaunchIntentForPackage(packageName)).thenReturn(null)

        // When
        val result = repository.launchApp(packageName)

        // Then
        assertFalse(result)
        verify(errorHandler)?.showError("Error", "Unable to launch this app")
    }

    @Test
    fun `syncInstalledApps removes uninstalled apps from database`() = runTest {
        // Given
        val installedApps = listOf(
            createMockResolveInfo("com.test.app1", "App 1"),
            createMockResolveInfo("com.test.app2", "App 2")
        )
        val storedApps = listOf(
            createMockApp("com.test.app1", "App 1"),
            createMockApp("com.test.app2", "App 2"),
            createMockApp("com.test.oldapp", "Old App") // This should be removed
        )

        whenever(packageManager.queryIntentActivities(any(), any<Int>())).thenReturn(installedApps)
        whenever(appDao.getAllAppsSync()).thenReturn(storedApps)

        // When
        repository.syncInstalledApps()

        // Then
        verify(appDao).deleteApp(argThat { it.packageName == "com.test.oldapp" })
        verify(appDao, never()).deleteApp(argThat { it.packageName == "com.test.app1" })
        verify(appDao, never()).deleteApp(argThat { it.packageName == "com.test.app2" })
    }

    @Test
    fun `getApp returns correct app from dao`() = runTest {
        // Given
        val packageName = "com.test.app"
        val expectedApp = createMockApp(packageName, "Test App")
        whenever(appDao.getApp(packageName)).thenReturn(expectedApp)

        // When
        val result = repository.getApp(packageName)

        // Then
        assertEquals(expectedApp, result)
    }

    @Test
    fun `insertApp delegates to dao`() = runTest {
        // Given
        val app = createMockApp("com.test.new", "New App")

        // When
        repository.insertApp(app)

        // Then
        verify(appDao).insertApp(app)
    }

    @Test
    fun `renameApp updates app name`() = runTest {
        // Given
        val packageName = "com.test.app"
        val newName = "New App Name"

        // When
        repository.renameApp(packageName, newName)

        // Then
        verify(appDao).updateApp(any())
    }

    @Test
    fun `getAppDisplayName returns correct display name`() = runTest {
        // Given
        val packageName = "com.test.app"
        val expectedName = "Test App"
        val mockApp = createMockApp(packageName, expectedName)
        whenever(appDao.getApp(packageName)).thenReturn(mockApp)

        // When
        val result = repository.getAppDisplayName(packageName)

        // Then
        assertEquals(expectedName, result)
    }

    @Test
    fun `getAllAppsSync returns all apps synchronously`() = runTest {
        // Given
        val expectedApps = listOf(
            createMockApp("com.test.app1", "App 1"),
            createMockApp("com.test.app2", "App 2")
        )
        whenever(appDao.getAllAppsSync()).thenReturn(expectedApps)

        // When
        val result = repository.getAllAppsSync()

        // Then
        assertEquals(expectedApps, result)
    }

    // Helper methods
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
        isDistracting = isDistracting
    )

    private fun createMockApplicationInfo(packageName: String, label: String): ApplicationInfo {
        return mock<ApplicationInfo>().apply {
            this.packageName = packageName
        }
    }

    private fun createMockResolveInfo(packageName: String, label: String): ResolveInfo {
        val mockActivityInfo = mock<ActivityInfo>()
        mockActivityInfo.packageName = packageName
        mockActivityInfo.applicationInfo = createMockApplicationInfo(packageName, label)

        return mock<ResolveInfo>().apply {
            activityInfo = mockActivityInfo
        }
    }
}