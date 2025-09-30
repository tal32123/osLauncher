package com.talauncher.ui.home

import android.Manifest
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
import com.talauncher.data.model.ColorPaletteOption
import com.talauncher.data.model.MathDifficulty
import com.talauncher.data.model.UiDensityOption
import com.talauncher.data.model.WeatherDisplayOption
import com.talauncher.data.model.WeatherTemperatureUnit
import com.talauncher.data.repository.AppRepository
import com.talauncher.data.repository.SearchInteractionRepository
import com.talauncher.data.repository.SearchInteractionRepository.ContactAction
import com.talauncher.data.repository.SettingsRepository
import com.talauncher.data.repository.SessionRepository
import com.talauncher.service.OverlayService
import com.talauncher.service.WeatherService
import com.talauncher.utils.BackgroundOverlayManager
import com.talauncher.utils.ContactHelper
import com.talauncher.utils.ContactInfo
import com.talauncher.utils.ErrorHandler
import com.talauncher.utils.PermissionsHelper
import com.talauncher.utils.UsageStatsHelper
import com.talauncher.utils.IdlingResourceHelper
import com.talauncher.utils.SearchScoring
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import com.talauncher.BuildConfig

internal const val RECENT_APPS_INDEX_KEY = "*"

sealed class SearchItem {
    abstract val name: String
    abstract val relevanceScore: Int
    abstract val lastInteractionTimestamp: Long?

    data class App(
        val appInfo: AppInfo,
        override val relevanceScore: Int,
        override val lastInteractionTimestamp: Long?
    ) : SearchItem() {
        override val name: String = appInfo.appName
    }

    data class Contact(
        val contactInfo: ContactInfo,
        override val relevanceScore: Int,
        override val lastInteractionTimestamp: Long?
    ) : SearchItem() {
        override val name: String = contactInfo.name
    }
}

data class AlphabetIndexEntry(
    val key: String,
    val displayLabel: String,
    val targetIndex: Int?,
    val hasApps: Boolean,
    val previewAppName: String?
)

sealed interface HomeEvent {
    data object RequestContactsPermission : HomeEvent
    data object RequestOverlayPermission : HomeEvent
}

