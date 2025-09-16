package com.talauncher.utils

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import io.mockk.any
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PermissionsHelperTest {

    private val context: Context = mockk()
    private val appOpsManager: AppOpsManager = mockk()

    @BeforeTest
    fun setUp() {
        every { context.getSystemService(Context.APP_OPS_SERVICE) } returns appOpsManager
        every { context.packageName } returns "com.talauncher.test"
    }

    @AfterTest
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `canUninstallOtherApps returns true for pre Android 14 devices`() {
        val helper = PermissionsHelper(context) { Build.VERSION_CODES.TIRAMISU }

        assertTrue(helper.canUninstallOtherApps())
        verify(exactly = 0) { appOpsManager.unsafeCheckOpNoThrow(any(), any(), any()) }
        verify(exactly = 0) { appOpsManager.checkOpNoThrow(any(), any(), any()) }
    }

    @Test
    fun `canUninstallOtherApps returns true when permission granted on Android 14 plus`() {
        every {
            appOpsManager.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_REQUEST_DELETE_PACKAGES,
                any(),
                any()
            )
        } returns AppOpsManager.MODE_ALLOWED

        val helper = PermissionsHelper(context) { Build.VERSION_CODES.UPSIDE_DOWN_CAKE }

        assertTrue(helper.canUninstallOtherApps())
        verify(exactly = 1) {
            appOpsManager.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_REQUEST_DELETE_PACKAGES,
                any(),
                any()
            )
        }
    }

    @Test
    fun `canUninstallOtherApps returns false when permission denied on Android 14 plus`() {
        every {
            appOpsManager.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_REQUEST_DELETE_PACKAGES,
                any(),
                any()
            )
        } returns AppOpsManager.MODE_ERRORED

        val helper = PermissionsHelper(context) { Build.VERSION_CODES.UPSIDE_DOWN_CAKE }

        assertFalse(helper.canUninstallOtherApps())
    }
}
