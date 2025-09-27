package com.talauncher

import android.app.AppOpsManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.talauncher.utils.UsageStatsHelper
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAppOpsManager
import org.junit.Assert.*

@RunWith(RobolectricTestRunner::class)
class UsageStatsHelperTest {

    private lateinit var context: Context
    private lateinit var usageStatsHelper: UsageStatsHelper
    private lateinit var shadowAppOpsManager: ShadowAppOpsManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        shadowAppOpsManager = shadowOf(appOpsManager)
        usageStatsHelper = UsageStatsHelper(context)
    }

    @Test
    fun `getTodayUsageStats returns empty list when permission not granted`() = runTest {
        println("Running getTodayUsageStats returns empty list when permission not granted test")
        val usageStats = usageStatsHelper.getTodayUsageStats(false)
        assertTrue(usageStats.isEmpty())
    }
}
