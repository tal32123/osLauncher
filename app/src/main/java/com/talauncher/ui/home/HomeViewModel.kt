package com.talauncher.ui.home

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talauncher.R
import com.talauncher.data.model.AppInfo
import com.talauncher.data.model.ColorPaletteOption
import com.talauncher.data.model.AppIconStyleOption
import com.talauncher.data.model.UiDensityOption
import com.talauncher.data.model.WeatherDisplayOption
import com.talauncher.data.model.WeatherTemperatureUnit
import com.talauncher.data.model.AppSectionLayoutOption
import com.talauncher.data.model.AppDisplayStyleOption
import com.talauncher.data.model.IconColorOption
import com.talauncher.data.repository.AppRepository
import com.talauncher.data.repository.SearchInteractionRepository
import com.talauncher.data.repository.SearchInteractionRepository.ContactAction
import com.talauncher.data.repository.SettingsRepository
import com.talauncher.service.WeatherService
import com.talauncher.utils.ContactHelper
import com.talauncher.utils.ContactInfo
import com.talauncher.utils.ErrorHandler
import com.talauncher.utils.PermissionsHelper
import com.talauncher.utils.UsageStatsHelper
import com.talauncher.utils.IdlingResourceHelper
import com.talauncher.utils.SearchScoring
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import com.talauncher.BuildConfig
import com.talauncher.domain.model.AlphabetIndexEntry
import com.talauncher.domain.model.SectionIndex
import com.talauncher.domain.usecases.BuildAlphabetIndexUseCase
import com.talauncher.domain.usecases.FormatTimeUseCase
import com.talauncher.domain.usecases.GetRecentAppsUseCase
import com.talauncher.domain.usecases.SearchAppsUseCase

private const val TAG = "HomeViewModel"

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

