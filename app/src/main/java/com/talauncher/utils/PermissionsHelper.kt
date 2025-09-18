package com.talauncher.utils

import android.Manifest
import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PermissionState(
    val hasUsageStats: Boolean = false,
    val hasSystemAlertWindow: Boolean = false,
    val hasNotifications: Boolean = false,
    val hasContacts: Boolean = false,
    val hasCallPhone: Boolean = false
) {
    val allOnboardingPermissionsGranted: Boolean
        get() = hasUsageStats && hasSystemAlertWindow && hasNotifications
}

enum class PermissionType {
    USAGE_STATS,
    SYSTEM_ALERT_WINDOW,
    NOTIFICATIONS,
    CONTACTS,
    CALL_PHONE,
    DEFAULT_LAUNCHER
}

class PermissionsHelper(
    private val context: Context
) {

    private val _permissionState = MutableStateFlow(PermissionState())
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    init {
        checkAllPermissions()
    }

    fun checkAllPermissions() {
        _permissionState.value = PermissionState(
            hasUsageStats = hasUsageStatsPermission(),
            hasSystemAlertWindow = hasSystemAlertWindowPermission(),
            hasNotifications = hasNotificationPermission(),
            hasContacts = hasContactsPermission(),
            hasCallPhone = hasCallPhonePermission()
        )
    }

    fun requestPermission(activity: Activity, type: PermissionType) {
        when (type) {
            PermissionType.USAGE_STATS -> openUsageAccessSettings()
            PermissionType.SYSTEM_ALERT_WINDOW -> requestSystemAlertWindowPermission()
            PermissionType.NOTIFICATIONS -> requestNotificationPermission(activity)
            PermissionType.CONTACTS -> requestContactsPermission(activity)
            PermissionType.CALL_PHONE -> requestCallPhonePermission(activity)
            PermissionType.DEFAULT_LAUNCHER -> openDefaultLauncherSettings()
        }
    }

    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkAllPermissions()
        }
    }

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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

    private fun hasSystemAlertWindowPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true // Permission not required for API < 23
        }
    }

    private fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasCallPhonePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestContactsPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.READ_CONTACTS),
            CONTACTS_PERMISSION_REQUEST_CODE
        )
    }

    private fun requestCallPhonePermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.CALL_PHONE),
            CALL_PHONE_PERMISSION_REQUEST_CODE
        )
    }

    private fun requestSystemAlertWindowPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            }
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            true
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun openUsageAccessSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun openDefaultLauncherSettings() {
        val packageManager = context.packageManager
        val candidateActions = listOf(
            Settings.ACTION_HOME_SETTINGS,
            Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS,
            Settings.ACTION_SETTINGS
        )

        val targetIntent = candidateActions
            .map { action ->
                Intent(action).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            }
            .firstOrNull { intent ->
                intent.resolveActivity(packageManager) != null
            }

        if (targetIntent != null) {
            context.startActivity(targetIntent)
        }
    }

    companion object {
        const val CONTACTS_PERMISSION_REQUEST_CODE = 1001
        const val CALL_PHONE_PERMISSION_REQUEST_CODE = 1002
        const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1003
    }
}
