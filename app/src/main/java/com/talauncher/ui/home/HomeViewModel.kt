package com.talauncher.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talauncher.data.model.AppInfo
import com.talauncher.data.model.AppSession
import com.talauncher.data.repository.AppRepository
import com.talauncher.data.repository.SettingsRepository
import com.talauncher.data.repository.SessionRepository
import com.talauncher.service.OverlayService
import com.talauncher.utils.PermissionsHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale

class HomeViewModel(
    private val appRepository: AppRepository,
    private val settingsRepository: SettingsRepository,
    private val onLaunchApp: ((String, Int?) -> Unit)? = null,
    private val sessionRepository: SessionRepository? = null,
    private val context: Context? = null,
    private val permissionsHelper: PermissionsHelper? = null
) : ViewModel() {

    private enum class TimeLimitRequestSource { STANDARD, SESSION_EXTENSION }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
    private val pendingExpiredSessions = ArrayDeque<AppSession>()
    private var currentExpiredSession: AppSession? = null
    private var countdownJob: Job? = null
    private var timeLimitRequestSource = TimeLimitRequestSource.STANDARD
    private var overlayPermissionPrompted = false

    private val sessionExpiryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.talauncher.SESSION_EXPIRY_EXTEND" -> {
                    onSessionExpiryDecisionExtend()
                }
                "com.talauncher.SESSION_EXPIRY_CLOSE" -> {
                    onSessionExpiryDecisionClose()
                }
                "com.talauncher.SESSION_EXPIRY_MATH_CHALLENGE" -> {
                    onSessionExpiryDecisionMathChallenge()
                }
            }
        }
    }

    init {
        observeData()
        updateTime()
        observeSessionExpirations()
        checkExpiredSessions()
        setupBroadcastReceiver()
    }

    private fun setupBroadcastReceiver() {
        context?.let { ctx ->
            val filter = IntentFilter().apply {
                addAction("com.talauncher.SESSION_EXPIRY_EXTEND")
                addAction("com.talauncher.SESSION_EXPIRY_CLOSE")
                addAction("com.talauncher.SESSION_EXPIRY_MATH_CHALLENGE")
            }
            ContextCompat.registerReceiver(
                ctx,
                sessionExpiryReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                appRepository.getPinnedApps(),
                settingsRepository.getSettings()
            ) { pinnedApps, settings ->
                _uiState.value = _uiState.value.copy(
                    pinnedApps = pinnedApps,
                    showTime = settings?.showTimeOnHomeScreen ?: true,
                    showDate = settings?.showDateOnHomeScreen ?: true,
                    showWallpaper = settings?.showWallpaper ?: true,
                    backgroundColor = settings?.backgroundColor ?: "system"
                )
            }.collect { /* Data collection handled in the combine block */ }
        }
    }

    private fun updateTime() {
        viewModelScope.launch {
            val now = Date()
            _uiState.value = _uiState.value.copy(
                currentTime = timeFormat.format(now),
                currentDate = dateFormat.format(now)
            )
        }
    }

    fun launchApp(packageName: String) {
        viewModelScope.launch {
            if (appRepository.shouldShowTimeLimitPrompt(packageName)) {
                _uiState.value = _uiState.value.copy(
                    showTimeLimitDialog = true,
                    selectedAppForTimeLimit = packageName
                )
                timeLimitRequestSource = TimeLimitRequestSource.STANDARD
                return@launch
            }

            val launched = appRepository.launchApp(packageName)
            if (launched) {
                onLaunchApp?.invoke(packageName, null)
            } else {
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
            appRepository.launchApp(packageName, bypassFriction = true)
            onLaunchApp?.invoke(packageName, null)
            dismissFrictionDialog()
        }
    }

    fun dismissTimeLimitDialog() {
        val wasExtension = timeLimitRequestSource == TimeLimitRequestSource.SESSION_EXTENSION
        timeLimitRequestSource = TimeLimitRequestSource.STANDARD
        _uiState.value = _uiState.value.copy(
            showTimeLimitDialog = false,
            selectedAppForTimeLimit = null
        )
        if (wasExtension && currentExpiredSession != null) {
            _uiState.value = _uiState.value.copy(showSessionExpiryDecisionDialog = true)
        }
    }

    fun launchAppWithTimeLimit(packageName: String, durationMinutes: Int) {
        viewModelScope.launch {
            val isExtension = timeLimitRequestSource == TimeLimitRequestSource.SESSION_EXTENSION
            val launched = appRepository.launchApp(
                packageName,
                bypassFriction = true,
                plannedDuration = durationMinutes
            )

            if (launched) {
                onLaunchApp?.invoke(packageName, durationMinutes)
                if (isExtension) {
                    _uiState.value = _uiState.value.copy(
                        showTimeLimitDialog = false,
                        selectedAppForTimeLimit = null
                    )
                    timeLimitRequestSource = TimeLimitRequestSource.STANDARD
                    finalizeExpiredSession()
                } else {
                    dismissTimeLimitDialog()
                }
            } else if (isExtension) {
                _uiState.value = _uiState.value.copy(
                    showTimeLimitDialog = false,
                    selectedAppForTimeLimit = null,
                    showSessionExpiryDecisionDialog = true
                )
                timeLimitRequestSource = TimeLimitRequestSource.STANDARD
            }
        }
    }

    fun dismissMathChallengeDialog() {
        viewModelScope.launch {
            val wasExpiredSession = _uiState.value.isMathChallengeForExpiredSession
            val wasExtensionChallenge = _uiState.value.isMathChallengeForSessionExtension
            val packageName = _uiState.value.selectedAppForMathChallenge

            if (wasExpiredSession && packageName != null) {
                appRepository.closeCurrentApp()
                appRepository.endSessionForApp(packageName)
                finalizeExpiredSession()
            }

            _uiState.value = _uiState.value.copy(
                showMathChallengeDialog = false,
                selectedAppForMathChallenge = null,
                isMathChallengeForExpiredSession = false,
                isMathChallengeForSessionExtension = false
            )

            if (wasExtensionChallenge && currentExpiredSession != null) {
                _uiState.value = _uiState.value.copy(showSessionExpiryDecisionDialog = true)
            }
        }
    }

    fun showMathChallengeForApp(packageName: String) {
        viewModelScope.launch {
            if (appRepository.shouldShowMathChallenge(packageName)) {
                _uiState.value = _uiState.value.copy(
                    showMathChallengeDialog = true,
                    selectedAppForMathChallenge = packageName,
                    isMathChallengeForExpiredSession = false,
                    isMathChallengeForSessionExtension = false
                )
            }
        }
    }

    fun onMathChallengeCompleted(packageName: String) {
        viewModelScope.launch {
            if (_uiState.value.isMathChallengeForSessionExtension) {
                _uiState.value = _uiState.value.copy(
                    showMathChallengeDialog = false,
                    selectedAppForMathChallenge = null,
                    isMathChallengeForSessionExtension = false,
                    isMathChallengeForExpiredSession = false,
                    showSessionExpiryDecisionDialog = false,
                    showTimeLimitDialog = true,
                    selectedAppForTimeLimit = packageName
                )
                timeLimitRequestSource = TimeLimitRequestSource.SESSION_EXTENSION
            } else {
                appRepository.endSessionForApp(packageName)
                _uiState.value = _uiState.value.copy(
                    showMathChallengeDialog = false,
                    selectedAppForMathChallenge = null,
                    isMathChallengeForExpiredSession = false,
                    isMathChallengeForSessionExtension = false
                )
            }
        }
    }

    fun refreshTime() {
        updateTime()
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

    fun onSessionExpiryDecisionExtend() {
        val packageName = currentExpiredSession?.packageName ?: return
        timeLimitRequestSource = TimeLimitRequestSource.SESSION_EXTENSION
        _uiState.value = _uiState.value.copy(
            showSessionExpiryDecisionDialog = false,
            showTimeLimitDialog = true,
            selectedAppForTimeLimit = packageName
        )
    }

    fun onSessionExpiryDecisionClose() {
        viewModelScope.launch {
            currentExpiredSession?.let { session ->
                appRepository.closeCurrentApp()
                appRepository.endSessionForApp(session.packageName)
            }
            finalizeExpiredSession()
        }
    }

    fun onSessionExpiryDecisionMathChallenge() {
        viewModelScope.launch {
            val packageName = currentExpiredSession?.packageName ?: return@launch
            val settings = settingsRepository.getSettingsSync()

            // Only show math challenge if it's enabled in settings
            if (settings.enableMathChallenge) {
                _uiState.value = _uiState.value.copy(
                    showSessionExpiryDecisionDialog = false,
                    showMathChallengeDialog = true,
                    selectedAppForMathChallenge = packageName,
                    isMathChallengeForSessionExtension = true,
                    isMathChallengeForExpiredSession = false
                )
            } else {
                // If math challenge is disabled, fallback to close the app
                currentExpiredSession?.let { session ->
                    appRepository.endSessionForApp(session.packageName)
                }
                finalizeExpiredSession()
            }
        }
    }

    private fun observeSessionExpirations() {
        val repository = sessionRepository ?: return
        viewModelScope.launch {
            repository.observeSessionExpirations().collect { session ->
                enqueueExpiredSession(session)
            }
        }
    }

    private fun enqueueExpiredSession(session: AppSession) {
        if (currentExpiredSession != null) {
            pendingExpiredSessions.addLast(session)
            return
        }
        processExpiredSession(session)
    }

    private fun processExpiredSession(session: AppSession) {
        if (sessionRepository == null) return
        currentExpiredSession = session
        countdownJob?.cancel()

        viewModelScope.launch {
            val settings = settingsRepository.getSettingsSync()
            val countdownSeconds = settings.sessionExpiryCountdownSeconds.coerceAtLeast(0)
            val appName = appRepository.getAppDisplayName(session.packageName)

            _uiState.value = _uiState.value.copy(
                sessionExpiryAppName = appName,
                sessionExpiryPackageName = session.packageName,
                sessionExpiryCountdownTotal = countdownSeconds,
                sessionExpiryCountdownRemaining = countdownSeconds,
                sessionExpiryShowMathOption = settings.enableMathChallenge,
                showSessionExpiryCountdown = countdownSeconds > 0,
                showSessionExpiryDecisionDialog = countdownSeconds <= 0
            )

            if (countdownSeconds > 0) {
                startCountdown(countdownSeconds)
                // Show system overlay countdown
                showOverlayCountdown(appName, countdownSeconds, countdownSeconds)
            } else {
                // Show decision dialog immediately
                showOverlayDecision(appName, session.packageName, settings.enableMathChallenge)
            }
        }
    }

    private fun startCountdown(totalSeconds: Int) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var remaining = totalSeconds
            while (remaining > 0) {
                _uiState.value = _uiState.value.copy(
                    sessionExpiryCountdownRemaining = remaining
                )
                // Update overlay countdown
                val appName = _uiState.value.sessionExpiryAppName ?: "this app"
                showOverlayCountdown(appName, remaining, totalSeconds)
                delay(1000)
                remaining--
            }
            _uiState.value = _uiState.value.copy(sessionExpiryCountdownRemaining = 0)
            onCountdownFinished()
        }
    }

    private fun onCountdownFinished() {
        _uiState.value = _uiState.value.copy(
            showSessionExpiryCountdown = false,
            showSessionExpiryDecisionDialog = true
        )
        // Show overlay decision dialog
        val appName = _uiState.value.sessionExpiryAppName ?: "this app"
        val packageName = _uiState.value.sessionExpiryPackageName ?: ""
        val showMathOption = _uiState.value.sessionExpiryShowMathOption
        showOverlayDecision(appName, packageName, showMathOption)
    }

    private fun finalizeExpiredSession() {
        countdownJob?.cancel()
        countdownJob = null
        // Hide overlay
        hideOverlay()
        _uiState.value = _uiState.value.copy(
            sessionExpiryAppName = null,
            sessionExpiryPackageName = null,
            sessionExpiryCountdownTotal = 0,
            sessionExpiryCountdownRemaining = 0,
            showSessionExpiryCountdown = false,
            showSessionExpiryDecisionDialog = false,
            sessionExpiryShowMathOption = false
        )
        currentExpiredSession = null
        timeLimitRequestSource = TimeLimitRequestSource.STANDARD
        overlayPermissionPrompted = false

        if (pendingExpiredSessions.isNotEmpty()) {
            val nextSession = pendingExpiredSessions.removeFirst()
            processExpiredSession(nextSession)
        }
    }

    private fun checkExpiredSessions() {
        val repository = sessionRepository ?: return
        viewModelScope.launch {
            repository.emitExpiredSessions()
        }
    }

    fun checkExpiredSessionsManually() {
        checkExpiredSessions()
    }

    private fun startOverlayService(intent: Intent, requireForeground: Boolean) {
        val ctx = context ?: return
        val appContext = ctx.applicationContext
        try {
            if (requireForeground && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(appContext, intent)
            } else {
                appContext.startService(intent)
            }
        } catch (illegalState: IllegalStateException) {
            if (requireForeground && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(appContext, intent)
            }
        }
    }

    private fun showOverlayCountdown(appName: String, remainingSeconds: Int, totalSeconds: Int) {
        if (!ensureOverlayPermission(appName)) {
            return
        }

        val ctx = context ?: return
        val intent = Intent(ctx, OverlayService::class.java).apply {
            action = OverlayService.ACTION_SHOW_COUNTDOWN
            putExtra(OverlayService.EXTRA_APP_NAME, appName)
            putExtra(OverlayService.EXTRA_REMAINING_SECONDS, remainingSeconds)
            putExtra(OverlayService.EXTRA_TOTAL_SECONDS, totalSeconds)
        }
        startOverlayService(intent, requireForeground = true)
    }

    private fun showOverlayDecision(appName: String, packageName: String, showMathOption: Boolean) {
        if (!ensureOverlayPermission(appName)) {
            return
        }

        val ctx = context ?: return
        val intent = Intent(ctx, OverlayService::class.java).apply {
            action = OverlayService.ACTION_SHOW_DECISION
            putExtra(OverlayService.EXTRA_APP_NAME, appName)
            putExtra(OverlayService.EXTRA_PACKAGE_NAME, packageName)
            putExtra(OverlayService.EXTRA_SHOW_MATH_OPTION, showMathOption)
        }
        startOverlayService(intent, requireForeground = true)
    }

    private fun hideOverlay() {
        val ctx = context ?: return
        val intent = Intent(ctx, OverlayService::class.java).apply {
            action = OverlayService.ACTION_HIDE_OVERLAY
        }
        startOverlayService(intent, requireForeground = false)
    }

    private fun ensureOverlayPermission(appName: String?): Boolean {
        val helper = permissionsHelper ?: context?.let { PermissionsHelper(it.applicationContext) }
        val hasPermission = helper?.hasSystemAlertWindowPermission()

        if (hasPermission == true) {
            overlayPermissionPrompted = false
            if (_uiState.value.showOverlayPermissionDialog) {
                _uiState.value = _uiState.value.copy(showOverlayPermissionDialog = false)
            }
            return true
        }

        if (!overlayPermissionPrompted) {
            overlayPermissionPrompted = true
            _uiState.value = _uiState.value.copy(showOverlayPermissionDialog = true)
            helper?.requestSystemAlertWindowPermission()
            viewModelScope.launch {
                appRepository.closeCurrentApp()
            }
        }

        return false
    }

    fun dismissOverlayPermissionDialog() {
        _uiState.value = _uiState.value.copy(showOverlayPermissionDialog = false)
    }

    fun openOverlayPermissionSettings() {
        val helper = permissionsHelper ?: context?.let { PermissionsHelper(it.applicationContext) }
        helper?.requestSystemAlertWindowPermission()
    }

    override fun onCleared() {
        super.onCleared()
        try {
            context?.unregisterReceiver(sessionExpiryReceiver)
        } catch (e: Exception) {
            // Receiver may not be registered
        }
        hideOverlay()
        countdownJob?.cancel()
    }
}

data class HomeUiState(
    val pinnedApps: List<AppInfo> = emptyList(),
    val currentTime: String = "",
    val currentDate: String = "",
    val showTime: Boolean = true,
    val showDate: Boolean = true,
    val showWallpaper: Boolean = true,
    val backgroundColor: String = "system",
    val showFrictionDialog: Boolean = false,
    val selectedAppForFriction: String? = null,
    val showTimeLimitDialog: Boolean = false,
    val selectedAppForTimeLimit: String? = null,
    val showMathChallengeDialog: Boolean = false,
    val selectedAppForMathChallenge: String? = null,
    val isMathChallengeForExpiredSession: Boolean = false,
    val isMathChallengeForSessionExtension: Boolean = false,
    val showSessionExpiryCountdown: Boolean = false,
    val showSessionExpiryDecisionDialog: Boolean = false,
    val sessionExpiryAppName: String? = null,
    val sessionExpiryPackageName: String? = null,
    val sessionExpiryCountdownTotal: Int = 0,
    val sessionExpiryCountdownRemaining: Int = 0,
    val sessionExpiryShowMathOption: Boolean = false,
    val isLoading: Boolean = false,
    val showOverlayPermissionDialog: Boolean = false
)
