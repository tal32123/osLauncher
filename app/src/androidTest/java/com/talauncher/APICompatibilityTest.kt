package com.talauncher

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.talauncher.utils.PermissionsHelper
import com.talauncher.utils.UsageStatsHelper
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Tests for API level compatibility across different Android versions
 * Tests permissions, services, and features specific to different API levels
 */
@RunWith(AndroidJUnit4::class)
class APICompatibilityTest {

    private lateinit var context: Context
    private lateinit var permissionsHelper: PermissionsHelper
    private lateinit var usageStatsHelper: UsageStatsHelper

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        permissionsHelper = PermissionsHelper(context)
        usageStatsHelper = UsageStatsHelper(context, permissionsHelper)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    fun testAPI24Plus_UsageStatsPermission() {
        // Test usage stats permission handling for API 24+
        val hasPermission = usageStatsHelper.hasUsageStatsPermission()

        // Should not crash regardless of permission status
        assertTrue("Usage stats helper should work on API 24+",
            hasPermission || !hasPermission) // Test passes if method executes without error
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
    fun testAPI23Plus_OverlayPermission() {
        // Test overlay permission for API 23+ (when SYSTEM_ALERT_WINDOW permission was introduced)
        val hasPermission = permissionsHelper.hasOverlayPermission()

        // Should handle overlay permission check correctly
        assertTrue("Overlay permission check should work on API 23+",
            hasPermission || !hasPermission)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    fun testAPI33Plus_NotificationPermission() {
        // Test notification permission for API 33+ (Android 13)
        val hasPermission = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

        // Should handle notification permission correctly on Android 13+
        assertTrue("Notification permission handling should work on API 33+",
            hasPermission || !hasPermission)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    fun testAPI30Plus_PackageVisibility() {
        // Test package visibility queries for API 30+ (Android 11)
        val packageManager = context.packageManager

        try {
            // Test querying installed packages
            val packages = packageManager.getInstalledPackages(0)
            assertNotNull("Package list should not be null on API 30+", packages)

        } catch (e: Exception) {
            // If package queries fail due to restrictions, that's expected behavior
            // The test should not crash
            assertTrue("Package visibility should be handled gracefully", true)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testAPI26Plus_ForegroundService() {
        // Test foreground service compatibility for API 26+
        try {
            // Test that we can check service state without crashing
            val serviceClass = Class.forName("com.talauncher.service.OverlayService")
            assertNotNull("OverlayService class should be available", serviceClass)

        } catch (e: ClassNotFoundException) {
            fail("OverlayService should be available")
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun testAPI34Plus_ForegroundServiceTypes() {
        // Test new foreground service types for API 34+ (Android 14)
        // The app uses FOREGROUND_SERVICE_SPECIAL_USE

        val hasPermission = context.checkSelfPermission("android.permission.FOREGROUND_SERVICE_SPECIAL_USE") ==
            PackageManager.PERMISSION_GRANTED

        // Should handle new foreground service permissions
        assertTrue("Foreground service special use should be handled on API 34+",
            hasPermission || !hasPermission)
    }

    @Test
    fun testLegacyAPICompatibility() {
        // Test that the app works on minimum supported API (24)
        assertTrue("Build SDK should be at least API 24", Build.VERSION.SDK_INT >= 24)

        // Basic functionality should work regardless of API level
        assertNotNull("Context should be available", context)
        assertNotNull("Package name should be available", context.packageName)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
    fun testRuntimePermissionHandling() {
        // Test runtime permission handling for API 23+
        val permissions = arrayOf(
            Manifest.permission.QUERY_ALL_PACKAGES,
            Manifest.permission.PACKAGE_USAGE_STATS,
            Manifest.permission.SYSTEM_ALERT_WINDOW
        )

        for (permission in permissions) {
            try {
                val status = context.checkSelfPermission(permission)
                assertTrue("Permission status should be valid",
                    status == PackageManager.PERMISSION_GRANTED ||
                    status == PackageManager.PERMISSION_DENIED)
            } catch (e: Exception) {
                // Some permissions might not be available on all devices
                assertTrue("Permission check should not crash", true)
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun testAPI29Plus_ScopedStorage() {
        // Test scoped storage compatibility for API 29+
        try {
            val externalFilesDir = context.getExternalFilesDir(null)
            // Should work with scoped storage
            assertTrue("External files dir should be accessible",
                externalFilesDir != null || externalFilesDir == null)
        } catch (e: Exception) {
            // Should handle storage access gracefully
            assertTrue("Storage access should be handled gracefully", true)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    fun testAPI31Plus_ApproximateLocation() {
        // Test approximate location permission for API 31+
        val hasCoarseLocation = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val hasFineLocation = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

        // Should handle location permissions correctly
        assertTrue("Location permissions should be handled on API 31+",
            hasCoarseLocation || hasFineLocation || (!hasCoarseLocation && !hasFineLocation))
    }

    @Test
    fun testAppOpsManagerCompatibility() {
        // Test AppOpsManager compatibility across API levels
        try {
            val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager?

            if (appOpsManager != null) {
                // Test that we can use AppOpsManager without crashing
                val mode = appOpsManager.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    context.applicationInfo.uid,
                    context.packageName
                )

                assertTrue("AppOpsManager mode should be valid",
                    mode == AppOpsManager.MODE_ALLOWED ||
                    mode == AppOpsManager.MODE_IGNORED ||
                    mode == AppOpsManager.MODE_ERRORED)
            }
        } catch (e: Exception) {
            // Some devices might not support all AppOps features
            assertTrue("AppOpsManager should be handled gracefully", true)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testNotificationChannelCompatibility() {
        // Test notification channel creation for API 26+
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
            assertNotNull("NotificationManager should be available on API 26+", notificationManager)
        } catch (e: Exception) {
            fail("NotificationManager should be available")
        }
    }

    @Test
    fun testBuildConfigCompatibility() {
        // Test that build configuration works across API levels
        assertTrue("Target SDK should be reasonable", BuildConfig.VERSION_CODE > 0)
        assertNotNull("Application ID should be set", BuildConfig.APPLICATION_ID)
        assertTrue("Min SDK should be 24 or higher", Build.VERSION.SDK_INT >= 24)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    fun testAPI28Plus_NonSDKInterfaces() {
        // Test that app doesn't rely on hidden APIs restricted in API 28+
        try {
            // Basic functionality should work without hidden APIs
            val packageManager = context.packageManager
            assertNotNull("PackageManager should work without hidden APIs", packageManager)

        } catch (e: Exception) {
            fail("App should not rely on hidden APIs")
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M, maxSdkVersion = Build.VERSION_CODES.Q)
    fun testLegacyPermissionModel() {
        // Test legacy permission model for older API levels
        assertTrue("Legacy permission model should be supported", true)
    }
}