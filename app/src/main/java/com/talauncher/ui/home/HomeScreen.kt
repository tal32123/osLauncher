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
// import androidx.compose.foundation.lazy.LazyListDefaults
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
import com.talauncher.data.model.AppIconStyleOption
import com.talauncher.data.model.WeatherDisplayOption
import com.talauncher.domain.model.RECENT_APPS_INDEX_KEY
import com.talauncher.ui.components.ContactItem
import com.talauncher.ui.components.GoogleSearchItem
import com.talauncher.ui.components.ModernGlassCard
import com.talauncher.ui.components.ModernSearchField
import com.talauncher.ui.components.ModernAppItem
import com.talauncher.ui.components.AppIcon
import com.talauncher.ui.components.ModernBackdrop
import com.talauncher.ui.components.UiDensity
import com.talauncher.ui.components.UnifiedSearchResults
import com.talauncher.ui.theme.UiSettings
import com.talauncher.ui.theme.*
import com.talauncher.ui.components.TimeLimitDialog
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
    val hasActiveDialog = uiState.showFrictionDialog ||
        uiState.showTimeLimitDialog

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
            }
        }
    }

    // Handle back button for dialogs - prioritize dialogs over navigation
    BackHandler(
        enabled = hasActiveDialog
    ) {
        when {
            uiState.showTimeLimitDialog -> {
                // Time limit dialog cannot be dismissed with back button - force choice
            }
            uiState.showFrictionDialog -> {
                // Friction dialog can be dismissed to respect user choice
                viewModel.dismissFrictionDialog()
            }
        }
    }

    BackHandler(
        enabled = isSearching && !hasActiveDialog
    ) {
        viewModel.clearSearch()
        keyboardController?.hide()
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

            val searchPlaceholder = runCatching { stringResource(R.string.home_search_placeholder) }.getOrElse { "Search..." }
            val clearDescription = runCatching { stringResource(R.string.home_search_clear) }.getOrElse { "Clear" }

            ModernSearchField(
                value = searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                placeholder = searchPlaceholder,
                enableGlassmorphism = uiState.enableGlassmorphism,
                testTag = "search_field",
                onClear = viewModel::clearSearch,
                clearContentDescription = clearDescription,
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
                        viewModel.showAppActionDialog(searchItem.appInfo)
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
                        appIconStyle = uiState.appIconStyle,
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
                                        // Add header + recent apps + spacer/title before "All Apps"
                                        targetIndex + uiState.recentApps.size + 2
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
                    ) {
                        // Recently Used Apps Section
                        if (uiState.recentApps.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.home_recent_apps_section),
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
                                },
                                iconStyle = uiState.appIconStyle
                            )
                            }

                            item {
                                Spacer(modifier = Modifier.height(PrimerSpacing.lg))
                                Text(
                                    text = stringResource(R.string.home_all_apps_section),
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
                                packageName = app.packageName,
                                onClick = { viewModel.launchApp(app.packageName) },
                                onLongClick = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.showAppActionDialog(app)
                                },
                                appIconStyle = uiState.appIconStyle,
                                enableGlassmorphism = uiState.enableGlassmorphism,
                                uiDensity = uiState.uiDensity.toUiDensity()
                            )
                        }

                        if (uiState.hiddenApps.isNotEmpty()) {
                            item {
                                OtherAppsToggle(
                                    hiddenCount = uiState.hiddenApps.size,
                                    isExpanded = uiState.isOtherAppsExpanded,
                                    enableGlassmorphism = uiState.enableGlassmorphism,
                                    onToggle = { viewModel.toggleOtherAppsExpansion() }
                                )
                            }

                            if (uiState.isOtherAppsExpanded) {
                                items(uiState.hiddenApps, key = { "hidden_${it.packageName}" }) { app ->
                                    ModernAppItem(
                                        appName = app.appName,
                                        packageName = app.packageName,
                                        onClick = { viewModel.launchApp(app.packageName) },
                                        onLongClick = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.showAppActionDialog(app)
                                        },
                                        appIconStyle = uiState.appIconStyle,
                                        enableGlassmorphism = uiState.enableGlassmorphism,
                                        uiDensity = uiState.uiDensity.toUiDensity(),
                                        isHidden = true
                                    )
                                }
                            }
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
                canUninstall = uiState.selectedAppSupportsUninstall,
                onDismiss = { viewModel.dismissAppActionDialog() },
                onRename = { app -> viewModel.renameApp(app) },
                onHide = { packageName -> viewModel.hideApp(packageName) },
                onUnhide = { packageName -> viewModel.unhideApp(packageName) },
                onAppInfo = { packageName -> viewModel.openAppInfo(packageName) },
                onUninstall = { packageName -> viewModel.uninstallApp(packageName) }
            )
        }

        // Rename App Dialog
        RenameAppDialog(
            app = uiState.appBeingRenamed,
            newName = uiState.renameInput,
            onNameChange = { viewModel.updateRenameInput(it) },
            onConfirm = { viewModel.confirmRename() },
            onDismiss = { viewModel.dismissRenameDialog() }
        )

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

        // Contacts permission dialog
        if (uiState.showContactsPermissionDialog) {
            ContactsPermissionDialog(
                onDismiss = { viewModel.dismissContactsPermissionDialog() },
                onConfirm = {
                    keyboardController?.hide()
                    viewModel.requestContactsPermission()
                }
            )
        }

        } // End Box
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}


