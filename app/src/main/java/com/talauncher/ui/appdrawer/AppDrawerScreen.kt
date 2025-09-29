package com.talauncher.ui.appdrawer

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
// import androidx.compose.foundation.lazy.stickyHeader
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
// import androidx.compose.ui.layout.offset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.talauncher.R
import com.talauncher.data.model.AppInfo
import com.talauncher.ui.components.ContactItem
import com.talauncher.ui.components.GoogleSearchItem
import com.talauncher.ui.components.MathChallengeDialog
import com.talauncher.ui.components.TimeLimitDialog
import com.talauncher.ui.components.AppDrawerUnifiedSearchResults
import com.talauncher.ui.components.NiagaraFastScroll
import com.talauncher.ui.theme.*
import com.talauncher.ui.theme.UiSettings
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.Locale

@Composable
fun AppDrawerScreen(
    viewModel: AppDrawerViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val hapticFeedback = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val searchQuery = uiState.searchQuery
    var showHiddenApps by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val layoutDirection = LocalLayoutDirection.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Refresh apps when screen becomes visible
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshApps()
            awaitCancellation()
        }
    }

    val locale = remember(configuration) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            configuration.locale
        }
    }

    val collator = remember(locale) {
        Collator.getInstance(locale).apply {
            strength = Collator.PRIMARY
        }
    }

    LaunchedEffect(locale, collator) {
        viewModel.onLocaleChanged(locale, collator)
    }

    val sections = uiState.sections

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var isNiagaraScrolling by remember { mutableStateOf(false) }

    val showIndex = uiState.searchQuery.isEmpty() && uiState.alphabetIndexEntries.isNotEmpty()

    BackHandler(
        enabled = uiState.showFrictionDialog ||
            uiState.showTimeLimitDialog ||
            uiState.showMathChallengeDialog ||
            uiState.selectedAppForAction != null ||
            uiState.appBeingRenamed != null
    ) {
        when {
            uiState.appBeingRenamed != null -> {
                viewModel.dismissRenameDialog()
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
            uiState.selectedAppForAction != null -> {
                // App action dialog can be dismissed
                viewModel.dismissAppActionDialog()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            PrimerCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PrimerSpacing.md),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = {
                        Text(
                            text = "Search",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Text(
                            text = "Search",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = PrimerSpacing.xs)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    viewModel.updateSearchQuery("")
                                    keyboardController?.hide()
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                contentPadding = PaddingValues(
                                    horizontal = PrimerSpacing.xs,
                                    vertical = 0.dp
                                )
                            ) {
                                Text(
                                    text = "Clear",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (searchQuery.trim().isNotBlank()) {
                                viewModel.performGoogleSearch(searchQuery.trim())
                            }
                            keyboardController?.hide()
                        }
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = PrimerShapes.small
                )
            }

            var selectedTab by remember { mutableIntStateOf(0) }
            val tabs = listOf("Apps", "Contacts")

            if (searchQuery.isNotBlank()) {
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            text = { Text(title) },
                            selected = selectedTab == index,
                            onClick = { selectedTab = index }
                        )
                    }
                }
            } else {
                selectedTab = 0
            }

            Box(
                modifier = Modifier.weight(1f)
            ) {
                if (selectedTab == 0) {
                if (searchQuery.isNotBlank()) {
                    // Use unified search when searching
                    AppDrawerUnifiedSearchResults(
                        searchQuery = searchQuery,
                        allApps = uiState.allApps,
                        onAppClick = { packageName ->
                            viewModel.launchApp(packageName)
                            keyboardController?.hide()
                        },
                        onAppLongClick = { app ->
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.showAppActionDialog(app)
                        },
                        onGoogleSearch = { query ->
                            keyboardController?.hide()
                            viewModel.performGoogleSearch(query)
                        },
                        uiSettings = uiState.uiSettings,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = PrimerSpacing.md,
                                end = PrimerSpacing.md,
                                top = PrimerSpacing.sm,
                                bottom = PrimerSpacing.xl
                            )
                    )
                } else {
                    // Show sectioned apps when not searching
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(
                            start = PrimerSpacing.md,
                            end = PrimerSpacing.md + 56.dp,
                            top = PrimerSpacing.sm,
                            bottom = PrimerSpacing.xl + 80.dp // Extra bottom padding for accessibility
                        ),
                        verticalArrangement = Arrangement.spacedBy(PrimerSpacing.xs),
                    ) {
                        sections.forEachIndexed { index, section ->
                            item {
                                SectionHeader(
                                    label = section.label,
                                    modifier = Modifier
                                        .padding(
                                            top = if (index == 0) PrimerSpacing.sm else PrimerSpacing.xs,
                                            bottom = PrimerSpacing.xs
                                        )
                                )
                            }

                            if (section.key == "recent") { // TODO: Use constant
                                items(section.apps, key = { "recent_${it.packageName}" }) { app ->
                                    RecentAppItem(
                                        appInfo = app,
                                        onClick = {
                                            viewModel.launchApp(app.packageName)
                                            keyboardController?.hide()
                                        },
                                        onLongClick = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.showAppActionDialog(app)
                                        }
                                    )
                                }
                            } else {
                                items(section.apps, key = { it.packageName }) { app ->
                                    AppDrawerItem(
                                        appInfo = app,
                                        onClick = {
                                            viewModel.launchApp(app.packageName)
                                            keyboardController?.hide()
                                        },
                                        onLongClick = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.showAppActionDialog(app)
                                        }
                                    )
                                }
                            }
                        }

                        if (uiState.hiddenApps.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(PrimerSpacing.lg))

                                PrimerCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = PrimerSpacing.sm),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    TextButton(
                                        onClick = {
                                            showHiddenApps = !showHiddenApps
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(PrimerSpacing.sm)
                                    ) {
                                        Text(
                                            text = if (showHiddenApps) {
                                                "Hide hidden apps (${uiState.hiddenApps.size})"
                                            } else {
                                                "Show hidden apps (${uiState.hiddenApps.size})"
                                            },
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        if (showHiddenApps) {
                            item {
                                SectionHeader(
                                    label = "Hidden Apps",
                                    modifier = Modifier.padding(bottom = PrimerSpacing.xs)
                                )
                            }
                            items(uiState.hiddenApps, key = { "hidden_${it.packageName}" }) { app ->
                                HiddenAppItem(
                                    appInfo = app,
                                    onClick = {
                                        viewModel.launchApp(app.packageName)
                                        keyboardController?.hide()
                                    },
                                    onLongClick = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.unhideApp(app.packageName)
                                    }
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(PrimerSpacing.xl))
                        }
                    }
                }
                } else {
                    // Contacts tab - show contacts search results
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = PrimerSpacing.md,
                            end = PrimerSpacing.md,
                            top = PrimerSpacing.sm,
                            bottom = PrimerSpacing.xl + 80.dp // Extra bottom padding for accessibility
                        ),
                        verticalArrangement = Arrangement.spacedBy(PrimerSpacing.xs),
                    ) {
                        items(uiState.contacts) { contact ->
                            ContactItem(
                                contact = contact,
                                onCall = { viewModel.callContact(contact) },
                                onMessage = { viewModel.messageContact(contact) },
                                onWhatsApp = { viewModel.whatsAppContact(contact) },
                                onOpenContact = { viewModel.openContact(contact) },
                                showPhoneAction = uiState.showPhoneAction,
                                showMessageAction = uiState.showMessageAction,
                                showWhatsAppAction = uiState.showWhatsAppAction
                            )
                        }
                    }
                }

                if (showIndex && selectedTab == 0) {
                    val alphabetEntries = uiState.alphabetIndexEntries

                    LaunchedEffect(uiState.scrollToIndex) {
                        uiState.scrollToIndex?.let { index ->
                            coroutineScope.launch {
                                try {
                                    // Use animateScrollToItem for smooth scrolling
                                    listState.animateScrollToItem(index, scrollOffset = 0)
                                } catch (e: Exception) {
                                    // Fallback to instant scroll if animation fails
                                    listState.scrollToItem(index)
                                }
                                viewModel.onScrollHandled()
                            }
                        }
                    }

                    NiagaraFastScroll(
                        entries = alphabetEntries,
                        isEnabled = showIndex,
                        modifier = Modifier
                            .align(
                                if (layoutDirection == LayoutDirection.Rtl) {
                                    Alignment.CenterStart
                                } else {
                                    Alignment.CenterEnd
                                }
                            ),
                        onEntryFocused = { entry, fraction ->
                            viewModel.onAlphabetIndexFocused(entry, fraction)
                        },
                        onScrollingChanged = { active ->
                            viewModel.onAlphabetScrubbingChanged(active)
                            isNiagaraScrolling = active
                            if (active) {
                                keyboardController?.hide()
                            }
                        }
                    )
                }
            }
        }

        AppActionDialog(
            app = uiState.selectedAppForAction,
            onDismiss = viewModel::dismissAppActionDialog,
            onRename = viewModel::startRenamingApp,
            onHide = viewModel::hideApp,
            onAppInfo = { packageName ->
                viewModel.openAppInfo(context, packageName)
            },
            onUninstall = { packageName ->
                viewModel.uninstallApp(context, packageName)
            }
        )

        RenameAppDialog(
            app = uiState.appBeingRenamed,
            newName = uiState.renameInput,
            onNameChange = viewModel::updateRenameInput,
            onConfirm = viewModel::confirmRename,
            onDismiss = viewModel::dismissRenameDialog
        )

        if (uiState.showFrictionDialog) {
            run {
                val selectedPackage = uiState.selectedAppForFriction ?: return@run
                com.talauncher.ui.home.FrictionDialog(
                    appPackageName = selectedPackage,
                    onDismiss = { viewModel.dismissFrictionDialog() },
                    onProceed = { reason ->
                        viewModel.launchAppWithReason(selectedPackage, reason)
                    }
                )
            }
        }

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

        if (uiState.showMathChallengeDialog) {
            run {
                val selectedPackage = uiState.selectedAppForMathChallenge ?: return@run
                MathChallengeDialog(
                    difficulty = uiState.mathChallengeDifficulty,
                    onCorrect = {
                        viewModel.onMathChallengeCompleted(selectedPackage)
                    },
                    onDismiss = { viewModel.dismissMathChallengeDialog() },
                    isTimeExpired = false
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Text(
            text = label,
            modifier = Modifier
                .padding(horizontal = PrimerSpacing.xs, vertical = PrimerSpacing.xs),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}



@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawerItem(
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
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HiddenAppItem(
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
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
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
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f),
                shape = PrimerShapes.small,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(
                    text = "Hidden",
                    modifier = Modifier.padding(
                        horizontal = PrimerSpacing.xs,
                        vertical = 2.dp
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            modifier = Modifier.fillMaxWidth(),
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
fun RenameAppDialog(
    app: AppInfo?,
    newName: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (app != null) {
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current
        val trimmedName = newName.trim()
        val isConfirmEnabled = trimmedName.isNotEmpty() && trimmedName != app.appName

        LaunchedEffect(app.packageName) {
            focusRequester.requestFocus()
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = stringResource(R.string.rename_app_dialog_title, app.appName),
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(PrimerSpacing.sm)
                ) {
                    Text(
                        text = stringResource(R.string.rename_app_dialog_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = newName,
                        onValueChange = onNameChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        label = {
                            Text(stringResource(R.string.rename_app_dialog_field_label))
                        },
                        placeholder = {
                            Text(stringResource(R.string.rename_app_dialog_placeholder))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (isConfirmEnabled) {
                                    onConfirm()
                                    keyboardController?.hide()
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = PrimerShapes.small
                    )

                    Text(
                        text = stringResource(R.string.rename_app_dialog_supporting_text),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (isConfirmEnabled) {
                            onConfirm()
                            keyboardController?.hide()
                        }
                    },
                    enabled = isConfirmEnabled
                ) {
                    Text(stringResource(R.string.rename_app_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        keyboardController?.hide()
                        onDismiss()
                    }
                ) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = PrimerShapes.medium
        )
    }
}

@Composable
fun ActionTextButton(
    label: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PrimerCard(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = PrimerSpacing.md,
                    vertical = PrimerSpacing.sm
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(PrimerSpacing.xs))

            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
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