sealed interface HomeEvent {
    data object RequestContactsPermission : HomeEvent
}
class HomeViewModel(
    private val appRepository: AppRepository,
    private val settingsRepository: SettingsRepository,
    private val searchInteractionRepository: SearchInteractionRepository? = null,
    private val onLaunchApp: ((String, Int?) -> Unit)? = null,
    private val appContext: Context,
    private val weatherService: WeatherService = WeatherService(appContext),
    initialContactHelper: ContactHelper? = null,
    private val permissionsHelper: PermissionsHelper? = null,
    private val usageStatsHelper: UsageStatsHelper? = null,
    private val errorHandler: ErrorHandler? = null,
    private val formatTimeUseCase: FormatTimeUseCase = FormatTimeUseCase(),
    private val searchAppsUseCase: SearchAppsUseCase = SearchAppsUseCase(),
    private val buildAlphabetIndexUseCase: BuildAlphabetIndexUseCase = BuildAlphabetIndexUseCase(),
    private val getRecentAppsUseCase: GetRecentAppsUseCase = GetRecentAppsUseCase(usageStatsHelper)
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    private val contactHelper: ContactHelper? = initialContactHelper ?: permissionsHelper?.let {
        ContactHelper(appContext, it)
    }
    private val fallbackPermissionsHelper: PermissionsHelper by lazy { PermissionsHelper(appContext) }
    private val resolvedPermissionsHelper: PermissionsHelper?
        get() = permissionsHelper ?: fallbackPermissionsHelper

    private var allVisibleApps: List<AppInfo> = emptyList()
    private var allHiddenApps: List<AppInfo> = emptyList()
    private var pendingContactQuery: String? = null
    private var hasShownContactsPermissionPrompt = false
    private var weatherUpdateJob: Job? = null
    private var contactSearchJob: Job? = null
    private val searchQueryFlow = MutableStateFlow("")

    /**
     * BroadcastReceiver for system time updates.
     *
     * Architecture:
     * - Responds to ACTION_TIME_TICK (sent every minute when time changes)
     * - Responds to ACTION_TIME_CHANGED (manual time changes)
     * - Responds to ACTION_TIMEZONE_CHANGED (timezone changes)
     *
     * Lifecycle Management:
     * - Registered when time/date display is enabled
     * - Unregistered when ViewModel is cleared to prevent memory leaks
     * - Registered with application context (not Activity) to survive config changes
     *
     * Performance:
     * - More efficient than manual polling with delay()
     * - No wasted battery from constant coroutine loops
     * - Updates exactly when needed (minute boundaries)
     */
    private val timeTickReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_TIME_TICK,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED -> {
                    updateTime()
                }
            }
        }
    }

    private var isTimeReceiverRegistered = false

    init {
        observeData()
        updateTime()
        setupDebouncedContactSearch()
        registerTimeReceiver()
    }

    private fun observeData() {
        viewModelScope.launch {
            try {
                combine(
                    appRepository.getAllVisibleApps(),
                    settingsRepository.getSettings()
                ) { allApps, settings ->
                    withContext(Dispatchers.Default) {
                        try {
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
                                Log.e(TAG, "Error getting hidden apps", e)
                                emptyList<AppInfo>()
                            }
                            allHiddenApps = hiddenApps
                            val searchableApps = allApps + hiddenApps
                            // Use SearchAppsUseCase for filtering
                            val filtered = if (currentQuery.isNotBlank()) {
                                searchAppsUseCase.execute(currentQuery, searchableApps)
                            } else {
                                emptyList()
                            }
                            val recentAppsLimit = settings?.recentAppsLimit ?: 10
                            // Use GetRecentAppsUseCase for recent apps calculation
                            val recentApps = try {
                                getRecentAppsUseCase.execute(
                                    allApps,
                                    hiddenApps,
                                    recentAppsLimit,
                                    hasUsageStatsPermission
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Error getting recent apps", e)
                                errorHandler?.showError(
                                    "Recent Apps Error",
                                    "Failed to load recent apps: ${e.message}",
                                    e
                                )
                                emptyList()
                            }
                            // Use BuildAlphabetIndexUseCase for alphabet index
                            val alphabetIndex = try {
                                buildAlphabetIndexUseCase.execute(allApps, recentApps)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error building alphabet index", e)
                                errorHandler?.showError(
                                    "Alphabet Index Error",
                                    "Failed to build alphabet index: ${e.message}",
                                    e
                                )
                                emptyList()
                            }
                            // Get pinned apps
                            val pinnedApps = try {
                                appRepository.getPinnedApps().first()
                            } catch (e: Exception) {
                                Log.e(TAG, "Error getting pinned apps", e)
                                emptyList<AppInfo>()
                            }
                            // Build enhanced SectionIndex for per-app fast scrolling
                            val sectionIndex = try {
                                buildAlphabetIndexUseCase.buildSectionIndex(allApps, recentApps, pinnedApps)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error building section index", e)
                                errorHandler?.showError(
                                    "Section Index Error",
                                    "Failed to build section index: ${e.message}",
                                    e
                                )
                                SectionIndex.EMPTY
                            }

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
                            searchResults = filtered,
                            showPhoneAction = settings?.showPhoneAction ?: true,
                            showMessageAction = settings?.showMessageAction ?: true,
                            showWhatsAppAction = (settings?.showWhatsAppAction ?: true) && isWhatsAppInstalled,
                            weatherDisplay = weatherDisplay,
                            weatherTemperatureUnit = settings?.weatherTemperatureUnit
                                ?: WeatherTemperatureUnit.CELSIUS,
                            colorPalette = settings?.colorPalette ?: ColorPaletteOption.DEFAULT,
                            appIconStyle = settings?.appIconStyle ?: AppIconStyleOption.ORIGINAL,
                            wallpaperBlurAmount = settings?.wallpaperBlurAmount ?: 0f,
                            customWallpaperPath = settings?.customWallpaperPath,
                            enableGlassmorphism = settings?.enableGlassmorphism ?: true,
                            uiDensity = settings?.uiDensity ?: UiDensityOption.COMPACT,
                            enableAnimations = settings?.enableAnimations ?: true,
                            // App drawer functionality moved to home screen
                            pinnedApps = pinnedApps,
                            recentApps = recentApps,
                            alphabetIndexEntries = alphabetIndex,
                            sectionIndex = sectionIndex,
                            isAlphabetIndexEnabled = allApps.isNotEmpty(),

                            // App section display settings
                            pinnedAppsLayout = settings?.pinnedAppsLayout ?: AppSectionLayoutOption.LIST,
                            pinnedAppsDisplayStyle = settings?.pinnedAppsDisplayStyle ?: AppDisplayStyleOption.ICON_AND_TEXT,
                            pinnedAppsIconColor = settings?.pinnedAppsIconColor ?: IconColorOption.ORIGINAL,

                            recentAppsLayout = settings?.recentAppsLayout ?: AppSectionLayoutOption.LIST,
                            recentAppsDisplayStyle = settings?.recentAppsDisplayStyle ?: AppDisplayStyleOption.ICON_AND_TEXT,
                            recentAppsIconColor = settings?.recentAppsIconColor ?: IconColorOption.ORIGINAL,

                            allAppsLayout = settings?.allAppsLayout ?: AppSectionLayoutOption.LIST,
                            allAppsDisplayStyle = settings?.allAppsDisplayStyle ?: AppDisplayStyleOption.ICON_AND_TEXT,
                            allAppsIconColor = settings?.allAppsIconColor ?: IconColorOption.ORIGINAL,

                            searchLayout = settings?.searchLayout ?: AppSectionLayoutOption.LIST,
                            searchDisplayStyle = settings?.searchDisplayStyle ?: AppDisplayStyleOption.ICON_AND_TEXT,
                            searchIconColor = settings?.searchIconColor ?: IconColorOption.ORIGINAL,

                            // Sidebar customization from settings
                            sidebarActiveScale = settings?.sidebarActiveScale ?: 1.4f,
                            sidebarPopOutDp = settings?.sidebarPopOutDp ?: 16,
                            sidebarWaveSpread = settings?.sidebarWaveSpread ?: 1.5f,
                            fastScrollerActiveItemScale = settings?.fastScrollerActiveItemScale ?: 1.06f
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
                        } catch (e: Exception) {
                            Log.e(TAG, "Error in observeData processing", e)
                            errorHandler?.showError(
                                "Data Processing Error",
                                "Failed to process app data: ${e.message}",
                                e
                            )
                        }
                    }
                }.collect { }
            } catch (e: Exception) {
                Log.e(TAG, "Critical error in observeData flow", e)
                errorHandler?.showError(
                    "Critical Error",
                    "Failed to initialize home screen: ${e.message}",
                    e
                )
            }
        }
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

    /**
     * Registers the BroadcastReceiver for time updates.
     *
     * SOLID Principle - Single Responsibility:
     * - This method only handles receiver registration
     * - Separate from time formatting logic
     *
     * Android Best Practices:
     * - Uses application context to avoid memory leaks
     * - Registers for multiple time-related intents
     * - Guarded against multiple registrations
     */
    private fun registerTimeReceiver() {
        if (isTimeReceiverRegistered) return

        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_TIME_TICK)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            }
            appContext.registerReceiver(timeTickReceiver, filter)
            isTimeReceiverRegistered = true
            Log.d(TAG, "Time receiver registered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register time receiver", e)
        }
    }

    /**
     * Unregisters the BroadcastReceiver for time updates.
     *
     * Lifecycle Management:
     * - Called in onCleared() to prevent memory leaks
     * - Guarded against unregistering when not registered
     */
    private fun unregisterTimeReceiver() {
        if (!isTimeReceiverRegistered) return

        try {
            appContext.unregisterReceiver(timeTickReceiver)
            isTimeReceiverRegistered = false
            Log.d(TAG, "Time receiver unregistered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister time receiver", e)
        }
    }

    /**
     * Updates the current time and date in the UI state.
     *
     * Thread-Safety:
     * - Uses thread-safe FormatTimeUseCase with DateTimeFormatter
     * - No synchronization needed
     *
     * Performance:
     * - Lightweight operation, safe to call from BroadcastReceiver
     * - Updates happen on main thread via coroutine
     */
    private fun updateTime() {
        viewModelScope.launch {
            val result = formatTimeUseCase.execute()
            _uiState.value = _uiState.value.copy(
                currentTime = result.time,
                currentDate = result.date
            )
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
                return@launch
            }

            val launched = appRepository.launchApp(packageName)
            if (launched) {
                searchInteractionRepository?.recordAppLaunch(packageName)
                clearSearch()
                onLaunchApp?.invoke(packageName, null)
            }
        }
    }


    fun updateSearchQuery(query: String) {
        val sanitized = query.trimStart()
        searchQueryFlow.value = sanitized

        val appResults = if (sanitized.isBlank()) {
            emptyList<AppInfo>()
        } else {
            searchAppsUseCase.execute(sanitized, allVisibleApps + allHiddenApps)
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

    fun dismissTimeLimitDialog() {
        _uiState.value = _uiState.value.copy(
            showTimeLimitDialog = false,
            selectedAppForTimeLimit = null,
            timeLimitDialogAppName = null,
            timeLimitDialogUsageMinutes = null,
            timeLimitDialogTimeLimitMinutes = 0,
            timeLimitDialogUsesDefaultLimit = true
        )
    }

    fun launchAppWithTimeLimit(packageName: String, durationMinutes: Int) {
        viewModelScope.launch {
            val launched = appRepository.launchApp(
                packageName,
                plannedDuration = durationMinutes
            )

            if (launched) {
                searchInteractionRepository?.recordAppLaunch(packageName)
                clearSearch()
                onLaunchApp?.invoke(packageName, durationMinutes)
                dismissTimeLimitDialog()
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
        unregisterTimeReceiver()
        weatherUpdateJob?.cancel()
        contactSearchJob?.cancel()
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
            selectedAppForAction = app,
            selectedAppSupportsUninstall = canUninstallApp(app.packageName)
        )
    }

    fun dismissAppActionDialog() {
        _uiState.value = _uiState.value.copy(
            showAppActionDialog = false,
            selectedAppForAction = null,
            selectedAppSupportsUninstall = false
        )
    }

    fun renameApp(app: AppInfo) {
        dismissAppActionDialog()
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

    fun markAppAsDistracting(packageName: String) {
        viewModelScope.launch {
            try {
                appRepository.updateDistractingStatus(packageName, true)
                dismissAppActionDialog()
            } catch (e: Exception) {
                errorHandler?.showError(
                    "Failed to mark app as distracting",
                    e.message ?: "Unknown error",
                    e
                )
            }
        }
    }

    fun unmarkAppAsDistracting(packageName: String) {
        viewModelScope.launch {
            try {
                appRepository.updateDistractingStatus(packageName, false)
                dismissAppActionDialog()
            } catch (e: Exception) {
                errorHandler?.showError(
                    "Failed to remove distracting status",
                    e.message ?: "Unknown error",
                    e
                )
            }
        }
    }

    fun pinApp(packageName: String) {
        viewModelScope.launch {
            try {
                appRepository.pinApp(packageName)
                dismissAppActionDialog()
            } catch (e: Exception) {
                errorHandler?.showError(
                    "Failed to pin app",
                    e.message ?: "Unknown error",
                    e
                )
            }
        }
    }

    fun unpinApp(packageName: String) {
        viewModelScope.launch {
            try {
                appRepository.unpinApp(packageName)
                dismissAppActionDialog()
            } catch (e: Exception) {
                errorHandler?.showError(
                    "Failed to unpin app",
                    e.message ?: "Unknown error",
                    e
                )
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
            if (!_uiState.value.selectedAppSupportsUninstall) {
                return
            }
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

    private fun canUninstallApp(packageName: String): Boolean {
        if (packageName == appContext.packageName || packageName == "android.settings") {
            return false
        }

        return try {
            val appInfo = appContext.packageManager.getApplicationInfo(packageName, 0)
            val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            !isSystemApp
        } catch (_: PackageManager.NameNotFoundException) {
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to determine if $packageName is uninstallable", e)
            false
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
    val showTimeLimitDialog: Boolean = false,
    val selectedAppForTimeLimit: String? = null,
    val timeLimitDialogAppName: String? = null,
    val timeLimitDialogUsageMinutes: Int? = null,
    val timeLimitDialogTimeLimitMinutes: Int = 0,
    val timeLimitDialogUsesDefaultLimit: Boolean = true,
    val isLoading: Boolean = false,
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
    val appIconStyle: AppIconStyleOption = AppIconStyleOption.ORIGINAL,
    val wallpaperBlurAmount: Float = 0f,
    val enableGlassmorphism: Boolean = true,
    val uiDensity: UiDensityOption = UiDensityOption.COMPACT,
    val enableAnimations: Boolean = true,
    // App drawer functionality moved to home screen
    val pinnedApps: List<AppInfo> = emptyList(),
    val recentApps: List<AppInfo> = emptyList(),
    val alphabetIndexEntries: List<AlphabetIndexEntry> = emptyList(),
    val sectionIndex: SectionIndex = SectionIndex.EMPTY,
    val alphabetIndexActiveKey: String? = null,
    val isAlphabetIndexEnabled: Boolean = true,
    // Sidebar customization
    val sidebarActiveScale: Float = 1.4f,
    val sidebarPopOutDp: Int = 16,
    val sidebarWaveSpread: Float = 1.5f,
    val fastScrollerActiveItemScale: Float = 1.06f,
    val showAppActionDialog: Boolean = false,
    val selectedAppForAction: AppInfo? = null,
    val selectedAppSupportsUninstall: Boolean = false,
    val isOtherAppsExpanded: Boolean = false,
    val appBeingRenamed: AppInfo? = null,
    val renameInput: String = "",

    // App section display settings
    val pinnedAppsLayout: AppSectionLayoutOption = AppSectionLayoutOption.LIST,
    val pinnedAppsDisplayStyle: AppDisplayStyleOption = AppDisplayStyleOption.ICON_AND_TEXT,
    val pinnedAppsIconColor: IconColorOption = IconColorOption.ORIGINAL,

    val recentAppsLayout: AppSectionLayoutOption = AppSectionLayoutOption.LIST,
    val recentAppsDisplayStyle: AppDisplayStyleOption = AppDisplayStyleOption.ICON_AND_TEXT,
    val recentAppsIconColor: IconColorOption = IconColorOption.ORIGINAL,

    val allAppsLayout: AppSectionLayoutOption = AppSectionLayoutOption.LIST,
    val allAppsDisplayStyle: AppDisplayStyleOption = AppDisplayStyleOption.ICON_AND_TEXT,
    val allAppsIconColor: IconColorOption = IconColorOption.ORIGINAL,

    val searchLayout: AppSectionLayoutOption = AppSectionLayoutOption.LIST,
    val searchDisplayStyle: AppDisplayStyleOption = AppDisplayStyleOption.ICON_AND_TEXT,
    val searchIconColor: IconColorOption = IconColorOption.ORIGINAL
)
