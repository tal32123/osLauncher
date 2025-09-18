package com.talauncher

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.talauncher.utils.PermissionsHelper
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Assert.*

/**
 * Unit tests for PermissionsHelper
 * Tests permission checking and request logic for various Android versions
 */
@RunWith(RobolectricTestRunner::class)
class PermissionsHelperTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var appOpsManager: AppOpsManager

    @Mock
    private lateinit var packageManager: PackageManager

    private lateinit var permissionsHelper: PermissionsHelper

    private val testPackageName = "com.talauncher"

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        whenever(context.packageName).thenReturn(testPackageName)
        whenever(context.getSystemService(Context.APP_OPS_SERVICE)).thenReturn(appOpsManager)
        whenever(context.packageManager).thenReturn(packageManager)

        permissionsHelper = PermissionsHelper(context)
    }

    @Test
    fun `hasUsageStatsPermission returns true when permission granted`() {
        // Given
        whenever(appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            testPackageName
        )).thenReturn(AppOpsManager.MODE_ALLOWED)

        // When
        val result = permissionsHelper.hasUsageStatsPermission()

        // Then
        assertTrue(result)
    }

    @Test
    fun `hasUsageStatsPermission returns false when permission denied`() {
        // Given
        whenever(appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            testPackageName
        )).thenReturn(AppOpsManager.MODE_IGNORED)

        // When
        val result = permissionsHelper.hasUsageStatsPermission()

        // Then
        assertFalse(result)
    }

    @Test
    fun `hasUsageStatsPermission handles null AppOpsManager gracefully`() {
        // Given
        whenever(context.getSystemService(Context.APP_OPS_SERVICE)).thenReturn(null)
        val helperWithNullService = PermissionsHelper(context)

        // When
        val result = helperWithNullService.hasUsageStatsPermission()

        // Then
        assertFalse(result)
    }

    @Test
    fun `requestUsageStatsPermission starts correct intent`() {
        // Given
        val expectedIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

        // When
        permissionsHelper.requestUsageStatsPermission()

        // Then
        verify(context).startActivity(argThat { intent ->
            intent.action == Settings.ACTION_USAGE_ACCESS_SETTINGS &&
            intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0
        })
    }

    @Test
    fun `hasOverlayPermission returns true when permission granted on API 23+`() {
        // Given
        whenever(Settings.canDrawOverlays(context)).thenReturn(true)

        // When
        val result = permissionsHelper.hasOverlayPermission()

        // Then
        assertTrue(result)
    }

    @Test
    fun `hasOverlayPermission returns false when permission denied on API 23+`() {
        // Given
        whenever(Settings.canDrawOverlays(context)).thenReturn(false)

        // When
        val result = permissionsHelper.hasOverlayPermission()

        // Then
        assertFalse(result)
    }

    @Test
    @Config(sdk = [22]) // API level below 23
    fun `hasOverlayPermission returns true on pre-API 23`() {
        // When
        val result = permissionsHelper.hasOverlayPermission()

        // Then
        assertTrue(result) // Should be true on older Android versions
    }

    @Test
    fun `requestOverlayPermission starts correct intent`() {
        // When
        permissionsHelper.requestOverlayPermission()

        // Then
        verify(context).startActivity(argThat { intent ->
            intent.action == Settings.ACTION_MANAGE_OVERLAY_PERMISSION &&
            intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0 &&
            intent.data?.toString()?.contains(testPackageName) == true
        })
    }

    @Test
    @Config(sdk = [33]) // API level 33 (Android 13)
    fun `hasNotificationPermission returns true when granted on API 33+`() {
        // Given
        whenever(ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        )).thenReturn(PackageManager.PERMISSION_GRANTED)

        // When
        val result = permissionsHelper.hasNotificationPermission()

        // Then
        assertTrue(result)
    }

    @Test
    @Config(sdk = [33]) // API level 33 (Android 13)
    fun `hasNotificationPermission returns false when denied on API 33+`() {
        // Given
        whenever(ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        )).thenReturn(PackageManager.PERMISSION_DENIED)

        // When
        val result = permissionsHelper.hasNotificationPermission()

        // Then
        assertFalse(result)
    }

    @Test
    @Config(sdk = [32]) // API level below 33
    fun `hasNotificationPermission returns true on pre-API 33`() {
        // When
        val result = permissionsHelper.hasNotificationPermission()

        // Then
        assertTrue(result) // Should be true on older Android versions
    }

    @Test
    fun `getAllRequiredPermissions returns comprehensive permission list`() {
        // When
        val permissions = permissionsHelper.getAllRequiredPermissions()

        // Then
        assertTrue(permissions.contains("Usage Stats"))
        assertTrue(permissions.contains("System Alert Window"))
        assertTrue(permissions.contains("Notifications"))
        assertEquals(3, permissions.size)
    }

    @Test
    fun `getMissingPermissions returns only missing permissions`() {
        // Given - Only usage stats is missing
        whenever(appOpsManager.checkOpNoThrow(any(), any(), any()))
            .thenReturn(AppOpsManager.MODE_IGNORED)
        whenever(Settings.canDrawOverlays(context)).thenReturn(true)

        // When
        val missingPermissions = permissionsHelper.getMissingPermissions()

        // Then
        assertEquals(1, missingPermissions.size)
        assertTrue(missingPermissions.contains("Usage Stats"))
        assertFalse(missingPermissions.contains("System Alert Window"))
    }

    @Test
    fun `getMissingPermissions returns empty list when all permissions granted`() {
        // Given - All permissions granted
        whenever(appOpsManager.checkOpNoThrow(any(), any(), any()))
            .thenReturn(AppOpsManager.MODE_ALLOWED)
        whenever(Settings.canDrawOverlays(context)).thenReturn(true)

        // When
        val missingPermissions = permissionsHelper.getMissingPermissions()

        // Then
        assertTrue(missingPermissions.isEmpty())
    }

    @Test
    fun `areAllPermissionsGranted returns true when all permissions granted`() {
        // Given
        whenever(appOpsManager.checkOpNoThrow(any(), any(), any()))
            .thenReturn(AppOpsManager.MODE_ALLOWED)
        whenever(Settings.canDrawOverlays(context)).thenReturn(true)

        // When
        val result = permissionsHelper.areAllPermissionsGranted()

        // Then
        assertTrue(result)
    }

    @Test
    fun `areAllPermissionsGranted returns false when any permission missing`() {
        // Given - Overlay permission missing
        whenever(appOpsManager.checkOpNoThrow(any(), any(), any()))
            .thenReturn(AppOpsManager.MODE_ALLOWED)
        whenever(Settings.canDrawOverlays(context)).thenReturn(false)

        // When
        val result = permissionsHelper.areAllPermissionsGranted()

        // Then
        assertFalse(result)
    }

    @Test
    fun `hasPermission handles unknown permission types gracefully`() {
        // When
        val result = permissionsHelper.hasPermission("UNKNOWN_PERMISSION")

        // Then
        assertFalse(result) // Should return false for unknown permissions
    }

    @Test
    fun `requestPermission handles unknown permission types gracefully`() {
        // When - Should not throw exception
        permissionsHelper.requestPermission("UNKNOWN_PERMISSION")

        // Then - No exception should be thrown, and no intent should be started
        verify(context, never()).startActivity(any())
    }

    @Test
    fun `permission requests handle SecurityException gracefully`() {
        // Given
        whenever(context.startActivity(any())).thenThrow(SecurityException("Permission denied"))

        // When - Should not throw exception
        permissionsHelper.requestUsageStatsPermission()

        // Then - Exception should be caught and handled gracefully
        verify(context).startActivity(any())
    }

    @Test
    fun `hasPermission returns correct values for all known permission types`() {
        // Given
        whenever(appOpsManager.checkOpNoThrow(any(), any(), any()))
            .thenReturn(AppOpsManager.MODE_ALLOWED)
        whenever(Settings.canDrawOverlays(context)).thenReturn(true)

        // When & Then
        assertTrue(permissionsHelper.hasPermission("Usage Stats"))
        assertTrue(permissionsHelper.hasPermission("System Alert Window"))
        assertTrue(permissionsHelper.hasPermission("Notifications"))
    }

    @Test
    fun `requestPermission calls appropriate request methods`() {
        // When
        permissionsHelper.requestPermission("Usage Stats")
        permissionsHelper.requestPermission("System Alert Window")

        // Then
        verify(context, times(2)).startActivity(any())
    }

    @Test
    fun `permission checking is case insensitive`() {
        // Given
        whenever(appOpsManager.checkOpNoThrow(any(), any(), any()))
            .thenReturn(AppOpsManager.MODE_ALLOWED)

        // When & Then
        assertTrue(permissionsHelper.hasPermission("usage stats"))
        assertTrue(permissionsHelper.hasPermission("USAGE STATS"))
        assertTrue(permissionsHelper.hasPermission("Usage Stats"))
    }
}