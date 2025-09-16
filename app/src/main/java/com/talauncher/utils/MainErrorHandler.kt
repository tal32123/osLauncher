package com.talauncher.utils

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ErrorState(
    val isVisible: Boolean = false,
    val title: String = "",
    val message: String = "",
    val stackTrace: String? = null,
    val onRetry: (() -> Unit)? = null,
    val retryButtonText: String = "Retry"
)

class MainErrorHandler(private val activity: Activity) : ErrorHandler {

    private val _errorState = MutableStateFlow(ErrorState())
    val errorState: StateFlow<ErrorState> = _errorState.asStateFlow()

    private data class PermissionRequest(
        val permission: String,
        val rationale: String,
        val onResult: (Boolean) -> Unit
    )

    private var currentPermissionRequest: PermissionRequest? = null

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    override fun showError(
        title: String,
        message: String,
        throwable: Throwable?,
        onRetry: (() -> Unit)?,
        retryButtonText: String
    ) {
        val stackTrace = throwable?.let { ErrorUtils.getStackTraceString(it) }
        _errorState.value = ErrorState(
            isVisible = true,
            title = title,
            message = message,
            stackTrace = stackTrace,
            onRetry = onRetry,
            retryButtonText = retryButtonText
        )
    }

    override fun requestPermission(permission: String, rationale: String, onResult: (Boolean) -> Unit) {
        // Check if permission is already granted
        if (ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED) {
            onResult(true)
            return
        }

        // Track the active permission request so we can handle retries or permanent denials
        currentPermissionRequest = PermissionRequest(permission, rationale, onResult)

        // Show rationale if needed
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            showError(
                "Permission Required",
                rationale,
                null,
                onRetry = {
                    requestPermissionInternal(permission)
                },
                retryButtonText = "Try Again"
            )
        } else {
            requestPermissionInternal(permission)
        }
    }

    private fun requestPermissionInternal(permission: String) {
        dismissError()
        ActivityCompat.requestPermissions(activity, arrayOf(permission), PERMISSION_REQUEST_CODE)
    }

    fun dismissError() {
        _errorState.value = ErrorState()
    }

    fun onPermissionResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode != PERMISSION_REQUEST_CODE) {
            return
        }

        val request = currentPermissionRequest ?: return
        if (permissions.isEmpty() || permissions[0] != request.permission) {
            return
        }

        val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        if (granted) {
            request.onResult(true)
            currentPermissionRequest = null
            dismissError()
            return
        }

        request.onResult(false)

        val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, request.permission)
        if (shouldShowRationale) {
            showError(
                "Permission Required",
                request.rationale,
                null,
                onRetry = {
                    requestPermissionInternal(request.permission)
                },
                retryButtonText = "Try Again"
            )
        } else {
            showError(
                "Permission Required",
                request.rationale + "\n\nEnable this permission in system settings to continue.",
                null,
                onRetry = {
                    openAppSettings()
                },
                retryButtonText = "Open Settings"
            )
        }
    }

    private fun openAppSettings() {
        dismissError()
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
        }
        if (intent.resolveActivity(activity.packageManager) != null) {
            activity.startActivity(intent)
        }
    }

    // Helper function to check common permissions needed for package management
    fun checkAndRequestPackageQueryPermission(onResult: (Boolean) -> Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // For Android 11+, we might need QUERY_ALL_PACKAGES permission for some operations
            // This is a sensitive permission that needs to be declared in manifest
            // and may require approval from Google Play Store
            onResult(true) // For now, assume we have necessary permissions
        } else {
            onResult(true)
        }
    }
}