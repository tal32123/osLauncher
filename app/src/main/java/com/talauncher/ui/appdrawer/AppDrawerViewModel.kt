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
import com.talauncher.data.model.MathDifficulty
import com.talauncher.data.repository.AppRepository
import com.talauncher.data.repository.SettingsRepository
import com.talauncher.utils.PermissionsHelper
import com.talauncher.utils.UsageStatsHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import java.util.concurrent.TimeUnit
import com.talauncher.utils.ContactHelper
import com.talauncher.utils.ContactInfo
import com.talauncher.ui.theme.UiSettings
import com.talauncher.ui.theme.toUiSettingsOrDefault
import java.text.Collator
import java.util.Locale
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

private const val RECENT_SECTION_KEY = "recent"

data class AppDrawerSection(
    val key: String,
    val label: String,
    val apps: List<AppInfo>,
    val isIndexable: Boolean
)

data class AlphabetIndexEntry(
    val key: String,
    val displayLabel: String,
    val targetIndex: Int?,
    val hasApps: Boolean,
    val previewAppName: String?,
    val rangeStartIndex: Int?,
    val rangeEndIndexExclusive: Int?
)

private data class SectionPosition(
    val headerIndex: Int,
    val firstAppIndex: Int?,
    val endIndexExclusive: Int,
    val previewAppName: String?
)

private fun sectionKeyForApp(label: String, locale: Locale): String {
    val trimmed = label.trim()
    if (trimmed.isEmpty()) {
        return "#"
    }
    val firstChar = trimmed.firstOrNull { it.isLetterOrDigit() } ?: return "#"
    if (!firstChar.isLetter()) {
        return "#"
    }
    val upper = firstChar.toString().uppercase(locale)
    return upper.take(1)
}

data class AppDrawerUiState(
    val sections: List<AppDrawerSection> = emptyList(),
    val allApps: List<AppInfo> = emptyList(),
    val hiddenApps: List<AppInfo> = emptyList(),
    val recentApps: List<AppInfo> = emptyList(), // Most used apps from past 48 hours respecting user limit
    val contacts: List<ContactInfo> = emptyList(),
    val isLoading: Boolean = false,
    val selectedAppForAction: AppInfo? = null,
    val appBeingRenamed: AppInfo? = null,
    val renameInput: String = "",
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
    val mathChallengeDifficulty: MathDifficulty = MathDifficulty.EASY,
    val recentAppsLimit: Int = 5,
    val searchQuery: String = "",
    val showPhoneAction: Boolean = true,
    val showMessageAction: Boolean = true,
    val showWhatsAppAction: Boolean = true,
    val uiSettings: UiSettings = UiSettings(),
    val alphabetIndexEntries: List<AlphabetIndexEntry> = emptyList(),
    val alphabetIndexActiveKey: String? = null,
    val isAlphabetIndexEnabled: Boolean = true,
    val locale: Locale? = null,
    val collator: java.text.Collator? = null
)

