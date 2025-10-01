package com.talauncher

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import androidx.core.content.ContextCompat
import com.talauncher.utils.PermissionsHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any as anyArg
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.mockStatic
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

class PermissionsHelperTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var appOpsManager: AppOpsManager

    private val testPackageName = "com.talauncher"

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        whenever(context.packageName).thenReturn(testPackageName)
        whenever(context.getSystemService(Context.APP_OPS_SERVICE)).thenReturn(appOpsManager)
    }

    @Test
    fun `permissionState reflects usage stats permission`() = runTest {
        println("Running permissionState reflects usage stats permission test")
        mockStatic(Process::class.java).use { processMock ->
            processMock.`when`<Int> { Process.myUid() }.thenReturn(TEST_UID)
            mockStatic(ContextCompat::class.java).use { contextCompatMock ->
                contextCompatMock.`when`<Int> { ContextCompat.checkSelfPermission(anyArg(Context::class.java), anyString()) }
                    .thenReturn(PackageManager.PERMISSION_DENIED)
                whenever(appOpsManager.checkOpNoThrow(any(), any(), any())).thenReturn(AppOpsManager.MODE_ALLOWED)
                whenever(appOpsManager.unsafeCheckOpNoThrow(any(), any(), any())).thenReturn(AppOpsManager.MODE_ALLOWED)

                val permissionsHelper = PermissionsHelper(context)
                permissionsHelper.checkAllPermissions()
                val state = permissionsHelper.permissionState.first()

                assertTrue(state.hasUsageStats)
            }
        }
    }

    @Test
    fun `permissionState reflects no usage stats permission`() = runTest {
        println("Running permissionState reflects no usage stats permission test")
        mockStatic(Process::class.java).use { processMock ->
            processMock.`when`<Int> { Process.myUid() }.thenReturn(TEST_UID)
            mockStatic(ContextCompat::class.java).use { contextCompatMock ->
                contextCompatMock.`when`<Int> { ContextCompat.checkSelfPermission(anyArg(Context::class.java), anyString()) }
                    .thenReturn(PackageManager.PERMISSION_DENIED)
                whenever(appOpsManager.checkOpNoThrow(any(), any(), any())).thenReturn(AppOpsManager.MODE_ERRORED)
                whenever(appOpsManager.unsafeCheckOpNoThrow(any(), any(), any())).thenReturn(AppOpsManager.MODE_ERRORED)

                val permissionsHelper = PermissionsHelper(context)
                permissionsHelper.checkAllPermissions()
                val state = permissionsHelper.permissionState.first()

                assertFalse(state.hasUsageStats)
            }
        }
    }

    private companion object {
        const val TEST_UID = 1234
    }
}
