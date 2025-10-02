package com.talauncher.uitests.utils

import android.app.Activity
import android.content.Context
import com.talauncher.utils.PermissionState
import com.talauncher.utils.PermissionType
import com.talauncher.utils.PermissionsHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Mock implementation of PermissionsHelper for UI tests.
 * Allows controlling permission states without requiring actual Android permissions.
 */
class TestPermissionsHelper(
    context: Context,
    initialPermissionState: PermissionState = PermissionState()
) : PermissionsHelper(context) {

    private val _testPermissionState = MutableStateFlow(initialPermissionState)
    override val permissionState: StateFlow<PermissionState> = _testPermissionState.asStateFlow()

    override fun checkAllPermissions() {
        // Do nothing - permissions are controlled manually in tests
    }

    override fun requestPermission(activity: Activity, type: PermissionType) {
        // In tests, automatically grant the permission
        val currentState = _testPermissionState.value
        _testPermissionState.value = when (type) {
            PermissionType.USAGE_STATS -> currentState.copy(hasUsageStats = true)
            PermissionType.NOTIFICATIONS -> currentState.copy(hasNotifications = true)
            PermissionType.CONTACTS -> currentState.copy(hasContacts = true)
            PermissionType.CALL_PHONE -> currentState.copy(hasCallPhone = true)
            PermissionType.LOCATION -> currentState.copy(hasLocation = true)
            PermissionType.DEFAULT_LAUNCHER -> currentState // Handled by UsageStatsHelper
        }
    }

    fun setPermissionState(permissionState: PermissionState) {
        _testPermissionState.value = permissionState
    }

    fun grantPermission(type: PermissionType) {
        val currentState = _testPermissionState.value
        _testPermissionState.value = when (type) {
            PermissionType.USAGE_STATS -> currentState.copy(hasUsageStats = true)
            PermissionType.NOTIFICATIONS -> currentState.copy(hasNotifications = true)
            PermissionType.CONTACTS -> currentState.copy(hasContacts = true)
            PermissionType.CALL_PHONE -> currentState.copy(hasCallPhone = true)
            PermissionType.LOCATION -> currentState.copy(hasLocation = true)
            PermissionType.DEFAULT_LAUNCHER -> currentState
        }
    }

    fun revokePermission(type: PermissionType) {
        val currentState = _testPermissionState.value
        _testPermissionState.value = when (type) {
            PermissionType.USAGE_STATS -> currentState.copy(hasUsageStats = false)
            PermissionType.NOTIFICATIONS -> currentState.copy(hasNotifications = false)
            PermissionType.CONTACTS -> currentState.copy(hasContacts = false)
            PermissionType.CALL_PHONE -> currentState.copy(hasCallPhone = false)
            PermissionType.LOCATION -> currentState.copy(hasLocation = false)
            PermissionType.DEFAULT_LAUNCHER -> currentState
        }
    }

    fun grantAllOnboardingPermissions() {
        _testPermissionState.value = PermissionState(
            hasUsageStats = true,
            hasNotifications = true,
            hasContacts = true,
            hasCallPhone = true,
            hasLocation = true
        )
    }
}