class HomeViewModel(
    private val appRepository: AppRepository,
    private val settingsRepository: SettingsRepository,
    private val searchInteractionRepository: SearchInteractionRepository? = null,
    private val onLaunchApp: ((String, Int?) -> Unit)? = null,
    private val sessionRepository: SessionRepository? = null,
    private val appContext: Context,
    private val backgroundOverlayManager: BackgroundOverlayManager = BackgroundOverlayManager.getInstance(appContext),
    initialContactHelper: ContactHelper? = null,
    private val permissionsHelper: PermissionsHelper? = null,
    private val usageStatsHelper: UsageStatsHelper? = null,
    private val errorHandler: ErrorHandler? = null
) : ViewModel() {

    private enum class TimeLimitRequestSource { STANDARD, SESSION_EXTENSION }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    private val contactHelper: ContactHelper? = initialContactHelper ?: permissionsHelper?.let {
        ContactHelper(appContext, it)
    }
    private val weatherService = WeatherService(appContext)
    private val fallbackPermissionsHelper: PermissionsHelper by lazy { PermissionsHelper(appContext) }
    private val resolvedPermissionsHelper: PermissionsHelper?
        get() = permissionsHelper ?: fallbackPermissionsHelper

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
    private val pendingExpiredSessions = ArrayDeque<AppSession>()
    private var allVisibleApps: List<AppInfo> = emptyList()
    private var allHiddenApps: List<AppInfo> = emptyList()
    private var currentExpiredSession: AppSession? = null
    private var countdownJob: Job? = null
    private var timeLimitRequestSource = TimeLimitRequestSource.STANDARD
    private var overlayPermissionPrompted = false
    private var isReceiverRegistered = false
    private val sessionExpirationMutex = Mutex()
    private var pendingContactQuery: String? = null
    private var hasShownContactsPermissionPrompt = false
    private var weatherUpdateJob: Job? = null
    private var contactSearchJob: Job? = null
    private val searchQueryFlow = MutableStateFlow("")


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
        setupDebouncedContactSearch()
    }

    private fun setupBroadcastReceiver() {
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
                    appContext,
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

    private fun observeData() {
        viewModelScope.launch {
            combine(
                appRepository.getAllVisibleApps(),
                settingsRepository.getSettings()
            ) { allApps, settings ->
                withContext(Dispatchers.Default) {
                    allVisibleApps = allApps
                    val currentQuery = _uiState.value.searchQuery

                    // Cache expensive operations
                    val isWhatsAppInstalled = contactHelper?.isWhatsAppInstalled() ?: false
                    val weatherDisplay = settings?.weatherDisplay ?: WeatherDisplayOption.DAILY

                    // Get recent apps and alphabet index for the moved app drawer functionality
                    val hasUsageStatsPermission = resolvedPermissionsHelper?.permissionState?.value?.hasUsageStats ?: false
                    val hiddenApps = try {
                        appRepository.getHiddenApps().first()
                    } catch (e: Exception) {
                        emptyList<AppInfo>()
                    }
                    allHiddenApps = hiddenApps
                    val searchableApps = allApps + hiddenApps
                    val filtered = if (currentQuery.isNotBlank()) filterApps(currentQuery, searchableApps) else emptyList()
                    val recentAppsLimit = settings?.recentAppsLimit ?: 10
                    val recentApps = getRecentApps(allApps, hiddenApps, recentAppsLimit, hasUsageStatsPermission)
                    val alphabetIndex = buildAlphabetIndex(allApps, recentApps)

                    withContext(Dispatchers.Main.immediate) {
                        val wasExpanded = _uiState.value.isOtherAppsExpanded
                        _uiState.value = _uiState.value.copy(
                            allVisibleApps = allApps,
                            hiddenApps = hiddenApps,
                            showTime = settings?.showTimeOnHomeScreen ?: true,
                            showDate = settings?.showDateOnHomeScreen ?: true,
                            showWallpaper = settings?.showWallpaper ?: true,
                            backgroundColor = settings?.backgroundColor ?: "system",
                            backgroundOpacity = settings?.backgroundOpacity ?: 1f,
                            mathChallengeDifficulty = settings?.mathDifficulty ?: MathDifficulty.EASY,
                            searchResults = filtered,
                            showPhoneAction = settings?.showPhoneAction ?: true,
                            showMessageAction = settings?.showMessageAction ?: true,
                            showWhatsAppAction = (settings?.showWhatsAppAction ?: true) && isWhatsAppInstalled,
                            weatherDisplay = weatherDisplay,
                            weatherTemperatureUnit = settings?.weatherTemperatureUnit
                                ?: WeatherTemperatureUnit.CELSIUS,
                            colorPalette = settings?.colorPalette ?: ColorPaletteOption.DEFAULT,
                            wallpaperBlurAmount = settings?.wallpaperBlurAmount ?: 0f,
                            customWallpaperPath = settings?.customWallpaperPath,
                            enableGlassmorphism = settings?.enableGlassmorphism ?: true,
                            uiDensity = settings?.uiDensity ?: UiDensityOption.COMPACT,
                            enableAnimations = settings?.enableAnimations ?: true,
                            // App drawer functionality moved to home screen
                            recentApps = recentApps,
                            alphabetIndexEntries = alphabetIndex,
                            isAlphabetIndexEnabled = allApps.isNotEmpty()
                        ).let { updated ->
                            val keepExpanded = wasExpanded && hiddenApps.isNotEmpty()
                            if (updated.isOtherAppsExpanded != keepExpanded) {
                                updated.copy(isOtherAppsExpanded = keepExpanded)
                            } else {
                                updated
                            }
                        }

                        // Update weather data if needed
                        _uiState.value = when (weatherDisplay) {
                            WeatherDisplayOption.OFF -> _uiState.value.copy(
                                weatherData = null,
                                weatherDailyHigh = null,
                                weatherDailyLow = null,
                                weatherError = null
                            )
                            WeatherDisplayOption.DAILY -> _uiState.value
                            WeatherDisplayOption.HOURLY -> _uiState.value.copy(
                                weatherDailyHigh = null,
                                weatherDailyLow = null
                            )
                        }
                    }

                    if (weatherDisplay != WeatherDisplayOption.OFF) {
                        updateWeatherData(
                            savedLat = settings?.weatherLocationLat,
                            savedLon = settings?.weatherLocationLon,
                            display = weatherDisplay
                        )
                    }
                }
            }.collect { }
        }
    }


    private fun filterApps(query: String, apps: List<AppInfo>): List<AppInfo> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) {
            return emptyList()
        }
        return apps.mapNotNull { app ->
            val score = SearchScoring.calculateRelevanceScore(normalizedQuery, app.appName)
            if (score > 0) {
                app to score
            } else {
                null
            }
        }
            .sortedWith(
                compareByDescending<Pair<AppInfo, Int>> { it.second }
                    .thenBy { it.first.appName.lowercase(Locale.getDefault()) }
            )
            .map { it.first }
    }

    private suspend fun createUnifiedSearchResults(query: String, apps: List<AppInfo>, contacts: List<ContactInfo>): List<SearchItem> {
        if (query.isBlank()) return emptyList()

        val interactionSnapshot = searchInteractionRepository?.getLastUsedSnapshot(
            appPackages = apps.map { it.packageName },
            contactIds = contacts.map { it.id }
        ) ?: SearchInteractionRepository.SearchInteractionSnapshot.EMPTY

        val locale = Locale.getDefault()

        val appItems = apps.mapNotNull { app ->
            val baseScore = SearchScoring.calculateRelevanceScore(query, app.appName)
            if (baseScore > 0) {
                SearchItem.App(
                    appInfo = app,
                    relevanceScore = baseScore,
                    lastInteractionTimestamp = interactionSnapshot.appLastUsed[app.packageName]
                )
            } else null
        }

        val contactItems = contacts.mapNotNull { contact ->
            val score = SearchScoring.calculateRelevanceScore(query, contact.name)
            if (score > 0) {
                SearchItem.Contact(
                    contactInfo = contact,
                    relevanceScore = score,
                    lastInteractionTimestamp = interactionSnapshot.contactLastUsed[contact.id]
                )
            } else null
        }

        return (appItems + contactItems)
            .sortedWith(
                compareByDescending<SearchItem> { it.relevanceScore }
                    .thenByDescending { it.lastInteractionTimestamp ?: Long.MIN_VALUE }
                    .thenBy { it.name.lowercase(locale) }
            )
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
                val promptInfo = buildTimeLimitPromptState(packageName)
                _uiState.value = _uiState.value.copy(
                    showTimeLimitDialog = true,
                    selectedAppForTimeLimit = packageName,
                    timeLimitDialogAppName = promptInfo.appName,
                    timeLimitDialogUsageMinutes = promptInfo.usageMinutes,
                    timeLimitDialogTimeLimitMinutes = promptInfo.limitMinutes,
                    timeLimitDialogUsesDefaultLimit = promptInfo.usesDefaultLimit
                )
                timeLimitRequestSource = TimeLimitRequestSource.STANDARD
                return@launch
            }

            val launched = appRepository.launchApp(packageName)
            if (launched) {
                searchInteractionRepository?.recordAppLaunch(packageName)
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
        searchQueryFlow.value = sanitized

        val appResults = if (sanitized.isBlank()) {
            emptyList<AppInfo>()
        } else {
            filterApps(sanitized, allVisibleApps + allHiddenApps)
        }

        // Update individual results for backward compatibility
        _uiState.value = _uiState.value.copy(
            searchQuery = sanitized,
            searchResults = appResults,
            contactResults = emptyList(),
            unifiedSearchResults = emptyList()
        )

        if (sanitized.isBlank()) {
            contactSearchJob?.cancel()
            pendingContactQuery = null
            hasShownContactsPermissionPrompt = false
            _uiState.value = _uiState.value.copy(
                isContactsPermissionMissing = false,
                showContactsPermissionDialog = false
            )
            return
        }

        pendingContactQuery = sanitized

        val helper = resolvedPermissionsHelper
        if (contactHelper == null || helper == null) {
            // Only show apps if no contact helper
            viewModelScope.launch {
                val unifiedResults = createUnifiedSearchResults(sanitized, appResults, emptyList())
                _uiState.value = _uiState.value.copy(unifiedSearchResults = unifiedResults)
            }
            return
        }

        if (!helper.permissionState.value.hasContacts) {
            // Only show apps if no contacts permission
            viewModelScope.launch {
                val unifiedResults = createUnifiedSearchResults(sanitized, appResults, emptyList())
                _uiState.value = _uiState.value.copy(
                    unifiedSearchResults = unifiedResults,
                    isContactsPermissionMissing = true
                )
            }
            if (!hasShownContactsPermissionPrompt) {
                hasShownContactsPermissionPrompt = true
                _uiState.value = _uiState.value.copy(showContactsPermissionDialog = true)
            }
            return
        }

        hasShownContactsPermissionPrompt = false
        // Contact search will be handled by the debounced flow
    }

    @OptIn(FlowPreview::class)
    private fun setupDebouncedContactSearch() {
        viewModelScope.launch {
            searchQueryFlow
                .debounce(200) // 200ms debounce
                .collect { query ->
                    if (query.isNotBlank() && contactHelper != null &&
                        resolvedPermissionsHelper?.permissionState?.value?.hasContacts == true) {
                        launchContactSearch(query)
                    }
                }
        }
    }

    private fun launchContactSearch(query: String) {
        contactSearchJob?.cancel()
        contactSearchJob = viewModelScope.launch {
            try {
                val contacts = withContext(Dispatchers.IO) {
                    contactHelper?.searchContacts(query) ?: emptyList()
                }
                val currentAppResults = _uiState.value.searchResults
                val unifiedResults = createUnifiedSearchResults(query, currentAppResults, contacts)

                _uiState.value = _uiState.value.copy(
                    contactResults = contacts,
                    unifiedSearchResults = unifiedResults,
                    isContactsPermissionMissing = false
                )
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error searching contacts", e)
            }
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
        val helper = resolvedPermissionsHelper
        if (helper == null || contactHelper == null) {
            _uiState.value = _uiState.value.copy(showContactsPermissionDialog = false)
            return
        }

        if (helper.permissionState.value.hasContacts) {
            onContactsPermissionGranted()
            return
        }

        _uiState.value = _uiState.value.copy(showContactsPermissionDialog = false)

        val rationale = appContext.getString(R.string.contact_permission_required_message)
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
            viewModelScope.launch {
                _events.emit(HomeEvent.RequestContactsPermission)
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
                unifiedSearchResults = emptyList(),
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

    fun toggleOtherAppsExpansion() {
        val currentState = _uiState.value
        if (currentState.hiddenApps.isEmpty()) {
            return
        }
        _uiState.value = currentState.copy(
            isOtherAppsExpanded = !currentState.isOtherAppsExpanded
        )
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
                searchInteractionRepository?.recordAppLaunch(packageName)
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
            selectedAppForTimeLimit = null,
            timeLimitDialogAppName = null,
            timeLimitDialogUsageMinutes = null,
            timeLimitDialogTimeLimitMinutes = 0,
            timeLimitDialogUsesDefaultLimit = true
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
                searchInteractionRepository?.recordAppLaunch(packageName)
                clearSearch()
                onLaunchApp?.invoke(packageName, durationMinutes)
                if (isExtension) {
                    _uiState.value = _uiState.value.copy(
                        showTimeLimitDialog = false,
                        selectedAppForTimeLimit = null,
                        timeLimitDialogAppName = null,
                        timeLimitDialogUsageMinutes = null,
                        timeLimitDialogTimeLimitMinutes = 0,
                        timeLimitDialogUsesDefaultLimit = true
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
                    timeLimitDialogAppName = null,
                    timeLimitDialogUsageMinutes = null,
                    timeLimitDialogTimeLimitMinutes = 0,
                    timeLimitDialogUsesDefaultLimit = true,
                    showSessionExpiryDecisionDialog = true
                )
                timeLimitRequestSource = TimeLimitRequestSource.STANDARD
            }
        }
    }

    private suspend fun buildTimeLimitPromptState(packageName: String): TimeLimitPromptState {
        val appName = appRepository.getAppDisplayName(packageName)
        val timeLimitInfo = appRepository.getAppTimeLimitInfo(packageName)
        val usageMinutes = getUsageMinutesForApp(packageName)
        return TimeLimitPromptState(
            appName = appName,
            usageMinutes = usageMinutes,
            limitMinutes = timeLimitInfo.minutes,
            usesDefaultLimit = timeLimitInfo.usesDefault
        )
    }

    private suspend fun getUsageMinutesForApp(packageName: String): Int? {
        val helper = usageStatsHelper ?: return null
        val permissionState = resolvedPermissionsHelper?.permissionState?.value
        if (permissionState?.hasUsageStats != true) {
            return null
        }

        val usageStats = helper.getTodayUsageStats(true)
        val usageMillis = usageStats.firstOrNull { it.packageName == packageName }?.timeInForeground ?: 0
        return TimeUnit.MILLISECONDS.toMinutes(usageMillis).toInt()
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
                val promptInfo = buildTimeLimitPromptState(packageName)
                _uiState.value = _uiState.value.copy(
                    showMathChallengeDialog = false,
                    selectedAppForMathChallenge = null,
                    isMathChallengeForSessionExtension = false,
                    isMathChallengeForExpiredSession = false,
                    showSessionExpiryDecisionDialog = false,
                    showTimeLimitDialog = true,
                    selectedAppForTimeLimit = packageName,
                    timeLimitDialogAppName = promptInfo.appName,
                    timeLimitDialogUsageMinutes = promptInfo.usageMinutes,
                    timeLimitDialogTimeLimitMinutes = promptInfo.limitMinutes,
                    timeLimitDialogUsesDefaultLimit = promptInfo.usesDefaultLimit
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


    fun performGoogleSearch(query: String) {
        try {
            val searchUrl = "https://www.google.com/search?q=${Uri.encode(query)}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
            clearSearch()
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Failed to open Google search", e)
        }
    }

    fun onSessionExpiryDecisionExtend() {
        val packageName = currentExpiredSession?.packageName ?: return
        viewModelScope.launch {
            timeLimitRequestSource = TimeLimitRequestSource.SESSION_EXTENSION
            val promptInfo = buildTimeLimitPromptState(packageName)
            _uiState.value = _uiState.value.copy(
                showSessionExpiryDecisionDialog = false,
                showTimeLimitDialog = true,
                selectedAppForTimeLimit = packageName,
                timeLimitDialogAppName = promptInfo.appName,
                timeLimitDialogUsageMinutes = promptInfo.usageMinutes,
                timeLimitDialogTimeLimitMinutes = promptInfo.limitMinutes,
                timeLimitDialogUsesDefaultLimit = promptInfo.usesDefaultLimit
            )
        }
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
            val permissionsHelper = resolvedPermissionsHelper
            if (usageStatsHelper != null && permissionsHelper != null) {
                val currentApp = usageStatsHelper.getCurrentForegroundApp(permissionsHelper.permissionState.value.hasUsageStats)
                if (currentApp != packageName) {
                    if (BuildConfig.DEBUG) Log.d("HomeViewModel", "User left target app ($packageName) before math challenge, current app: $currentApp. Cancelling.")
                    // User has left the target app, cancel and clean up
                    hideOverlay()
                    finalizeExpiredSession()
                    return@launch
                }
            }

            val appName = appRepository.getAppDisplayName(packageName)
            val settings = withContext(Dispatchers.IO) {
                settingsRepository.getSettingsSync()
            }

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
            val permissionsHelper = resolvedPermissionsHelper
            if (usageStatsHelper != null && permissionsHelper != null) {
                val currentApp = usageStatsHelper.getCurrentForegroundApp(permissionsHelper.permissionState.value.hasUsageStats)
                if (currentApp != session.packageName) {
                    if (BuildConfig.DEBUG) Log.d("HomeViewModel", "User not in target app (${session.packageName}) when session expired, current app: $currentApp. Skipping popup.")
                    // User is not in the target app, don't show popup and clean up
                    finalizeExpiredSession()
                    return@launch
                }
            }

            val settings = withContext(Dispatchers.IO) {
                settingsRepository.getSettingsSync()
            }
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
                val permissionsHelper = resolvedPermissionsHelper
                if (targetPackageName != null && usageStatsHelper != null && permissionsHelper != null) {
                    val currentApp = usageStatsHelper.getCurrentForegroundApp(permissionsHelper.permissionState.value.hasUsageStats)
                    if (currentApp != targetPackageName) {
                        if (BuildConfig.DEBUG) Log.d("HomeViewModel", "User left target app ($targetPackageName), current app: $currentApp. Cancelling countdown.")
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
            val permissionsHelper = resolvedPermissionsHelper
            if (targetPackageName != null && usageStatsHelper != null && permissionsHelper != null) {
                val currentApp = usageStatsHelper.getCurrentForegroundApp(permissionsHelper.permissionState.value.hasUsageStats)
                if (currentApp != targetPackageName) {
                    if (BuildConfig.DEBUG) Log.d("HomeViewModel", "User left target app ($targetPackageName) before decision dialog, current app: $currentApp. Cancelling.")
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

        val overlayManager = backgroundOverlayManager
        val success = overlayManager.showCountdownOverlay(appName, remainingSeconds, totalSeconds)

        if (!success) {
            Log.w("HomeViewModel", "Failed to show countdown overlay, using fallback")
            // Keep existing service fallback for compatibility
            val intent = Intent(appContext, OverlayService::class.java).apply {
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

        val overlayManager = backgroundOverlayManager
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
            val intent = Intent(appContext, OverlayService::class.java).apply {
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
        backgroundOverlayManager.hideCurrentOverlay()

        // Also hide service overlay for compatibility
        val intent = Intent(appContext, OverlayService::class.java).apply {
            action = OverlayService.ACTION_HIDE_OVERLAY
        }
        startOverlayServiceSafely(intent, requireForeground = false)
    }

    private fun ensureOverlayPermission(appName: String?): Boolean {
        val helper = resolvedPermissionsHelper
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
            viewModelScope.launch {
                _events.emit(HomeEvent.RequestOverlayPermission)
            }
            viewModelScope.launch {
                appRepository.closeCurrentApp()
            }
        }

        return false
    }

    private fun ensureOverlayPermissionImmediate(): Boolean {
        val helper = resolvedPermissionsHelper
        return helper?.permissionState?.value?.hasSystemAlertWindow == true
    }

    private fun showOverlayMathChallenge(
        appName: String,
        packageName: String,
        difficulty: MathDifficulty
    ) {
        val success = backgroundOverlayManager.showMathChallengeOverlay(
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

        // Fallback to service approach
        Log.w("HomeViewModel", "Failed to show math challenge overlay, using service fallback")
        val intent = Intent(appContext, OverlayService::class.java).apply {
            action = OverlayService.ACTION_SHOW_MATH_CHALLENGE
            putExtra(OverlayService.EXTRA_APP_NAME, appName)
            putExtra(OverlayService.EXTRA_PACKAGE_NAME, packageName)
            putExtra(OverlayService.EXTRA_DIFFICULTY, difficulty.storageValue)
        }
        startOverlayServiceSafely(intent, requireForeground = true)
    }

    private fun startOverlayServiceSafely(intent: Intent, requireForeground: Boolean) {
        try {
            if (requireForeground && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(appContext, intent)
            } else {
                appContext.startService(intent)
            }
            if (BuildConfig.DEBUG) Log.d("HomeViewModel", "Overlay service started successfully")
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
        viewModelScope.launch {
            _events.emit(HomeEvent.RequestOverlayPermission)
        }
    }

    fun callContact(contact: ContactInfo) {
        contactHelper?.callContact(contact)
        viewModelScope.launch {
            searchInteractionRepository?.recordContactAction(contact.id, ContactAction.CALL)
        }
        clearSearch()
    }

    fun messageContact(contact: ContactInfo) {
        contactHelper?.messageContact(contact)
        viewModelScope.launch {
            searchInteractionRepository?.recordContactAction(contact.id, ContactAction.MESSAGE)
        }
        clearSearch()
    }

    fun whatsAppContact(contact: ContactInfo) {
        contactHelper?.whatsAppContact(contact)
        viewModelScope.launch {
            searchInteractionRepository?.recordContactAction(contact.id, ContactAction.WHATSAPP)
        }
        clearSearch()
    }

    fun openContact(contact: ContactInfo) {
        contactHelper?.openContact(contact)
        viewModelScope.launch {
            searchInteractionRepository?.recordContactAction(contact.id, ContactAction.OPEN)
        }
        clearSearch()
    }

    private fun updateWeatherData(
        savedLat: Double?,
        savedLon: Double?,
        display: WeatherDisplayOption
    ) {
        weatherUpdateJob?.cancel()
        weatherUpdateJob = viewModelScope.launch {
            IdlingResourceHelper.increment()
            try {
                val location = if (savedLat != null && savedLon != null) {
                    Pair(savedLat, savedLon)
                } else {
                    // Try to get current location
                    if (resolvedPermissionsHelper?.hasLocationPermission() == true) {
                        withContext(Dispatchers.IO) {
                            weatherService.getCurrentLocation()
                        }
                    } else {
                        null
                    }
                }

                if (location != null) {
                    // Save the location if we got it fresh
                    if (savedLat == null || savedLon == null) {
                        settingsRepository.updateWeatherLocation(location.first, location.second)
                    }

                    val result = weatherService.getCurrentWeather(location.first, location.second)
                    result?.fold(
                        onSuccess = { weatherData ->
                            _uiState.value = _uiState.value.copy(
                                weatherData = weatherData,
                                weatherError = null
                            )
                        },
                        onFailure = { error ->
                            _uiState.value = _uiState.value.copy(
                                weatherData = null,
                                weatherError = "Failed to get weather: ${error.message}"
                            )
                        }
                    )

                    if (display == WeatherDisplayOption.DAILY) {
                        val dailyResult = weatherService.getDailyWeather(location.first, location.second)
                        if (dailyResult != null) {
                            dailyResult.fold(
                                onSuccess = { dailyData ->
                                    val today = dailyData.firstOrNull()
                                    val shouldClearError = _uiState.value.weatherData != null
                                    _uiState.value = _uiState.value.copy(
                                        weatherDailyHigh = today?.temperatureMax,
                                        weatherDailyLow = today?.temperatureMin,
                                        weatherError = if (shouldClearError) null else _uiState.value.weatherError
                                    )
                                },
                                onFailure = { error ->
                                    _uiState.value = _uiState.value.copy(
                                        weatherDailyHigh = null,
                                        weatherDailyLow = null,
                                        weatherError = "Failed to get daily weather: ${error.message}"
                                    )
                                }
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                weatherDailyHigh = null,
                                weatherDailyLow = null
                            )
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            weatherDailyHigh = null,
                            weatherDailyLow = null
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        weatherData = null,
                        weatherError = "Location permission required",
                        weatherDailyHigh = null,
                        weatherDailyLow = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    weatherData = null,
                    weatherError = "Weather update failed: ${e.message}",
                    weatherDailyHigh = null,
                    weatherDailyLow = null
                )
            } finally {
                IdlingResourceHelper.decrement()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (isReceiverRegistered) {
            try {
                appContext.unregisterReceiver(sessionExpiryReceiver)
                isReceiverRegistered = false
            } catch (e: IllegalArgumentException) {
                // Receiver was not registered, which is fine
                if (BuildConfig.DEBUG) Log.d("HomeViewModel", "Broadcast receiver was not registered")
            } catch (e: Exception) {
                // Log other unexpected errors but don't crash
                Log.e("HomeViewModel", "Unexpected error unregistering broadcast receiver", e)
            }
        }
        hideOverlay()
        countdownJob?.cancel()
        weatherUpdateJob?.cancel()
        contactSearchJob?.cancel()
    }

    // Methods for app drawer functionality moved to home screen

    private suspend fun getRecentApps(
        visibleApps: List<AppInfo>,
        hiddenApps: List<AppInfo>,
        limit: Int,
        hasPermission: Boolean
    ): List<AppInfo> = withContext(Dispatchers.Default) {
        if (!hasPermission || usageStatsHelper == null) {
            return@withContext emptyList()
        }

        val sanitizedLimit = limit.coerceAtLeast(0)
        if (sanitizedLimit == 0) {
            return@withContext emptyList()
        }

        val usageStats = usageStatsHelper
            .getPast48HoursUsageStats(hasPermission)
            .filter { it.timeInForeground > 0 }
            .sortedByDescending { it.timeInForeground }

        val hiddenPackages = hiddenApps.mapTo(mutableSetOf()) { it.packageName }
        val appMap = visibleApps.associateBy { it.packageName }
        val recentApps = mutableListOf<AppInfo>()
        val seenPackages = mutableSetOf<String>()

        for (usageApp in usageStats) {
            if (usageApp.packageName in hiddenPackages) {
                continue
            }

            val app = appMap[usageApp.packageName] ?: continue

            if (!seenPackages.add(app.packageName)) {
                continue
            }

            recentApps += app

            if (recentApps.size == sanitizedLimit) {
                break
            }
        }

        if (recentApps.size < sanitizedLimit) {
            val fallbackApps = visibleApps
                .asSequence()
                .filterNot { it.packageName in hiddenPackages }
                .filterNot { it.packageName in seenPackages }
                .sortedBy { it.appName.lowercase() }
                .toList()

            for (app in fallbackApps) {
                recentApps += app
                seenPackages += app.packageName
                if (recentApps.size == sanitizedLimit) {
                    break
                }
            }
        }

        recentApps
    }

    private fun buildAlphabetIndex(
        apps: List<AppInfo>,
        recentApps: List<AppInfo>
    ): List<AlphabetIndexEntry> {
        val entries = mutableListOf<AlphabetIndexEntry>()

        if (recentApps.isNotEmpty()) {
            entries += AlphabetIndexEntry(
                key = RECENT_APPS_INDEX_KEY,
                displayLabel = RECENT_APPS_INDEX_KEY,
                targetIndex = null,
                hasApps = true,
                previewAppName = recentApps.firstOrNull()?.appName
            )
        }

        val appsByFirstChar = apps.groupBy { app ->
            val firstChar = app.appName.firstOrNull()?.toString()?.uppercase()
            when {
                firstChar == null -> "#"
                firstChar.matches(Regex("[A-Z]")) -> firstChar
                else -> "#"
            }
        }

        val alphabet = ('A'..'Z').map { it.toString() } + listOf("#")

        alphabet.forEach { char ->
            val appsForChar = appsByFirstChar[char] ?: emptyList()
            entries += AlphabetIndexEntry(
                key = char,
                displayLabel = char,
                targetIndex = if (appsForChar.isNotEmpty()) {
                    // Find the index of the first app with this starting letter
                    apps.indexOfFirst {
                        val firstChar = it.appName.firstOrNull()?.toString()?.uppercase()
                        when {
                            char == "#" -> firstChar == null || !firstChar.matches(Regex("[A-Z]"))
                            else -> firstChar == char
                        }
                    }.takeIf { it != -1 }
                } else null,
                hasApps = appsForChar.isNotEmpty(),
                previewAppName = appsForChar.firstOrNull()?.appName
            )
        }

        return entries
    }

    fun onAlphabetIndexFocused(entry: AlphabetIndexEntry, fraction: Float) {
        _uiState.value = _uiState.value.copy(
            alphabetIndexActiveKey = entry.key
        )
    }

    fun onAlphabetScrubbingChanged(isScrubbing: Boolean) {
        if (!isScrubbing) {
            _uiState.value = _uiState.value.copy(
                alphabetIndexActiveKey = null
            )
        }
    }

    fun showAppActionDialog(app: AppInfo) {
        _uiState.value = _uiState.value.copy(
            showAppActionDialog = true,
            selectedAppForAction = app
        )
    }

    fun dismissAppActionDialog() {
        _uiState.value = _uiState.value.copy(
            showAppActionDialog = false,
            selectedAppForAction = null
        )
    }

    fun renameApp(app: AppInfo) {
        // For now, just dismiss the dialog - renaming would require additional UI
        dismissAppActionDialog()
    }

    fun hideApp(packageName: String) {
        viewModelScope.launch {
            try {
                appRepository.hideApp(packageName)
                dismissAppActionDialog()
            } catch (e: Exception) {
                errorHandler?.showError("Failed to hide app", e.message ?: "Unknown error", e)
            }
        }
    }

    fun unhideApp(packageName: String) {
        viewModelScope.launch {
            try {
                appRepository.unhideApp(packageName)
                dismissAppActionDialog()
            } catch (e: Exception) {
                errorHandler?.showError("Failed to unhide app", e.message ?: "Unknown error", e)
            }
        }
    }

    fun openAppInfo(packageName: String) {
        try {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
            dismissAppActionDialog()
        } catch (e: Exception) {
            errorHandler?.showError("Failed to open app info", e.message ?: "Unknown error", e)
        }
    }

    fun uninstallApp(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
            dismissAppActionDialog()
        } catch (e: Exception) {
            errorHandler?.showError("Failed to uninstall app", e.message ?: "Unknown error", e)
        }
    }
}

private data class TimeLimitPromptState(
    val appName: String,
    val usageMinutes: Int?,
    val limitMinutes: Int,
    val usesDefaultLimit: Boolean
)

data class HomeUiState(
    val allVisibleApps: List<AppInfo> = emptyList(),
    val hiddenApps: List<AppInfo> = emptyList(),
    val currentTime: String = "",
    val currentDate: String = "",
    val showTime: Boolean = true,
    val showDate: Boolean = true,
    val showWallpaper: Boolean = true,
    val backgroundColor: String = "system",
    val backgroundOpacity: Float = 1f,
    val customWallpaperPath: String? = null,
    val searchQuery: String = "",
    val searchResults: List<AppInfo> = emptyList(),
    val contactResults: List<ContactInfo> = emptyList(),
    val unifiedSearchResults: List<SearchItem> = emptyList(),
    val isContactsPermissionMissing: Boolean = false,
    val showContactsPermissionDialog: Boolean = false,
    val showFrictionDialog: Boolean = false,
    val selectedAppForFriction: String? = null,
    val showTimeLimitDialog: Boolean = false,
    val selectedAppForTimeLimit: String? = null,
    val timeLimitDialogAppName: String? = null,
    val timeLimitDialogUsageMinutes: Int? = null,
    val timeLimitDialogTimeLimitMinutes: Int = 0,
    val timeLimitDialogUsesDefaultLimit: Boolean = true,
    val showMathChallengeDialog: Boolean = false,
    val selectedAppForMathChallenge: String? = null,
    val isMathChallengeForExpiredSession: Boolean = false,
    val isMathChallengeForSessionExtension: Boolean = false,
    val mathChallengeDifficulty: MathDifficulty = MathDifficulty.EASY,
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
    val showWhatsAppAction: Boolean = true,
    val weatherDisplay: WeatherDisplayOption = WeatherDisplayOption.DAILY,
    val weatherData: com.talauncher.data.model.WeatherData? = null,
    val weatherError: String? = null,
    val weatherTemperatureUnit: WeatherTemperatureUnit = WeatherTemperatureUnit.CELSIUS,
    val weatherDailyHigh: Double? = null,
    val weatherDailyLow: Double? = null,
    val colorPalette: ColorPaletteOption = ColorPaletteOption.DEFAULT,
    val wallpaperBlurAmount: Float = 0f,
    val enableGlassmorphism: Boolean = true,
    val uiDensity: UiDensityOption = UiDensityOption.COMPACT,
    val enableAnimations: Boolean = true,
    // App drawer functionality moved to home screen
    val recentApps: List<AppInfo> = emptyList(),
    val alphabetIndexEntries: List<AlphabetIndexEntry> = emptyList(),
    val alphabetIndexActiveKey: String? = null,
    val isAlphabetIndexEnabled: Boolean = true,
    val showAppActionDialog: Boolean = false,
    val selectedAppForAction: AppInfo? = null,
    val isOtherAppsExpanded: Boolean = false
)
