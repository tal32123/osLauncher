package com.talauncher

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import com.talauncher.utils.PermissionsHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import org.junit.Assert.*

class PermissionsHelperTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var appOpsManager: AppOpsManager

    private lateinit var permissionsHelper: PermissionsHelper

    private val testPackageName = "com.talauncher"

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        whenever(context.packageName).thenReturn(testPackageName)
        whenever(context.getSystemService(Context.APP_OPS_SERVICE)).thenReturn(appOpsManager)
        permissionsHelper = PermissionsHelper(context)
    }

    @Test
    fun `permissionState reflects usage stats permission`() = runTest {
        println("Running permissionState reflects usage stats permission test")
        whenever(appOpsManager.checkOpNoThrow(any(), any(), any())).thenReturn(AppOpsManager.MODE_ALLOWED)

        permissionsHelper.checkAllPermissions()
        val state = permissionsHelper.permissionState.first()

        assertTrue(state.hasUsageStats)
    }

    @Test
    fun `permissionState reflects no usage stats permission`() = runTest {
        println("Running permissionState reflects no usage stats permission test")
        whenever(appOpsManager.checkOpNoThrow(any(), any(), any())).thenReturn(AppOpsManager.MODE_ERRORED)

        permissionsHelper.checkAllPermissions()
        val state = permissionsHelper.permissionState.first()

        assertFalse(state.hasUsageStats)
    }
}
