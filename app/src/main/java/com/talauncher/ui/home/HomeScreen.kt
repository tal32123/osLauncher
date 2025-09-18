package com.talauncher.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.talauncher.R
import com.talauncher.data.model.AppInfo
import com.talauncher.ui.components.ContactItem
import com.talauncher.ui.components.GoogleSearchItem
import com.talauncher.ui.components.MathChallengeDialog
import com.talauncher.ui.components.SessionExpiryActionDialog
import com.talauncher.ui.components.SessionExpiryCountdownDialog
import com.talauncher.ui.components.TimeLimitDialog
import com.talauncher.ui.home.MotivationalQuotesProvider
import com.talauncher.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToAppDrawer: (() -> Unit)? = null,
    onNavigateToSettings: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery = uiState.searchQuery
    val searchResults = uiState.searchResults
    val contactResults = uiState.contactResults
    val isSearching = searchQuery.isNotBlank()
    val keyboardController = LocalSoftwareKeyboardController.current
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current

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
            while (true) {
                viewModel.refreshTime()
                kotlinx.coroutines.delay(60000) // Only refresh if time/date is shown
            }
        }
    }

    // Check for expired sessions when home screen is displayed
    LaunchedEffect(Unit) {
        viewModel.checkExpiredSessionsManually()
    }


    // Determine background modifier based on settings
    val backgroundModifier = if (uiState.showWallpaper) {
        // Show wallpaper - transparent background to allow wallpaper to show through
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
    } else {
        // Show solid background color
        val backgroundColor = when (uiState.backgroundColor) {
            "black" -> Color.Black
            "white" -> Color.White
            "system" -> MaterialTheme.colorScheme.background
            else -> {
                // Try to parse as hex color, fallback to system background
                try {
                    Color(android.graphics.Color.parseColor(uiState.backgroundColor))
                } catch (e: Exception) {
                    MaterialTheme.colorScheme.background
                }
            }
        }
        Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .systemBarsPadding()
    }

    Box(
        modifier = backgroundModifier
    ) {
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
                    Text(
                        text = uiState.currentTime,
                        style = MaterialTheme.typography.displayLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
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

            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = PrimerSpacing.lg),
                placeholder = { Text(stringResource(R.string.home_search_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (searchQuery.isNotBlank()) {
                            // Search Google as the primary action
                            viewModel.performGoogleSearch(searchQuery)
                            keyboardController?.hide()
                        }
                    }
                ),
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        TextButton(onClick = {
                            viewModel.clearSearch()
                            keyboardController?.hide()
                        }) {
                            Text(stringResource(R.string.home_search_clear))
                        }
                    }
                }
            )

            if (isSearching) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(PrimerSpacing.xs)
                ) {
                    // Always show Google search as first option
                    item {
                        GoogleSearchItem(
                            query = searchQuery,
                            onClick = {
                                keyboardController?.hide()
                                viewModel.performGoogleSearch(searchQuery)
                            }
                        )
                    }

                    // Show app search results
                    if (searchResults.isNotEmpty()) {
                        items(searchResults, key = { it.packageName }) { app ->
                            SearchResultItem(
                                appInfo = app,
                                onClick = {
                                    keyboardController?.hide()
                                    viewModel.launchApp(app.packageName)
                                }
                            )
                        }
                    }

                    // Show contact search results
                    if (contactResults.isNotEmpty()) {
                        items(contactResults, key = { it.id }) { contact ->
                            ContactItem(
                                contact = contact,
                                onCall = {
                                    keyboardController?.hide()
                                    viewModel.callContact(contact)
                                },
                                onMessage = {
                                    keyboardController?.hide()
                                    viewModel.messageContact(contact)
                                }
                            )
                        }
                    }

                    if (uiState.isContactsPermissionMissing && searchQuery.isNotBlank()) {
                        item {
                            ContactPermissionCallout(
                                onGrantAccess = {
                                    keyboardController?.hide()
                                    viewModel.showContactsPermissionPrompt()
                                }
                            )
                        }
                    }

                    // Show no results message only if both app and contact results are empty
                    if (searchResults.isEmpty() && contactResults.isEmpty() && searchQuery.isNotBlank() && !uiState.isContactsPermissionMissing) {
                        item {
                            Text(
                                text = stringResource(R.string.home_search_no_results),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(PrimerSpacing.md)
                            )
                        }
                    }
                }
            } else {
                // Pinned Apps Section
                if (uiState.pinnedApps.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(PrimerSpacing.xs)
                    ) {
                        items(uiState.pinnedApps, key = { it.packageName }) { app ->
                            PinnedAppItem(
                                appInfo = app,
                                onClick = { viewModel.launchApp(app.packageName) },
                                onLongClick = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.unpinApp(app.packageName)
                                }
                            )
                        }
                    }
                } else {
                    // Empty state - GitHub style
                    PrimerCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = PrimerSpacing.sm),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(PrimerSpacing.xl),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No pinned apps",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(PrimerSpacing.sm))
                            Text(
                                text = "Swipe right to see all apps\nLong press any app to pin it here",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
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
                var appName by remember(selectedPackage) { mutableStateOf(selectedPackage) }

                LaunchedEffect(selectedPackage) {
                    appName = withContext(Dispatchers.IO) {
                        try {
                            val packageManager = context.packageManager
                            val appInfo = packageManager.getApplicationInfo(selectedPackage, 0)
                            packageManager.getApplicationLabel(appInfo).toString()
                        } catch (e: Exception) {
                            selectedPackage
                        }
                    }
                }

                TimeLimitDialog(
                    appName = appName,
                    onConfirm = { durationMinutes ->
                        viewModel.launchAppWithTimeLimit(selectedPackage, durationMinutes)
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
                    difficulty = "medium", // Could be made configurable
                    onCorrect = {
                        viewModel.onMathChallengeCompleted(selectedPackage)
                    },
                    onDismiss = { viewModel.dismissMathChallengeDialog() },
                    isTimeExpired = uiState.isMathChallengeForExpiredSession
                )
            }
        }
    }
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
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Type your reason here...") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    shape = PrimerShapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimerBlue,
                        unfocusedBorderColor = PrimerGray300
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
                enabled = reason.trim().isNotEmpty()
            ) {
                Text("Continue")
            }
        },
        dismissButton = {
            PrimerSecondaryButton(
                onClick = onDismiss
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

@Composable
fun ContactPermissionCallout(
    onGrantAccess: () -> Unit
) {
    PrimerCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PrimerSpacing.md),
            verticalArrangement = Arrangement.spacedBy(PrimerSpacing.sm)
        ) {
            Text(
                text = stringResource(R.string.contact_permission_missing_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            OutlinedButton(
                onClick = onGrantAccess,
                shape = PrimerShapes.small,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            ) {
                Text(stringResource(R.string.contact_permission_required_confirm))
            }
        }
    }
}
