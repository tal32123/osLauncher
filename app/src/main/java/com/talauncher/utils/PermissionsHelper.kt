package com.talauncher.utils

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.Process

class PermissionsHelper(
    private val context: Context,
    private val sdkIntProvider: () -> Int = { Build.VERSION.SDK_INT }
) {

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val sdkInt = sdkIntProvider()
        val mode = if (sdkInt >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun canUninstallOtherApps(): Boolean {
        val sdkInt = sdkIntProvider()
        if (sdkInt < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return true
        }

        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (sdkInt >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_REQUEST_DELETE_PACKAGES,
                Process.myUid(),
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_REQUEST_DELETE_PACKAGES,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
