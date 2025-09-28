package com.talauncher.ui.home

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListDefaults
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.foundation.layout.width
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.isActive
import com.talauncher.R
import com.talauncher.data.model.AppInfo
import com.talauncher.data.model.UiDensityOption
import com.talauncher.data.model.WeatherDisplayOption
import com.talauncher.ui.components.ContactItem
import com.talauncher.ui.components.GoogleSearchItem
import com.talauncher.ui.components.MathChallengeDialog
import com.talauncher.ui.components.SessionExpiryActionDialog
import com.talauncher.ui.components.SessionExpiryCountdownDialog
import com.talauncher.ui.components.TimeLimitDialog
import com.talauncher.ui.components.ModernGlassCard
import com.talauncher.ui.components.ModernSearchField
import com.talauncher.ui.components.ModernAppItem
import com.talauncher.ui.components.ModernBackdrop
import com.talauncher.ui.components.UiDensity
import com.talauncher.ui.components.UnifiedSearchResults
import com.talauncher.ui.theme.UiSettings
import com.talauncher.ui.home.MotivationalQuotesProvider
import com.talauncher.ui.home.SearchItem
import com.talauncher.ui.theme.*
import com.talauncher.utils.PermissionType
import com.talauncher.utils.PermissionsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext

private fun UiDensityOption.toUiDensity(): UiDensity = when (this) {
    UiDensityOption.COMPACT -> UiDensity.Compact
    UiDensityOption.SPACIOUS -> UiDensity.Spacious
    UiDensityOption.COMFORTABLE -> UiDensity.Comfortable
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    permissionsHelper: PermissionsHelper,
    onNavigateToAppDrawer: (() -> Unit)? = null,
    onNavigateToSettings: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery = uiState.searchQuery
    val searchResults = uiState.searchResults
    val contactResults = uiState.contactResults
    val unifiedSearchResults = uiState.unifiedSearchResults
    val isSearching = searchQuery.isNotBlank()
    val keyboardController = LocalSoftwareKeyboardController.current
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current

    LaunchedEffect(viewModel, permissionsHelper, context) {
        viewModel.events.collect { event ->
            val activity = context.findActivity()
            if (activity == null) {
                Log.w("HomeScreen", "Unable to handle event $event without Activity context")
                return@collect
            }

            when (event) {
                HomeEvent.RequestContactsPermission ->
                    permissionsHelper.requestPermission(activity, PermissionType.CONTACTS)
                HomeEvent.RequestOverlayPermission ->
                    permissionsHelper.requestPermission(activity, PermissionType.SYSTEM_ALERT_WINDOW)
            }
        }
    }

    // Handle back button for dialogs - prioritize dialogs over navigation
    BackHandler(
        enabled = uiState.showFrictionDialog ||
            uiState.showTimeLimitDialog ||
            uiState.showMathChallengeDialog ||
            uiState.showSessionExpiryCountdown ||
            uiState.showSessionExpiryDecisionDialog
    ) {
        when {
            uiState.showSessionExpiryCountdown -> {
                // Countdown cannot be dismissed
            }
            uiState.showSessionExpiryDecisionDialog -> {
                // Force the user to make a decision
            }
            uiState.showMathChallengeDialog -> {
                // Math challenge cannot be dismissed with back button - force completion
            }
            uiState.showTimeLimitDialog -> {
                // Time limit dialog cannot be dismissed with back button - force choice
            }
            uiState.showFrictionDialog -> {
                // Friction dialog can be dismissed to respect user choice
                viewModel.dismissFrictionDialog()
            }
        }
    }

    LaunchedEffect(uiState.showTime, uiState.showDate) {
        if (uiState.showTime || uiState.showDate) {
            viewModel.refreshTime() // Initial refresh
            // Use a timer instead of infinite loop to allow event queue to go idle during tests
            while (coroutineContext.isActive) {
                kotlinx.coroutines.delay(60000) // Only refresh if time/date is shown
                if (coroutineContext.isActive) {
                    viewModel.refreshTime()
                }
            }
        }
    }

    // Check for expired sessions when home screen is displayed
    LaunchedEffect(Unit) {
        viewModel.checkExpiredSessionsManually()
    }


    // Use modern backdrop with new design system
    ModernBackdrop(
        showWallpaper = uiState.showWallpaper,
        blurAmount = uiState.wallpaperBlurAmount,
        backgroundColor = uiState.backgroundColor,
        opacity = uiState.backgroundOpacity,
        customWallpaperPath = uiState.customWallpaperPath,
        modifier = Modifier.systemBarsPadding()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            // Time and Date Section - GitHub clean typography
            if (uiState.showTime || uiState.showDate) {
                Spacer(modifier = Modifier.height(PrimerSpacing.xxxl))

                if (uiState.showTime) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uiState.currentTime,
                            style = MaterialTheme.typography.displayLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        // Weather display next to time
                        if (uiState.weatherDisplay != WeatherDisplayOption.OFF && uiState.weatherData != null) {
                            Spacer(modifier = Modifier.width(12.dp))
                            val shouldShowTemperature = uiState.weatherDisplay != WeatherDisplayOption.DAILY ||
                                uiState.weatherDailyHigh == null || uiState.weatherDailyLow == null
                            com.talauncher.ui.components.WeatherDisplay(
                                weatherData = uiState.weatherData,
                                showTemperature = shouldShowTemperature,
                                temperatureUnit = uiState.weatherTemperatureUnit,
                                dailyHigh = if (uiState.weatherDisplay == WeatherDisplayOption.DAILY) uiState.weatherDailyHigh else null,
                                dailyLow = if (uiState.weatherDisplay == WeatherDisplayOption.DAILY) uiState.weatherDailyLow else null
                            )
                        }
                    }
                }

                if (uiState.showDate) {
                    Spacer(modifier = Modifier.height(PrimerSpacing.sm))
                    Text(
                        text = uiState.currentDate,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(PrimerSpacing.xl))

            ModernSearchField(
                value = searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                placeholder = stringResource(R.string.home_search_placeholder),
                enableGlassmorphism = uiState.enableGlassmorphism,
                testTag = "search_field",
                onSearch = { query ->
                    if (query.isNotBlank()) {
                        viewModel.performGoogleSearch(query)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = PrimerSpacing.lg)
            )

            if (isSearching) {
                UnifiedSearchResults(
                    searchQuery = searchQuery,
                    searchResults = unifiedSearchResults,
                    onAppClick = { packageName ->
                        viewModel.launchApp(packageName)
                    },
                    onAppLongClick = { searchItem ->
                        // Remove pin/unpin functionality
                    },
                    onContactCall = { searchItem ->
                        viewModel.callContact(searchItem.contactInfo)
                    },
                    onContactMessage = { searchItem ->
                        viewModel.messageContact(searchItem.contactInfo)
                    },
                    onContactWhatsApp = { searchItem ->
                        viewModel.whatsAppContact(searchItem.contactInfo)
                    },
                    onContactOpen = { searchItem ->
                        viewModel.openContact(searchItem.contactInfo)
                    },
                    showPhoneAction = uiState.showPhoneAction,
                    showMessageAction = uiState.showMessageAction,
                    showWhatsAppAction = uiState.showWhatsAppAction,
                    onGoogleSearch = { query ->
                        viewModel.performGoogleSearch(query)
                    },
                    showContactsPermissionMissing = uiState.isContactsPermissionMissing,
                    onGrantContactsPermission = {
                        viewModel.showContactsPermissionPrompt()
                    },
                    uiSettings = UiSettings(
                        colorPalette = uiState.colorPalette,
                        enableGlassmorphism = uiState.enableGlassmorphism,
                        enableAnimations = uiState.enableAnimations,
                        uiDensity = uiState.uiDensity,
                        showWallpaper = uiState.showWallpaper,
                        wallpaperBlurAmount = uiState.wallpaperBlurAmount,
                        backgroundColor = uiState.backgroundColor
                    ),
                    modifier = Modifier.weight(1f)
                )
            } else {
                // Apps Section with Recent Apps and Alphabet Index
                val listState = rememberLazyListState()

                // Handle alphabet index scrolling
                LaunchedEffect(uiState.alphabetIndexActiveKey) {
                    val activeKey = uiState.alphabetIndexActiveKey
                    if (activeKey != null) {
                        val entry = uiState.alphabetIndexEntries.find { it.key == activeKey }
                        if (entry != null) {
                            if (entry.key == RECENT_APPS_INDEX_KEY) {
                                listState.animateScrollToItem(0)
                            } else {
                                entry.targetIndex?.let { targetIndex ->
                                    // Adjust index to account for recent apps section
                                    val adjustedIndex = if (uiState.recentApps.isNotEmpty()) {
                                        // Add header + recent apps + spacer + "All Apps" header
                                        targetIndex + uiState.recentApps.size + 3
                                    } else {
                                        targetIndex
                                    }
                                    listState.animateScrollToItem(adjustedIndex)
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f).testTag("app_list"),
                        verticalArrangement = Arrangement.spacedBy(PrimerSpacing.xs),
                        contentPadding = PaddingValues(bottom = 80.dp), // Extra bottom padding for accessibility
                        flingBehavior = LazyListDefaults.flingBehavior()
                    ) {
                        // Recently Used Apps Section
                        if (uiState.recentApps.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Recent Apps",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(
                                        start = PrimerSpacing.md,
                                        top = PrimerSpacing.md,
                                        bottom = PrimerSpacing.sm
                                    )
                                )
                            }

                            items(uiState.recentApps, key = { "recent_${it.packageName}" }) { app ->
                                RecentAppItem(
                                    appInfo = app,
                                    onClick = { viewModel.launchApp(app.packageName) },
                                    onLongClick = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.showAppActionDialog(app)
                                    }
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(PrimerSpacing.lg))
                                Text(
                                    text = "All Apps",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(
                                        start = PrimerSpacing.md,
                                        bottom = PrimerSpacing.sm
                                    )
                                )
                            }
                        }

                        // All Apps Section
                        items(uiState.allVisibleApps, key = { it.packageName }) { app ->
                            ModernAppItem(
                                appName = app.appName,
                                onClick = { viewModel.launchApp(app.packageName) },
                                onLongClick = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.showAppActionDialog(app)
                                },
                                enableGlassmorphism = uiState.enableGlassmorphism,
                                uiDensity = uiState.uiDensity.toUiDensity()
                            )
                        }
                    }

                    // Alphabet Index on the right side
                    if (uiState.alphabetIndexEntries.isNotEmpty()) {
                        AlphabetIndex(
                            entries = uiState.alphabetIndexEntries,
                            activeKey = uiState.alphabetIndexActiveKey,
                            isEnabled = uiState.isAlphabetIndexEnabled,
                            modifier = Modifier.padding(end = PrimerSpacing.sm).testTag("alphabet_index"),
                            onEntryFocused = { entry, fraction ->
                                viewModel.onAlphabetIndexFocused(entry, fraction)
                            },
                            onScrubbingChanged = { isScrubbing ->
                                viewModel.onAlphabetScrubbingChanged(isScrubbing)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(PrimerSpacing.xl))
        }

        // Gesture Detection Overlays - Invisible areas for reliable gesture capture

        // Top area swipe down for notifications
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(100.dp) // Top swipe area
                .pointerInput("top_swipe") {
                    detectDragGestures { change, dragAmount ->
                        // Swipe down from top area
                        if (dragAmount.y > 80 && change.position.y < 100) {
                            try {
                                @Suppress("DEPRECATION")
                                val statusBarService = context.getSystemService("statusbar")
                                val statusBarClass = Class.forName("android.app.StatusBarManager")
                                val method = statusBarClass.getMethod("expandNotificationsPanel")
                                method.invoke(statusBarService)
                            } catch (e: Exception) {
                                // Fallback - some devices might not support this
                            }
                        }
                    }
                }
        )


        // App Action Dialog (moved from App Drawer)
        if (uiState.showAppActionDialog) {
            AppActionDialog(
                app = uiState.selectedAppForAction,
                onDismiss = { viewModel.dismissAppActionDialog() },
                onRename = { app -> viewModel.renameApp(app) },
                onHide = { packageName -> viewModel.hideApp(packageName) },
                onAppInfo = { packageName -> viewModel.openAppInfo(packageName) },
                onUninstall = { packageName -> viewModel.uninstallApp(packageName) }
            )
        }

        // Friction barrier dialog for distracting apps
        if (uiState.showFrictionDialog) {
            run {
                val selectedPackage = uiState.selectedAppForFriction ?: return@run
                FrictionDialog(
                    appPackageName = selectedPackage,
                    onDismiss = { viewModel.dismissFrictionDialog() },
                    onProceed = { reason ->
                        viewModel.launchAppWithReason(selectedPackage, reason)
                    }
                )
            }
        }

        // Time limit dialog for distracting apps
        if (uiState.showTimeLimitDialog) {
            run {
                val selectedPackage = uiState.selectedAppForTimeLimit ?: return@run
                TimeLimitDialog(
                    appName = uiState.timeLimitDialogAppName ?: selectedPackage,
                    usageMinutes = uiState.timeLimitDialogUsageMinutes,
                    timeLimitMinutes = uiState.timeLimitDialogTimeLimitMinutes,
                    isUsingDefaultLimit = uiState.timeLimitDialogUsesDefaultLimit,
                    onConfirm = {
                        viewModel.launchAppWithTimeLimit(
                            selectedPackage,
                            uiState.timeLimitDialogTimeLimitMinutes
                        )
                    },
                    onDismiss = { viewModel.dismissTimeLimitDialog() }
                )
            }
        }

        if (uiState.showSessionExpiryCountdown) {
            val appName = uiState.sessionExpiryAppName ?: "this app"
            SessionExpiryCountdownDialog(
                appName = appName,
                remainingSeconds = uiState.sessionExpiryCountdownRemaining,
                totalSeconds = uiState.sessionExpiryCountdownTotal
            )
        }

        if (uiState.showSessionExpiryDecisionDialog) {
            val appName = uiState.sessionExpiryAppName ?: "this app"
            SessionExpiryActionDialog(
                appName = appName,
                showMathChallengeOption = uiState.sessionExpiryShowMathOption,
                onExtend = { viewModel.onSessionExpiryDecisionExtend() },
                onClose = { viewModel.onSessionExpiryDecisionClose() },
                onMathChallenge = if (uiState.sessionExpiryShowMathOption) {
                    { viewModel.onSessionExpiryDecisionMathChallenge() }
                } else null
            )
        }

        if (uiState.showContactsPermissionDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissContactsPermissionDialog() },
                title = {
                    Text(
                        text = stringResource(R.string.contact_permission_required_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.contact_permission_required_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        keyboardController?.hide()
                        viewModel.requestContactsPermission()
                    }) {
                        Text(stringResource(R.string.contact_permission_required_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissContactsPermissionDialog() }) {
                        Text(stringResource(R.string.contact_permission_required_dismiss))
                    }
                }
            )
        }

        if (uiState.showOverlayPermissionDialog) {
            val appName = uiState.sessionExpiryAppName
                ?: stringResource(id = R.string.overlay_permission_generic_app)
            AlertDialog(
                onDismissRequest = { viewModel.dismissOverlayPermissionDialog() },
                title = {
                    Text(
                        text = stringResource(id = R.string.overlay_permission_required_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Text(
                        text = stringResource(
                            id = R.string.overlay_permission_required_message,
                            appName
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.openOverlayPermissionSettings() }) {
                        Text(stringResource(id = R.string.overlay_permission_required_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissOverlayPermissionDialog() }) {
                        Text(stringResource(id = R.string.overlay_permission_required_dismiss))
                    }
                }
            )
        }

        // Math challenge dialog for closing apps
        if (uiState.showMathChallengeDialog) {
            run {
                val selectedPackage = uiState.selectedAppForMathChallenge ?: return@run
                MathChallengeDialog(
                    difficulty = uiState.mathChallengeDifficulty,
                    onCorrect = {
                        viewModel.onMathChallengeCompleted(selectedPackage)
                    },
                    onDismiss = { viewModel.dismissMathChallengeDialog() },
                    isTimeExpired = uiState.isMathChallengeForExpiredSession
                )
            }
        }
        } // End Box
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun FrictionDialog(
    appPackageName: String,
    onDismiss: () -> Unit,
    onProceed: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    val context = LocalContext.current
    val quotesProvider = remember(context) { MotivationalQuotesProvider(context) }
    val motivationalQuote = remember(appPackageName, quotesProvider) {
        quotesProvider.getRandomQuote()
    }

    // Get app name for display
    var appName by remember(appPackageName) { mutableStateOf(appPackageName) }

    LaunchedEffect(appPackageName) {
        appName = withContext(Dispatchers.IO) {
            try {
                val packageManager = context.packageManager
                val appInfo = packageManager.getApplicationInfo(appPackageName, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                appPackageName
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("friction_dialog"),
        title = {
            Text(
                text = "Mindful Usage",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "You're trying to open $appName, which you've marked as distracting.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Why do you need to open this app right now?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    modifier = Modifier.fillMaxWidth().testTag("friction_reason_input"),
                    placeholder = { Text("Type your reason here...") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    shape = PrimerShapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )

                if (motivationalQuote.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = motivationalQuote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            PrimerButton(
                onClick = {
                    if (reason.trim().isNotEmpty()) {
                        onProceed(reason.trim())
                    }
                },
                enabled = reason.trim().isNotEmpty(),
                modifier = Modifier.testTag("friction_continue_button")
            ) {
                Text("Continue")
            }
        },
        dismissButton = {
            PrimerSecondaryButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("friction_cancel_button")
            ) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = PrimerShapes.medium
    )
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchResultItem(
    appInfo: AppInfo,
    onClick: () -> Unit
) {
    PrimerCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PrimerSpacing.md)
                .heightIn(min = PrimerListItemDefaults.minHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = appInfo.appName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PinnedAppItem(
    appInfo: AppInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    PrimerCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PrimerSpacing.md)
                .heightIn(min = PrimerListItemDefaults.minHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = appInfo.appName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// Components moved from App Drawer to Home Screen

@Composable
private fun AlphabetIndex(
    entries: List<AlphabetIndexEntry>,
    activeKey: String?,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
    onEntryFocused: (AlphabetIndexEntry, Float) -> Unit,
    onScrubbingChanged: (Boolean) -> Unit
) {
    var componentSize by remember { mutableStateOf(IntSize.Zero) }
    val entryBounds = remember(entries) { mutableStateMapOf<String, ClosedFloatingPointRange<Float>>() }

    fun resolveEntry(positionY: Float): Pair<AlphabetIndexEntry, Float>? {
        if (entries.isEmpty() || componentSize.height == 0 || entryBounds.isEmpty()) {
            return null
        }
        val totalHeight = componentSize.height.toFloat()
        val boundsList = entries.mapNotNull { entry ->
            entryBounds[entry.key]?.let { bounds -> entry to bounds }
        }
        if (boundsList.isEmpty()) {
            return null
        }

        val clampedY = positionY.coerceIn(0f, totalHeight)
        val candidate = boundsList.firstOrNull { (_, bounds) ->
            clampedY in bounds
        } ?: boundsList.minByOrNull { (_, bounds) ->
            when {
                clampedY < bounds.start -> bounds.start - clampedY
                clampedY > bounds.endInclusive -> clampedY - bounds.endInclusive
                else -> 0f
            }
        } ?: return null

        val (entry, bounds) = candidate
        val center = (bounds.start + bounds.endInclusive) / 2f
        val fraction = if (totalHeight > 0f) {
            (center / totalHeight).coerceIn(0f, 1f)
        } else {
            0f
        }
        return entry to fraction
    }

    Box(
        modifier = modifier
            .width(48.dp)
            .fillMaxHeight()
            .onSizeChanged { componentSize = it }
            .pointerInput(entries, isEnabled) {
                if (!isEnabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    onScrubbingChanged(true)
                    resolveEntry(down.position.y)?.let { (entry, fraction) ->
                        onEntryFocused(entry, fraction)
                    }
                    try {
                        drag(down.id) { change ->
                            resolveEntry(change.position.y)?.let { (entry, fraction) ->
                                onEntryFocused(entry, fraction)
                            }
                            change.consume()
                        }
                    } finally {
                        onScrubbingChanged(false)
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = PrimerSpacing.md),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            entries.forEach { entry ->
                val isActive = isEnabled && entry.hasApps && entry.key == activeKey
                val color = when {
                    !isEnabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    entry.hasApps && isActive -> MaterialTheme.colorScheme.onSurface
                    entry.hasApps -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                }
                Text(
                    text = entry.displayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    modifier = Modifier
                        .testTag("alphabet_index_entry_${entry.key}")
                        .onGloballyPositioned { coordinates ->
                            val position = coordinates.positionInParent()
                            val start = position.y
                            val end = start + coordinates.size.height
                            entryBounds[entry.key] = start..end
                        }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentAppItem(
    appInfo: AppInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    PrimerCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PrimerSpacing.md)
                .heightIn(min = PrimerListItemDefaults.minHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = appInfo.appName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Show a "recent" indicator
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = PrimerShapes.small,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "Recent",
                    modifier = Modifier.padding(
                        horizontal = PrimerSpacing.xs,
                        vertical = 2.dp
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun AppActionDialog(
    app: AppInfo?,
    onDismiss: () -> Unit,
    onRename: (AppInfo) -> Unit,
    onHide: (String) -> Unit,
    onAppInfo: (String) -> Unit,
    onUninstall: (String) -> Unit
) {
    if (app != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            modifier = Modifier.testTag("app_action_dialog"),
            title = {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Choose an action:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(PrimerSpacing.sm)
                    ) {

                        ActionTextButton(
                            label = stringResource(R.string.rename_app_action_label),
                            description = stringResource(R.string.rename_app_action_description),
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                onRename(app)
                                onDismiss()
                            }
                        )

                        ActionTextButton(
                            label = "Hide app",
                            description = "Move this app to the hidden list.",
                            modifier = Modifier.fillMaxWidth().testTag("hide_app_button"),
                            onClick = {
                                onHide(app.packageName)
                                onDismiss()
                            }
                        )

                        ActionTextButton(
                            label = "View app info",
                            description = "Open the system settings page for this app.",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                onAppInfo(app.packageName)
                                onDismiss()
                            }
                        )

                        ActionTextButton(
                            label = "Uninstall app",
                            description = "Remove this app from your device.",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                onUninstall(app.packageName)
                                onDismiss()
                            }
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = PrimerShapes.medium
        )
    }
}

@Composable
private fun ActionTextButton(
    label: String,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = PrimerSpacing.xs),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (description.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


