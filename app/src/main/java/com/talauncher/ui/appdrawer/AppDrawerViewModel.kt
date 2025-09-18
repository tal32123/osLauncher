package com.talauncher.ui.appdrawer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talauncher.R
import com.talauncher.data.model.AppInfo
import com.talauncher.data.model.InstalledApp
import com.talauncher.data.repository.AppRepository
import com.talauncher.data.repository.SettingsRepository
import com.talauncher.utils.PermissionsHelper
import com.talauncher.utils.UsageStatsHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class AppDrawerViewModel(
    private val appRepository: AppRepository,
    private val settingsRepository: SettingsRepository,
    private val usageStatsHelper: UsageStatsHelper,
    private val permissionsHelper: PermissionsHelper,
    private val context: Context? = null,
    private val onLaunchApp: ((String, Int?) -> Unit)? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppDrawerUiState())
    val uiState: StateFlow<AppDrawerUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                appRepository.getAllVisibleApps(),
                appRepository.getHiddenApps(),
                settingsRepository.getSettings()
            ) { visibleApps, hiddenApps, settings ->
                // Get top used apps from usage stats
                val recentLimit = settings?.recentAppsLimit ?: 5
                val recentApps = getRecentApps(visibleApps, recentLimit)

                _uiState.value = _uiState.value.copy(
                    allApps = visibleApps,
                    hiddenApps = hiddenApps,
                    recentApps = recentApps,
                    mathChallengeDifficulty = settings?.mathDifficulty ?: "easy",
                    recentAppsLimit = recentLimit
                )
            }.collect { }
        }
    }

    private suspend fun getRecentApps(allApps: List<AppInfo>, limit: Int): List<AppInfo> {
        if (!usageStatsHelper.hasUsageStatsPermission()) {
            return emptyList()
        }

        val sanitizedLimit = limit.coerceAtLeast(0)
        if (sanitizedLimit == 0) {
            return emptyList()
        }

        val topUsedApps = usageStatsHelper.getTopUsedApps(sanitizedLimit)
        val appMap = allApps.associateBy { it.packageName }

        return topUsedApps.mapNotNull { usageApp ->
            appMap[usageApp.packageName]
        }
    }

    fun launchApp(packageName: String) {
        viewModelScope.launch {
            // Check if we should show time limit prompt first
            if (appRepository.shouldShowTimeLimitPrompt(packageName)) {
                _uiState.value = _uiState.value.copy(
                    showTimeLimitDialog = true,
                    selectedAppForTimeLimit = packageName
                )
                return@launch
            }

            // Try to launch the app through repository (handles friction checks)
            val launched = appRepository.launchApp(packageName)
            if (launched) {
                // App was launched successfully, use callback if available
                onLaunchApp?.invoke(packageName, null)
            } else {
                // Show friction barrier for distracting app
                _uiState.value = _uiState.value.copy(
                    showFrictionDialog = true,
                    selectedAppForFriction = packageName
                )
            }
        }
    }

    fun dismissFrictionDialog() {
        _uiState.value = _uiState.value.copy(
            showFrictionDialog = false,
            selectedAppForFriction = null
        )
    }

    fun launchAppWithReason(packageName: String, reason: String) {
        viewModelScope.launch {
            // Log the reason for analytics/insights if needed
            val launched = appRepository.launchApp(packageName, bypassFriction = true)
            if (launched) {
                onLaunchApp?.invoke(packageName, null)
                dismissFrictionDialog()
            }
        }
    }

    fun dismissTimeLimitDialog() {
        _uiState.value = _uiState.value.copy(
            showTimeLimitDialog = false,
            selectedAppForTimeLimit = null
        )
    }

    fun launchAppWithTimeLimit(packageName: String, durationMinutes: Int) {
        viewModelScope.launch {
            val launched = appRepository.launchApp(
                packageName,
                bypassFriction = true,
                plannedDuration = durationMinutes
            )
            if (launched) {
                onLaunchApp?.invoke(packageName, durationMinutes)
            }
            dismissTimeLimitDialog()
        }
    }

    fun dismissMathChallengeDialog() {
        _uiState.value = _uiState.value.copy(
            showMathChallengeDialog = false,
            selectedAppForMathChallenge = null
        )
    }

    fun showMathChallengeForApp(packageName: String) {
        viewModelScope.launch {
            if (appRepository.shouldShowMathChallenge(packageName)) {
                _uiState.value = _uiState.value.copy(
                    showMathChallengeDialog = true,
                    selectedAppForMathChallenge = packageName
                )
            }
        }
    }

    fun onMathChallengeCompleted(packageName: String) {
        viewModelScope.launch {
            appRepository.endSessionForApp(packageName)
            dismissMathChallengeDialog()
        }
    }

    fun pinApp(packageName: String) {
        viewModelScope.launch {
            appRepository.pinApp(packageName)
        }
    }

    fun unpinApp(packageName: String) {
        viewModelScope.launch {
            appRepository.unpinApp(packageName)
        }
    }

    fun hideApp(packageName: String) {
        viewModelScope.launch {
            appRepository.hideApp(packageName)
        }
    }

    fun unhideApp(packageName: String) {
        viewModelScope.launch {
            appRepository.unhideApp(packageName)
        }
    }

    fun startRenamingApp(app: AppInfo) {
        _uiState.value = _uiState.value.copy(
            appBeingRenamed = app,
            renameInput = app.appName
        )
    }

    fun updateRenameInput(value: String) {
        _uiState.value = _uiState.value.copy(renameInput = value)
    }

    fun dismissRenameDialog() {
        _uiState.value = _uiState.value.copy(
            appBeingRenamed = null,
            renameInput = ""
        )
    }

    fun confirmRename() {
        val app = _uiState.value.appBeingRenamed ?: return
        val newName = _uiState.value.renameInput.trim()
        if (newName.isEmpty()) {
            return
        }

        if (newName == app.appName) {
            dismissRenameDialog()
            return
        }

        viewModelScope.launch {
            appRepository.renameApp(app.packageName, newName)
            dismissRenameDialog()
        }
    }

    fun showAppActionDialog(app: AppInfo) {
        _uiState.value = _uiState.value.copy(selectedAppForAction = app)
    }

    fun dismissAppActionDialog() {
        _uiState.value = _uiState.value.copy(selectedAppForAction = null)
    }

    fun openAppInfo(context: Context, packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            // Handle error - could show a toast or log
        }
    }

    fun uninstallApp(context: Context, packageName: String) {
        val shouldPromptForPermission = permissionsHelper.requiresUninstallPermission()

        try {
            val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
                data = Uri.parse("package:$packageName")
                putExtra(Intent.EXTRA_RETURN_RESULT, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: SecurityException) {
            // System denied the uninstall request. This can happen if the permission was revoked.
            android.util.Log.e("AppDrawerViewModel", "Unable to request uninstall for $packageName", e)
            if (shouldPromptForPermission) {
                notifyPermissionRequired(context)
            }
        } catch (e: Exception) {
            android.util.Log.e("AppDrawerViewModel", "Failed to launch uninstall intent for $packageName", e)
        }
    }

    private fun notifyPermissionRequired(context: Context) {
        val appName = context.getString(R.string.app_name)
        val message = context.getString(R.string.uninstall_permission_required, appName)
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        permissionsHelper.openUninstallPermissionSettings()
    }

    fun performGoogleSearch(query: String) {
        context?.let { ctx ->
            try {
                val searchUrl = "https://www.google.com/search?q=${Uri.encode(query)}"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(intent)
            } catch (e: Exception) {
                Log.e("AppDrawerViewModel", "Failed to open Google search", e)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun clearSearchOnNavigation() {
        _uiState.value = _uiState.value.copy(searchQuery = "")
    }

    fun refreshApps() {
        viewModelScope.launch {
            try {
                appRepository.syncInstalledApps()
            } catch (e: Exception) {
                Log.e("AppDrawerViewModel", "Failed to refresh apps", e)
            }
        }
    }
}

data class AppDrawerUiState(
    val allApps: List<AppInfo> = emptyList(),
    val hiddenApps: List<AppInfo> = emptyList(),
    val recentApps: List<AppInfo> = emptyList(), // Most used apps from past 48 hours respecting user limit
    val isLoading: Boolean = false,
    val selectedAppForAction: AppInfo? = null,
    val appBeingRenamed: AppInfo? = null,
    val renameInput: String = "",
    val showFrictionDialog: Boolean = false,
    val selectedAppForFriction: String? = null,
    val showTimeLimitDialog: Boolean = false,
    val selectedAppForTimeLimit: String? = null,
    val showMathChallengeDialog: Boolean = false,
    val selectedAppForMathChallenge: String? = null,
    val mathChallengeDifficulty: String = "easy",
    val recentAppsLimit: Int = 5,
    val searchQuery: String = ""
)