class AppDrawerViewModel(
    private val appRepository: AppRepository,
    private val settingsRepository: SettingsRepository,
    private val usageStatsHelper: UsageStatsHelper,
    private val permissionsHelper: PermissionsHelper,
    private val contactHelper: ContactHelper,
    private val context: Context? = null,
    private val onLaunchApp: ((String, Int?) -> Unit)? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppDrawerUiState())
    val uiState: StateFlow<AppDrawerUiState> = _uiState.asStateFlow()
    private val searchQueryFlow = MutableStateFlow("")
    private var contactSearchJob: Job? = null

    init {
        observeData()
        setupDebouncedContactSearch()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                appRepository.getAllVisibleApps(),
                appRepository.getHiddenApps(),
                settingsRepository.getSettings(),
                permissionsHelper.permissionState
            ) { visibleApps, hiddenApps, settings, permissionState ->
                val recentLimit = settings?.recentAppsLimit ?: 5
                val recentApps = getRecentApps(
                    visibleApps = visibleApps,
                    hiddenApps = hiddenApps,
                    limit = recentLimit,
                    hasPermission = permissionState.hasUsageStats
                )

                val uiSettings = settings.toUiSettingsOrDefault()
                val isWhatsAppInstalled = withContext(Dispatchers.Default) {
                    contactHelper.isWhatsAppInstalled()
                }

                withContext(Dispatchers.Main.immediate) {
                    val newState = _uiState.value.copy(
                        allApps = visibleApps,
                        hiddenApps = hiddenApps,
                        recentApps = recentApps,
                        mathChallengeDifficulty = settings?.mathDifficulty ?: MathDifficulty.EASY,
                        recentAppsLimit = recentLimit,
                        showPhoneAction = settings?.showPhoneAction ?: true,
                        showMessageAction = settings?.showMessageAction ?: true,
                        showWhatsAppAction = (settings?.showWhatsAppAction ?: true) && isWhatsAppInstalled,
                        uiSettings = uiSettings
                    )
                    _uiState.value = newState

                    val updatedState = newState
                    val locale = updatedState.locale ?: Locale.getDefault()
                    val collator = updatedState.collator ?: createCollator(locale)
                    val searchQuery = updatedState.searchQuery
                    buildSectionsAndIndex(
                        visibleApps,
                        recentApps,
                        searchQuery,
                        locale,
                        collator
                    )
                }
            }.collect { }
        }
    }

    private suspend fun getRecentApps(
        visibleApps: List<AppInfo>,
        hiddenApps: List<AppInfo>,
        limit: Int,
        hasPermission: Boolean
    ): List<AppInfo> = withContext(Dispatchers.Default) {
        if (!hasPermission) {
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

    fun launchApp(packageName: String) {
        viewModelScope.launch {
            // Check if we should show time limit prompt first
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

            // Try to launch the app through repository (handles friction checks)
            val launched = appRepository.launchApp(packageName)
            if (launched) {
                // App was launched successfully, use callback if available
                onLaunchApp?.invoke(packageName, null)
            }
            else {
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
            selectedAppForTimeLimit = null,
            timeLimitDialogAppName = null,
            timeLimitDialogUsageMinutes = null,
            timeLimitDialogTimeLimitMinutes = 0,
            timeLimitDialogUsesDefaultLimit = true
        )
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
        val hasPermission = permissionsHelper.permissionState.value.hasUsageStats
        if (!hasPermission) {
            return null
        }

        val usageStats = usageStatsHelper.getTodayUsageStats(true)
        val usageMillis = usageStats.firstOrNull { it.packageName == packageName }?.timeInForeground ?: 0
        return TimeUnit.MILLISECONDS.toMinutes(usageMillis).toInt()
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

        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                putExtra(Intent.EXTRA_RETURN_RESULT, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: SecurityException) {
            // System denied the uninstall request. This can happen if the permission was revoked.
            android.util.Log.e("AppDrawerViewModel", "Unable to request uninstall for $packageName", e)
            
        } catch (e: Exception) {
            android.util.Log.e("AppDrawerViewModel", "Failed to launch uninstall intent for $packageName", e)
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
            } catch (e: Exception) {
                Log.e("AppDrawerViewModel", "Failed to open Google search", e)
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun setupDebouncedContactSearch() {
        viewModelScope.launch {
            searchQueryFlow
                .debounce(300) // 300ms debounce
                .collect { query ->
                    if (query.isNotBlank()) {
                        searchContacts(query)
                    } else {
                        _uiState.value = _uiState.value.copy(contacts = emptyList())
                    }
                }
        }
    }

    private fun searchContacts(query: String) {
        contactSearchJob?.cancel()
        contactSearchJob = viewModelScope.launch {
            try {
                val contacts = withContext(Dispatchers.IO) {
                    contactHelper.searchContacts(query)
                }
                _uiState.value = _uiState.value.copy(contacts = contacts)
            } catch (e: Exception) {
                Log.e("AppDrawerViewModel", "Error searching contacts", e)
                _uiState.value = _uiState.value.copy(contacts = emptyList())
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        searchQueryFlow.value = query

        // Execute search with usage stats lookup on background thread
        viewModelScope.launch {
            val apps = _uiState.value.allApps
            val recentApps = _uiState.value.recentApps
            val locale = _uiState.value.locale ?: Locale.getDefault()
            val collator = _uiState.value.collator ?: createCollator(locale)

            buildSectionsAndIndexAsync(apps, recentApps, query, locale, collator)
        }
    }

    fun clearSearchOnNavigation() {
        contactSearchJob?.cancel()
        searchQueryFlow.value = ""
        _uiState.value = _uiState.value.copy(searchQuery = "", contacts = emptyList())
    }

    override fun onCleared() {
        super.onCleared()
        contactSearchJob?.cancel()
    }

    fun refreshApps() {
        viewModelScope.launch {
            try {
                appRepository.syncInstalledApps()
            }
            catch (e: Exception) {
                Log.e("AppDrawerViewModel", "Failed to refresh apps", e)
            }
        }
    }

    fun callContact(contact: ContactInfo) {
        contactHelper.callContact(contact)
    }

    fun messageContact(contact: ContactInfo) {
        contactHelper.messageContact(contact)
    }

    fun whatsAppContact(contact: ContactInfo) {
        contactHelper.whatsAppContact(contact)
    }

    fun openContact(contact: ContactInfo) {
        contactHelper.openContact(contact)
    }

    fun onAlphabetIndexFocused(entry: AlphabetIndexEntry, @Suppress("UNUSED_PARAMETER") fraction: Float) {
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

    fun onLocaleChanged(locale: Locale, collator: Collator) {
        collator.strength = Collator.PRIMARY
        collator.decomposition = Collator.CANONICAL_DECOMPOSITION
        _uiState.value = _uiState.value.copy(locale = locale, collator = collator)
        buildSectionsAndIndex(
            _uiState.value.allApps,
            _uiState.value.recentApps,
            _uiState.value.searchQuery,
            locale,
            collator
        )
    }

    private fun createCollator(locale: Locale): Collator {
        return Collator.getInstance(locale).apply {
            strength = Collator.PRIMARY
            decomposition = Collator.CANONICAL_DECOMPOSITION
        }
    }


    private suspend fun buildSectionsAndIndexAsync(apps: List<AppInfo>, recentApps: List<AppInfo>, searchQuery: String, locale: Locale, collator: Collator) {
        val filteredApps = if (searchQuery.isBlank()) {
            apps.filter { !it.isHidden }
        } else {
            // Use fuzzy search with recency scoring for better results
            val hasUsageStatsPermission = permissionsHelper.permissionState.value.hasUsageStats
            val usageStats = if (hasUsageStatsPermission) {
                withContext(Dispatchers.IO) {
                    usageStatsHelper.getPast48HoursUsageStats(true).associateBy { it.packageName }
                }
            } else {
                emptyMap()
            }

            apps.filter { !it.isHidden }
                .mapNotNull { app ->
                    val baseScore = calculateRelevanceScore(searchQuery, app.appName)
                    if (baseScore > 0) {
                        val recencyScore = calculateRecencyScore(app.packageName, usageStats)
                        app to (baseScore + recencyScore)
                    } else null
                }
                .sortedByDescending { it.second }
                .map { it.first }
        }

        buildSectionsAndIndexSync(filteredApps, recentApps, searchQuery, locale, collator)
    }

    private fun buildSectionsAndIndex(apps: List<AppInfo>, recentApps: List<AppInfo>, searchQuery: String, locale: Locale, collator: Collator) {
        val filteredApps = if (searchQuery.isBlank()) {
            apps.filter { !it.isHidden }
        } else {
            // For non-search queries or when called synchronously, skip usage stats to avoid blocking
            apps.filter { !it.isHidden }
                .mapNotNull { app ->
                    val baseScore = calculateRelevanceScore(searchQuery, app.appName)
                    if (baseScore > 0) {
                        app to baseScore
                    } else null
                }
                .sortedByDescending { it.second }
                .map { it.first }
        }

        buildSectionsAndIndexSync(filteredApps, recentApps, searchQuery, locale, collator)
    }

    private fun buildSectionsAndIndexSync(filteredApps: List<AppInfo>, recentApps: List<AppInfo>, searchQuery: String, locale: Locale, collator: Collator) {

        val sortedFilteredApps = if (searchQuery.isBlank()) {
            filteredApps.sortedWith { left, right ->
                val labelComparison = collator.compare(left.appName, right.appName)
                if (labelComparison != 0) {
                    labelComparison
                } else {
                    left.packageName.compareTo(right.packageName)
                }
            }
        } else {
            filteredApps // Already sorted by relevance score
        }

        val sections = buildList {
            if (recentApps.isNotEmpty() && searchQuery.isEmpty()) {
                add(
                    AppDrawerSection(
                        key = RECENT_SECTION_KEY,
                        label = "Recently Used",
                        apps = recentApps,
                        isIndexable = false
                    )
                )
            }

            if (sortedFilteredApps.isNotEmpty()) {
                val grouped = linkedMapOf<String, MutableList<AppInfo>>()
                sortedFilteredApps.forEach { app ->
                    val sectionKey = sectionKeyForApp(app.appName, locale)
                    val sectionApps = grouped.getOrPut(sectionKey) { mutableListOf() }
                    sectionApps += app
                }
                grouped.forEach { (key, apps) ->
                    add(
                        AppDrawerSection(
                            key = key,
                            label = key,
                            apps = apps,
                            isIndexable = true
                        )
                    )
                }
            }
        }

        val sectionPositions = mutableMapOf<String, SectionPosition>()
        var currentIndex = 0
        sections.forEach { section ->
            if (section.isIndexable && section.apps.isNotEmpty()) {
                val headerIndex = currentIndex
                val firstAppIndex = if (section.apps.isNotEmpty()) headerIndex + 1 else null
                val endIndexExclusive = headerIndex + 1 + section.apps.size
                sectionPositions[section.key] = SectionPosition(
                    headerIndex = headerIndex,
                    firstAppIndex = firstAppIndex,
                    endIndexExclusive = endIndexExclusive,
                    previewAppName = section.apps.first().appName
                )
            }
            currentIndex += 1 + section.apps.size
        }

        val alphabetIndexEntries = if (sectionPositions.isEmpty()) {
            emptyList()
        } else {
            val baseAlphabet = ('A'..'Z').map { it.toString() }
            val sectionKeys = sections.filter { it.isIndexable }.map { it.key }
            val extraKeys = sectionKeys.filter { it !in baseAlphabet && it != "#" }
            val orderedKeys = (baseAlphabet + extraKeys + listOf("#")).distinct()
            orderedKeys.map { key ->
                val position = sectionPositions[key]
                AlphabetIndexEntry(
                    key = key,
                    displayLabel = key,
                    targetIndex = position?.firstAppIndex ?: position?.headerIndex,
                    hasApps = position != null,
                    previewAppName = position?.previewAppName,
                    rangeStartIndex = position?.headerIndex,
                    rangeEndIndexExclusive = position?.endIndexExclusive
                )
            }
        }

        _uiState.value = _uiState.value.copy(
            sections = sections,
            alphabetIndexEntries = alphabetIndexEntries,
            isAlphabetIndexEnabled = alphabetIndexEntries.isNotEmpty()
        )
    }

    private fun calculateRelevanceScore(query: String, name: String): Int {
        val normalizedQuery = query.trim().lowercase()
        val normalizedName = name.lowercase()

        return when {
            normalizedName == normalizedQuery -> 100 // Exact match
            normalizedName.startsWith(normalizedQuery) -> 80 // Starts with
            normalizedName.contains(" $normalizedQuery") -> 60 // Word boundary match
            normalizedName.contains(normalizedQuery) -> 40 // Contains
            else -> calculateFuzzyScore(normalizedQuery, normalizedName) // Fuzzy match
        }
    }

    private fun calculateFuzzyScore(query: String, target: String): Int {
        if (query.isEmpty() || target.isEmpty()) return 0

        // Calculate Levenshtein distance ratio
        val distance = calculateLevenshteinDistance(query, target)
        val maxLength = max(query.length, target.length)
        val similarity = 1.0 - (distance.toDouble() / maxLength)

        // Only consider fuzzy matches above a threshold
        return if (similarity >= 0.6) {
            (similarity * 35).toInt() // Max fuzzy score of 35
        } else {
            0
        }
    }

    private fun calculateLevenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }

        return dp[s1.length][s2.length]
    }

    private fun calculateRecencyScore(packageName: String, usageStats: Map<String, com.talauncher.data.model.AppUsage>): Int {
        val usageApp = usageStats[packageName] ?: return 0

        // Calculate recency boost based on usage time and frequency
        val usageTimeHours = TimeUnit.MILLISECONDS.toHours(usageApp.timeInForeground)
        val now = System.currentTimeMillis()
        val lastUsedHours = TimeUnit.MILLISECONDS.toHours(now - usageApp.lastTimeUsed)

        // Exponential decay for recency (higher boost for more recent usage)
        val recencyFactor = exp(-lastUsedHours / 24.0) // Decay over 24 hours

        // Usage time factor (more used apps get higher boost)
        val usageFactor = min(usageTimeHours / 2.0, 5.0) // Cap at 5 points

        return (recencyFactor * usageFactor * 20).toInt() // Max recency boost of ~20 points
    }
}

private data class TimeLimitPromptState(
    val appName: String,
    val usageMinutes: Int?,
    val limitMinutes: Int,
    val usesDefaultLimit: Boolean
)
