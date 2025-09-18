package com.talauncher.ui.home

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talauncher.R
import com.talauncher.data.model.AppInfo
import com.talauncher.data.model.AppSession
import com.talauncher.data.repository.AppRepository
import com.talauncher.data.repository.SettingsRepository
import com.talauncher.data.repository.SessionRepository
import com.talauncher.service.OverlayService
import com.talauncher.utils.BackgroundOverlayManager
import com.talauncher.utils.ContactHelper
import com.talauncher.utils.ContactInfo
import com.talauncher.utils.ErrorHandler
import com.talauncher.utils.PermissionsHelper
import com.talauncher.utils.UsageStatsHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val permissionsHelper: PermissionsHelper? = null,
    private val usageStatsHelper: UsageStatsHelper? = null,
    private val errorHandler: ErrorHandler? = null
) : ViewModel() {

    private enum class TimeLimitRequestSource { STANDARD, SESSION_EXTENSION }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
    private val pendingExpiredSessions = ArrayDeque<AppSession>()
    private var allVisibleApps: List<AppInfo> = emptyList()
    private var currentExpiredSession: AppSession? = null
    private var countdownJob: Job? = null
    private var timeLimitRequestSource = TimeLimitRequestSource.STANDARD
    private var overlayPermissionPrompted = false
    private var isReceiverRegistered = false
    private val sessionExpirationMutex = Mutex()
    private var backgroundOverlayManager: BackgroundOverlayManager? = null
    private var contactHelper: ContactHelper? = null
    private var pendingContactQuery: String? = null
    private var hasShownContactsPermissionPrompt = false


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
                "com.talauncher.MATH_CHALLENGE_CORRECT" -> {
                    val packageName = intent.getStringExtra("package_name")
                    if (packageName != null) {
                        onMathChallengeCompleted(packageName)
                    }
                }
                "com.talauncher.MATH_CHALLENGE_DISMISS" -> {
                    val packageName = intent.getStringExtra("package_name")
                    if (packageName != null) {
                        dismissMathChallengeDialog()
                    }
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
        context?.let {
            backgroundOverlayManager = BackgroundOverlayManager.getInstance(it)
            if (permissionsHelper != null) {
                contactHelper = ContactHelper(it, permissionsHelper)
            }
        }
    }

    private fun setupBroadcastReceiver() {
        context?.let { ctx ->
            if (!isReceiverRegistered) {
                val filter = IntentFilter().apply {
                    addAction("com.talauncher.SESSION_EXPIRY_EXTEND")
                    addAction("com.talauncher.SESSION_EXPIRY_CLOSE")
                    addAction("com.talauncher.SESSION_EXPIRY_MATH_CHALLENGE")
                    addAction("com.talauncher.MATH_CHALLENGE_CORRECT")
                    addAction("com.talauncher.MATH_CHALLENGE_DISMISS")
                }
                try {
                    ContextCompat.registerReceiver(
                        ctx,
                        sessionExpiryReceiver,
                        filter,
                        ContextCompat.RECEIVER_NOT_EXPORTED
                    )
                    isReceiverRegistered = true
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Failed to register broadcast receiver", e)
                }
            }
        }
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                appRepository.getPinnedApps(),
                appRepository.getAllVisibleApps(),
                settingsRepository.getSettings()
            ) { pinnedApps, allApps, settings ->
                allVisibleApps = allApps
                val currentQuery = _uiState.value.searchQuery
                val filtered = if (currentQuery.isNotBlank()) filterApps(currentQuery, allApps) else emptyList()
                _uiState.value = _uiState.value.copy(
                    pinnedApps = pinnedApps,
                    showTime = settings?.showTimeOnHomeScreen ?: true,
                    showDate = settings?.showDateOnHomeScreen ?: true,
                    showWallpaper = settings?.showWallpaper ?: true,
                    backgroundColor = settings?.backgroundColor ?: "system",
                    mathChallengeDifficulty = settings?.mathDifficulty ?: "easy",
                    searchResults = filtered,
                    showPhoneAction = settings?.showPhoneAction ?: true,
                    showMessageAction = settings?.showMessageAction ?: true,
                    showWhatsAppAction = settings?.showWhatsAppAction ?: true
                )
            }.collect { } 
        }
    }


    private fun filterApps(query: String, apps: List<AppInfo>): List<AppInfo> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) {
            return emptyList()
        }
        return apps.filter { app ->
            app.appName.contains(normalizedQuery, ignoreCase = true)
        }.sortedBy { it.appName.lowercase(Locale.getDefault()) }
    }

    private fun updateTime() {
        viewModelScope.launch {
            try {
                val now = Date()
                val formattedTime = timeFormat.format(now) ?: ""
                val formattedDate = dateFormat.format(now) ?: ""

                _uiState.value = _uiState.value.copy(
                    currentTime = formattedTime,
                    currentDate = formattedDate
                )
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error formatting time/date", e)
                // Fallback to basic formatting
                _uiState.value = _uiState.value.copy(
                    currentTime = "--:--",
                    currentDate = ""
                )
            }
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
                clearSearch()
                onLaunchApp?.invoke(packageName, null)
            } else {
                _uiState.value = _uiState.value.copy(
                    showFrictionDialog = true,
                    selectedAppForFriction = packageName
                )
            }
        }
    }


    fun updateSearchQuery(query: String) {
        val sanitized = query.trimStart()
        val appResults = if (sanitized.isBlank()) {
            emptyList<AppInfo>()
        } else {
            filterApps(sanitized, allVisibleApps)
        }

        _uiState.value = _uiState.value.copy(
            searchQuery = sanitized,
            searchResults = appResults,
            contactResults = emptyList()
        )

        if (sanitized.isBlank()) {
            pendingContactQuery = null
            hasShownContactsPermissionPrompt = false
            _uiState.value = _uiState.value.copy(
                isContactsPermissionMissing = false,
                showContactsPermissionDialog = false
            )
            return
        }

        pendingContactQuery = sanitized

        val helper = permissionsHelper
        if (contactHelper == null || helper == null) {
            return
        }

        if (!helper.permissionState.value.hasContacts) {
            _uiState.value = _uiState.value.copy(
                isContactsPermissionMissing = true
            )
            if (!hasShownContactsPermissionPrompt) {
                hasShownContactsPermissionPrompt = true
                _uiState.value = _uiState.value.copy(showContactsPermissionDialog = true)
            }
            return
        }

        hasShownContactsPermissionPrompt = false
        launchContactSearch(sanitized)
    }

    private fun launchContactSearch(query: String) {
        pendingContactQuery = query
        viewModelScope.launch {
            val contacts = contactHelper?.searchContacts(query) ?: emptyList()
            _uiState.value = _uiState.value.copy(
                contactResults = contacts,
                isContactsPermissionMissing = false
            )
        }
    }

    fun showContactsPermissionPrompt() {
        if (_uiState.value.isContactsPermissionMissing) {
            hasShownContactsPermissionPrompt = true
            _uiState.value = _uiState.value.copy(showContactsPermissionDialog = true)
        }
    }

    fun dismissContactsPermissionDialog() {
        _uiState.value = _uiState.value.copy(showContactsPermissionDialog = false)
    }

    fun requestContactsPermission() {
        val helper = permissionsHelper
        if (helper == null || contactHelper == null) {
            _uiState.value = _uiState.value.copy(showContactsPermissionDialog = false)
            return
        }

        if (helper.permissionState.value.hasContacts) {
            onContactsPermissionGranted()
            return
        }

        _uiState.value = _uiState.value.copy(showContactsPermissionDialog = false)

        val rationale = context?.getString(R.string.contact_permission_required_message)
            ?: "Allow TALauncher to access your contacts to find people quickly."
        val handler = errorHandler
        if (handler != null) {
            handler.requestPermission(
                Manifest.permission.READ_CONTACTS,
                rationale
            ) { granted ->
                if (granted) {
                    onContactsPermissionGranted()
                } else {
                    onContactsPermissionDenied()
                }
            }
        } else {
            val activity = context as? Activity
            if (activity != null) {
                helper.requestPermission(activity, com.talauncher.utils.PermissionType.CONTACTS)
            } else {
                onContactsPermissionDenied()
            }
        }
    }

    private fun onContactsPermissionGranted() {
        hasShownContactsPermissionPrompt = false
        _uiState.value = _uiState.value.copy(
            isContactsPermissionMissing = false,
            showContactsPermissionDialog = false
        )
        val query = pendingContactQuery
        if (!query.isNullOrBlank()) {
            launchContactSearch(query)
        }
    }

    private fun onContactsPermissionDenied() {
        _uiState.value = _uiState.value.copy(
            isContactsPermissionMissing = true
        )
    }
    fun clearSearch() {
        if (_uiState.value.searchQuery.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                searchQuery = "",
                searchResults = emptyList(),
                contactResults = emptyList(),
                isContactsPermissionMissing = false,
                showContactsPermissionDialog = false
            )
        }
        pendingContactQuery = null
        hasShownContactsPermissionPrompt = false
    }

    fun clearSearchOnNavigation() {
        clearSearch()
    }

    fun dismissFrictionDialog() {
        _uiState.value = _uiState.value.copy(
            showFrictionDialog = false,
            selectedAppForFriction = null
        )
    }

    fun launchAppWithReason(packageName: String, reason: String) {
        viewModelScope.launch {
            val launched = appRepository.launchApp(packageName, bypassFriction = true)
            if (launched) {
                clearSearch()
                onLaunchApp?.invoke(packageName, null)
                dismissFrictionDialog()
            }
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
                clearSearch()
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

    fun performGoogleSearch(query: String) {
        context?.let { ctx ->
            try {
                val searchUrl = "https://www.google.com/search?q=${Uri.encode(query)}"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(intent)
                clearSearch()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to open Google search", e)
            }
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

            // Check if user is still in the target app before showing math challenge
            if (usageStatsHelper != null && permissionsHelper != null) {
                val currentApp = usageStatsHelper.getCurrentForegroundApp(permissionsHelper.permissionState.value.hasUsageStats)
                if (currentApp != packageName) {
                    Log.d("HomeViewModel", "User left target app ($packageName) before math challenge, current app: $currentApp. Cancelling.")
                    // User has left the target app, cancel and clean up
                    hideOverlay()
                    finalizeExpiredSession()
                    return@launch
                }
            }

            val appName = appRepository.getAppDisplayName(packageName)
            val settings = settingsRepository.getSettingsSync()

            // Only show math challenge if it's enabled in settings
            if (settings.enableMathChallenge) {
                // Try to show overlay math challenge first
                if (ensureOverlayPermissionImmediate()) {
                    showOverlayMathChallenge(appName, packageName, settings.mathDifficulty)
                } else {
                    // Fallback to in-app math challenge
                    _uiState.value = _uiState.value.copy(
                        showSessionExpiryDecisionDialog = false,
                        showMathChallengeDialog = true,
                        selectedAppForMathChallenge = packageName,
                        isMathChallengeForSessionExtension = true,
                        isMathChallengeForExpiredSession = true
                    )
                }
            } else {
                // If math challenge is disabled, fallback to close the app
                currentExpiredSession?.let { session ->
                    appRepository.closeCurrentApp()
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
        viewModelScope.launch {
            sessionExpirationMutex.withLock {
                if (currentExpiredSession != null) {
                    pendingExpiredSessions.addLast(session)
                    return@withLock
                }
                processExpiredSession(session)
            }
        }
    }

    private fun processExpiredSession(session: AppSession) {
        if (sessionRepository == null) return
        currentExpiredSession = session
        countdownJob?.cancel()

        viewModelScope.launch {
            // Check if user is still in the target app before showing any popup
            if (usageStatsHelper != null && permissionsHelper != null) {
                val currentApp = usageStatsHelper.getCurrentForegroundApp(permissionsHelper.permissionState.value.hasUsageStats)
                if (currentApp != session.packageName) {
                    Log.d("HomeViewModel", "User not in target app (${session.packageName}) when session expired, current app: $currentApp. Skipping popup.")
                    // User is not in the target app, don't show popup and clean up
                    finalizeExpiredSession()
                    return@launch
                }
            }

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
            val targetPackageName = _uiState.value.sessionExpiryPackageName

            while (remaining > 0) {
                // Check if user is still in the target app
                if (targetPackageName != null && usageStatsHelper != null && permissionsHelper != null) {
                    val currentApp = usageStatsHelper.getCurrentForegroundApp(permissionsHelper.permissionState.value.hasUsageStats)
                    if (currentApp != targetPackageName) {
                        Log.d("HomeViewModel", "User left target app ($targetPackageName), current app: $currentApp. Cancelling countdown.")
                        // User has left the target app, cancel countdown and hide overlay
                        hideOverlay()
                        finalizeExpiredSession()
                        return@launch
                    }
                }

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
        viewModelScope.launch {
            val targetPackageName = _uiState.value.sessionExpiryPackageName

            // Check if user is still in the target app before showing decision dialog
            if (targetPackageName != null && usageStatsHelper != null && permissionsHelper != null) {
                val currentApp = usageStatsHelper.getCurrentForegroundApp(permissionsHelper.permissionState.value.hasUsageStats)
                if (currentApp != targetPackageName) {
                    Log.d("HomeViewModel", "User left target app ($targetPackageName) before decision dialog, current app: $currentApp. Cancelling.")
                    // User has left the target app, cancel and clean up
                    hideOverlay()
                    finalizeExpiredSession()
                    return@launch
                }
            }

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
    }

    private fun finalizeExpiredSession() {
        viewModelScope.launch {
            sessionExpirationMutex.withLock {
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


    private fun showOverlayCountdown(appName: String, remainingSeconds: Int, totalSeconds: Int) {
        if (!ensureOverlayPermission(appName)) {
            return
        }

        val overlayManager = backgroundOverlayManager ?: return
        val success = overlayManager.showCountdownOverlay(appName, remainingSeconds, totalSeconds)

        if (!success) {
            Log.w("HomeViewModel", "Failed to show countdown overlay, using fallback")
            // Keep existing service fallback for compatibility
            val ctx = context ?: return
            val intent = Intent(ctx, OverlayService::class.java).apply {
                action = OverlayService.ACTION_SHOW_COUNTDOWN
                putExtra(OverlayService.EXTRA_APP_NAME, appName)
                putExtra(OverlayService.EXTRA_REMAINING_SECONDS, remainingSeconds)
                putExtra(OverlayService.EXTRA_TOTAL_SECONDS, totalSeconds)
            }
            startOverlayServiceSafely(intent, requireForeground = true)
        }
    }

    private fun showOverlayDecision(appName: String, packageName: String, showMathOption: Boolean) {
        if (!ensureOverlayPermission(appName)) {
            return
        }

        val overlayManager = backgroundOverlayManager ?: return
        val success = overlayManager.showDecisionOverlay(
            appName = appName,
            packageName = packageName,
            showMathOption = showMathOption,
            onExtend = { onSessionExpiryDecisionExtend() },
            onClose = { onSessionExpiryDecisionClose() },
            onMathChallenge = if (showMathOption) {{ onSessionExpiryDecisionMathChallenge() }} else null
        )

        if (!success) {
            Log.w("HomeViewModel", "Failed to show decision overlay, using fallback")
            // Keep existing service fallback for compatibility
            val ctx = context ?: return
            val intent = Intent(ctx, OverlayService::class.java).apply {
                action = OverlayService.ACTION_SHOW_DECISION
                putExtra(OverlayService.EXTRA_APP_NAME, appName)
                putExtra(OverlayService.EXTRA_PACKAGE_NAME, packageName)
                putExtra(OverlayService.EXTRA_SHOW_MATH_OPTION, showMathOption)
            }
            startOverlayServiceSafely(intent, requireForeground = true)
        }
    }

    private fun hideOverlay() {
        // Hide background overlay first
        backgroundOverlayManager?.hideCurrentOverlay()

        // Also hide service overlay for compatibility
        val ctx = context ?: return
        val intent = Intent(ctx, OverlayService::class.java).apply {
            action = OverlayService.ACTION_HIDE_OVERLAY
        }
        startOverlayServiceSafely(intent, requireForeground = false)
    }

    private fun ensureOverlayPermission(appName: String?): Boolean {
        val helper = permissionsHelper ?: context?.let { PermissionsHelper(it.applicationContext) }
        val hasPermission = helper?.permissionState?.value?.hasSystemAlertWindow

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
            helper?.requestPermission(context as Activity, com.talauncher.utils.PermissionType.SYSTEM_ALERT_WINDOW)
            viewModelScope.launch {
                appRepository.closeCurrentApp()
            }
        }

        return false
    }

    private fun ensureOverlayPermissionImmediate(): Boolean {
        val helper = permissionsHelper ?: context?.let { PermissionsHelper(it.applicationContext) }
        return helper?.permissionState?.value?.hasSystemAlertWindow == true
    }

    private fun showOverlayMathChallenge(appName: String, packageName: String, difficulty: String) {
        val overlayManager = backgroundOverlayManager
        if (overlayManager != null) {
            val success = overlayManager.showMathChallengeOverlay(
                appName = appName,
                packageName = packageName,
                difficulty = difficulty,
                onCorrect = {
                    onMathChallengeCompleted(packageName)
                },
                onDismiss = {
                    dismissMathChallengeDialog()
                }
            )

            if (success) {
                return // Successfully shown background overlay
            }
        }

        // Fallback to service approach
        Log.w("HomeViewModel", "Failed to show math challenge overlay, using service fallback")
        val ctx = context ?: return
        val intent = Intent(ctx, OverlayService::class.java).apply {
            action = OverlayService.ACTION_SHOW_MATH_CHALLENGE
            putExtra(OverlayService.EXTRA_APP_NAME, appName)
            putExtra(OverlayService.EXTRA_PACKAGE_NAME, packageName)
            putExtra(OverlayService.EXTRA_DIFFICULTY, difficulty)
        }
        startOverlayServiceSafely(intent, requireForeground = true)
    }

    private fun startOverlayServiceSafely(intent: Intent, requireForeground: Boolean) {
        val ctx = context ?: return
        val appContext = ctx.applicationContext

        try {
            if (requireForeground && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(appContext, intent)
            } else {
                appContext.startService(intent)
            }
            Log.d("HomeViewModel", "Overlay service started successfully")
        } catch (illegalState: IllegalStateException) {
            Log.w("HomeViewModel", "Failed to start service, trying fallback approach", illegalState)
            // Fallback: Show dialog in launcher app instead of overlay
            showInAppMathChallenge()
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Unexpected error starting overlay service", e)
            showInAppMathChallenge()
        }
    }

    private fun showInAppMathChallenge() {
        // Fallback: Show math challenge within the launcher app
        val packageName = currentExpiredSession?.packageName ?: return
        _uiState.value = _uiState.value.copy(
            showSessionExpiryDecisionDialog = false,
            showMathChallengeDialog = true,
            selectedAppForMathChallenge = packageName,
            isMathChallengeForExpiredSession = true,
            isMathChallengeForSessionExtension = true
        )
    }

    fun dismissOverlayPermissionDialog() {
        _uiState.value = _uiState.value.copy(showOverlayPermissionDialog = false)
    }

    fun openOverlayPermissionSettings() {
        val helper = permissionsHelper ?: context?.let { PermissionsHelper(it.applicationContext) }
        helper?.requestPermission(context as Activity, com.talauncher.utils.PermissionType.SYSTEM_ALERT_WINDOW)
    }

    fun callContact(contact: ContactInfo) {
        contactHelper?.callContact(contact)
        clearSearch()
    }

    fun messageContact(contact: ContactInfo) {
        contactHelper?.messageContact(contact)
        clearSearch()
    }

    fun whatsAppContact(contact: ContactInfo) {
        contactHelper?.whatsAppContact(contact)
        clearSearch()
    }

    override fun onCleared() {
        super.onCleared()
        if (isReceiverRegistered) {
            try {
                context?.unregisterReceiver(sessionExpiryReceiver)
                isReceiverRegistered = false
            } catch (e: IllegalArgumentException) {
                // Receiver was not registered, which is fine
                Log.d("HomeViewModel", "Broadcast receiver was not registered")
            } catch (e: Exception) {
                // Log other unexpected errors but don't crash
                Log.e("HomeViewModel", "Unexpected error unregistering broadcast receiver", e)
            }
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
    val searchQuery: String = "",
    val searchResults: List<AppInfo> = emptyList(),
    val contactResults: List<ContactInfo> = emptyList(),
    val isContactsPermissionMissing: Boolean = false,
    val showContactsPermissionDialog: Boolean = false,
    val showFrictionDialog: Boolean = false,
    val selectedAppForFriction: String? = null,
    val showTimeLimitDialog: Boolean = false,
    val selectedAppForTimeLimit: String? = null,
    val showMathChallengeDialog: Boolean = false,
    val selectedAppForMathChallenge: String? = null,
    val isMathChallengeForExpiredSession: Boolean = false,
    val isMathChallengeForSessionExtension: Boolean = false,
    val mathChallengeDifficulty: String = "easy",
    val showSessionExpiryCountdown: Boolean = false,
    val showSessionExpiryDecisionDialog: Boolean = false,
    val sessionExpiryAppName: String? = null,
    val sessionExpiryPackageName: String? = null,
    val sessionExpiryCountdownTotal: Int = 0,
    val sessionExpiryCountdownRemaining: Int = 0,
    val sessionExpiryShowMathOption: Boolean = false,
    val isLoading: Boolean = false,
    val showOverlayPermissionDialog: Boolean = false,
    val showPhoneAction: Boolean = true,
    val showMessageAction: Boolean = true,
    val showWhatsAppAction: Boolean = true
)