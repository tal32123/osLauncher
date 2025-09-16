package com.talauncher.ui.appdrawer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.talauncher.R
import com.talauncher.data.model.AppInfo
import com.talauncher.ui.components.TimeLimitDialog
import com.talauncher.ui.components.MathChallengeDialog
import com.talauncher.ui.theme.*

@Composable
fun AppDrawerScreen(
    viewModel: AppDrawerViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val hapticFeedback = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var searchQuery by remember { mutableStateOf("") }
    var showHiddenApps by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val filteredApps = remember(uiState.allApps, searchQuery) {
        uiState.allApps.filter { app ->
            !app.isHidden &&
            app.appName.contains(searchQuery, ignoreCase = true)
        }
    }

    // Handle back button for dialogs
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
            // Search Bar - GitHub style
            PrimerCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PrimerSpacing.md),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(2.dp, PrimerBlue.copy(alpha = 0.3f))
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = {
                        Text(
                            text = "Search apps...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Text(
                            text = "Search",
                            style = MaterialTheme.typography.labelMedium,
                            color = PrimerGray600,
                            modifier = Modifier.padding(start = PrimerSpacing.xs)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    searchQuery = ""
                                    keyboardController?.hide()
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = PrimerGray600
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
                            keyboardController?.hide()
                        }
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimerBlue,
                        unfocusedBorderColor = PrimerGray300,
                        cursorColor = PrimerBlue
                    ),
                    shape = PrimerShapes.small
                )
            }

            // Apps List - GitHub style
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = PrimerSpacing.md),
                verticalArrangement = Arrangement.spacedBy(PrimerSpacing.xs)
            ) {
                // Recently Used Apps Section
                if (uiState.recentApps.isNotEmpty() && searchQuery.isEmpty()) {
                    item {
                        Text(
                            text = "Recently Used",
                            style = MaterialTheme.typography.titleSmall,
                            color = PrimerGray600,
                            modifier = Modifier.padding(
                                start = PrimerSpacing.xs,
                                top = PrimerSpacing.sm,
                                bottom = PrimerSpacing.xs
                            )
                        )
                    }

                    items(uiState.recentApps, key = { "recent_${it.packageName}" }) { app ->
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

                    item {
                        Spacer(modifier = Modifier.height(PrimerSpacing.md))
                        HorizontalDivider(
                            color = PrimerGray200,
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = PrimerSpacing.sm)
                        )
                        Text(
                            text = "All Apps",
                            style = MaterialTheme.typography.titleSmall,
                            color = PrimerGray600,
                            modifier = Modifier.padding(
                                start = PrimerSpacing.xs,
                                bottom = PrimerSpacing.xs
                            )
                        )
                    }
                }

                // Regular Apps
                items(filteredApps, key = { it.packageName }) { app ->
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

                // Hidden Apps Section
                if (uiState.hiddenApps.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(PrimerSpacing.xl))

                        PrimerCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = PrimerSpacing.sm),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            border = BorderStroke(1.dp, PrimerGray300)
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
                                    text = if (showHiddenApps)
                                        "Hide hidden apps (${uiState.hiddenApps.size})"
                                    else
                                        "Show hidden apps (${uiState.hiddenApps.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = PrimerBlue
                                )
                            }
                        }
                    }

                    if (showHiddenApps) {
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
                }

                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(PrimerSpacing.xl))
                }
            }
        }


        // App Action Dialog
        AppActionDialog(
            app = uiState.selectedAppForAction,
            onDismiss = viewModel::dismissAppActionDialog,
            onPin = viewModel::pinApp,
            onUnpin = viewModel::unpinApp,
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

        // Friction barrier dialog for distracting apps
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

        // Time limit dialog for distracting apps
        if (uiState.showTimeLimitDialog) {
            run {
                val selectedPackage = uiState.selectedAppForTimeLimit ?: return@run
                val appName = remember(selectedPackage) {
                    try {
                        val packageManager = context.packageManager
                        val appInfo = packageManager.getApplicationInfo(selectedPackage, 0)
                        packageManager.getApplicationLabel(appInfo).toString()
                    } catch (e: Exception) {
                        selectedPackage
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
                    isTimeExpired = false  // App drawer challenges are not for expired sessions
                )
            }
        }
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
        border = BorderStroke(1.dp, PrimerGray200)
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

            if (appInfo.isPinned) {
                Surface(
                    color = PrimerBlue.copy(alpha = 0.1f),
                    shape = PrimerShapes.small,
                    border = BorderStroke(1.dp, PrimerBlue.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "Pinned",
                        modifier = Modifier.padding(
                            horizontal = PrimerSpacing.xs,
                            vertical = 2.dp
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimerBlue
                    )
                }
            }
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
        border = BorderStroke(1.dp, PrimerGray300.copy(alpha = 0.5f))
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
                color = PrimerGray500.copy(alpha = 0.1f),
                shape = PrimerShapes.small,
                border = BorderStroke(1.dp, PrimerGray500.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "Hidden",
                    modifier = Modifier.padding(
                        horizontal = PrimerSpacing.xs,
                        vertical = 2.dp
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimerGray600
                )
            }
        }
    }
}

@Composable
fun AppActionDialog(
    app: AppInfo?,
    onDismiss: () -> Unit,
    onPin: (String) -> Unit,
    onUnpin: (String) -> Unit,
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
                            label = if (app.isPinned) "Unpin from essentials" else "Pin to essentials",
                            description = if (app.isPinned) {
                                "Remove this app from your quick access list."
                            } else {
                                "Add this app to your essentials list for quick access."
                            },
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (app.isPinned) {
                                    onUnpin(app.packageName)
                                } else {
                                    onPin(app.packageName)
                                }
                                onDismiss()
                            }
                        )

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
                            focusedBorderColor = PrimerBlue,
                            unfocusedBorderColor = PrimerGray300,
                            cursorColor = PrimerBlue
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
            containerColor = PrimerBlue.copy(alpha = 0.05f) // Slight blue tint for recent apps
        ),
        border = BorderStroke(1.dp, PrimerBlue.copy(alpha = 0.2f))
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
                color = PrimerBlue.copy(alpha = 0.1f),
                shape = PrimerShapes.small,
                border = BorderStroke(1.dp, PrimerBlue.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "Recent",
                    modifier = Modifier.padding(
                        horizontal = PrimerSpacing.xs,
                        vertical = 2.dp
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimerBlue
                )
            }
        }
    }
}