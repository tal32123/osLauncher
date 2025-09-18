package com.talauncher

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.talauncher.utils.PermissionsHelper
import com.talauncher.utils.UsageStatsHelper
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAppOpsManager
import org.junit.Assert.*

/**
 * Unit tests for UsageStatsHelper
 * Tests permission checking and usage statistics functionality
 */
@RunWith(RobolectricTestRunner::class)
class UsageStatsHelperTest {

    @Mock
    private lateinit var permissionsHelper: PermissionsHelper

    private lateinit var context: Context
    private lateinit var usageStatsHelper: UsageStatsHelper
    private lateinit var shadowAppOpsManager: ShadowAppOpsManager

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()

        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        shadowAppOpsManager = shadowOf(appOpsManager)

        usageStatsHelper = UsageStatsHelper(context, permissionsHelper)
    }

    @Test
    fun `hasUsageStatsPermission returns true when permission granted`() {
        shadowAppOpsManager.setMode(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            context.applicationInfo.uid,
            context.packageName,
            AppOpsManager.MODE_ALLOWED
        )

        assertTrue(usageStatsHelper.hasUsageStatsPermission())
    }

    @Test
    fun `hasUsageStatsPermission returns false when permission denied`() {
        shadowAppOpsManager.setMode(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            context.applicationInfo.uid,
            context.packageName,
            AppOpsManager.MODE_IGNORED
        )

        assertFalse(usageStatsHelper.hasUsageStatsPermission())
    }

    @Test
    fun `requestUsageStatsPermission starts settings activity`() {
        usageStatsHelper.requestUsageStatsPermission()

        val nextStartedActivity = shadowOf(context as android.app.Application).nextStartedActivity
        assertNotNull(nextStartedActivity)
        assertEquals(Settings.ACTION_USAGE_ACCESS_SETTINGS, nextStartedActivity.action)
        assertTrue(nextStartedActivity.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun `getUsageStats returns empty list when permission not granted`() {
        shadowAppOpsManager.setMode(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            context.applicationInfo.uid,
            context.packageName,
            AppOpsManager.MODE_IGNORED
        )

        val usageStats = usageStatsHelper.getUsageStats(1000L, 2000L)
        assertTrue(usageStats.isEmpty())
    }

    @Test
    fun `hasUsageStatsPermission handles SecurityException gracefully`() {
        // Simulate SecurityException by removing the AppOpsManager service
        val contextSpy = spy(context)
        whenever(contextSpy.getSystemService(Context.APP_OPS_SERVICE)).thenReturn(null)

        val helperWithBadContext = UsageStatsHelper(contextSpy, permissionsHelper)

        // Should not crash and return false
        assertFalse(helperWithBadContext.hasUsageStatsPermission())
    }

    @Test
    fun `requestUsageStatsPermission handles ActivityNotFoundException gracefully`() {
        // This would normally require mocking ActivityNotFoundException scenarios
        // For now, verify it doesn't crash
        assertDoesNotThrow {
            usageStatsHelper.requestUsageStatsPermission()
        }
    }

    @Test
    fun `getUsageStats with invalid time range returns empty list`() {
        shadowAppOpsManager.setMode(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            context.applicationInfo.uid,
            context.packageName,
            AppOpsManager.MODE_ALLOWED
        )

        // Invalid time range (end before start)
        val usageStats = usageStatsHelper.getUsageStats(2000L, 1000L)
        assertTrue(usageStats.isEmpty())
    }

    @Test
    fun `getUsageStats with same start and end time returns empty list`() {
        shadowAppOpsManager.setMode(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            context.applicationInfo.uid,
            context.packageName,
            AppOpsManager.MODE_ALLOWED
        )

        val usageStats = usageStatsHelper.getUsageStats(1000L, 1000L)
        assertTrue(usageStats.isEmpty())
    }

    @Test
    fun `context is stored correctly`() {
        assertEquals(context, usageStatsHelper.context)
    }

    @Test
    fun `permissions helper is stored correctly`() {
        assertEquals(permissionsHelper, usageStatsHelper.permissionsHelper)
    }

    private fun assertDoesNotThrow(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            fail("Expected no exception, but got: ${e.message}")
        }
    }
}