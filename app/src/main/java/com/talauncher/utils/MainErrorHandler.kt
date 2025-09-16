package com.talauncher.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
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
    val onRetry: (() -> Unit)? = null
)

class MainErrorHandler(private val activity: Activity) : ErrorHandler {

    private val _errorState = MutableStateFlow(ErrorState())
    val errorState: StateFlow<ErrorState> = _errorState.asStateFlow()

    private var permissionCallback: ((Boolean) -> Unit)? = null

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    override fun showError(title: String, message: String, throwable: Throwable?, onRetry: (() -> Unit)?) {
        val stackTrace = throwable?.let { ErrorUtils.getStackTraceString(it) }
        _errorState.value = ErrorState(
            isVisible = true,
            title = title,
            message = message,
            stackTrace = stackTrace,
            onRetry = onRetry
        )
    }

    override fun requestPermission(permission: String, rationale: String, onResult: (Boolean) -> Unit) {
        // Check if permission is already granted
        if (ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED) {
            onResult(true)
            return
        }

        // Store callback for later use
        permissionCallback = onResult

        // Show rationale if needed
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            showError(
                "Permission Required",
                rationale,
                null,
                onRetry = {
                    requestPermissionInternal(permission)
                }
            )
        } else {
            requestPermissionInternal(permission)
        }
    }

    private fun requestPermissionInternal(permission: String) {
        ActivityCompat.requestPermissions(activity, arrayOf(permission), PERMISSION_REQUEST_CODE)
    }

    fun dismissError() {
        _errorState.value = ErrorState()
    }

    fun onPermissionResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            permissionCallback?.invoke(granted)
            permissionCallback = null
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