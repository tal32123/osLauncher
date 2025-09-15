package com.talauncher.ui.appdrawer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.talauncher.data.model.AppInfo
import com.talauncher.ui.theme.*

@Composable
fun NewAppDrawerScreen(
    viewModel: AppDrawerViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val hapticFeedback = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var searchQuery by remember { mutableStateOf("") }
    var showHiddenApps by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val filteredApps = remember(uiState.allApps, searchQuery, uiState.isFocusModeEnabled) {
        uiState.allApps.filter { app ->
            !app.isHidden &&
            (!uiState.isFocusModeEnabled || !app.isDistracting) &&
            app.appName.contains(searchQuery, ignoreCase = true)
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
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = PrimerGray600
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    searchQuery = ""
                                    keyboardController?.hide()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = PrimerGray600
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

        // Swipe indicator - GitHub style
        PrimerCard(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = PrimerSpacing.sm),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Text(
                text = "←",
                modifier = Modifier.padding(
                    horizontal = PrimerSpacing.sm,
                    vertical = PrimerSpacing.xs
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // App Action Dialog
        AppActionDialog(
            app = uiState.selectedAppForAction,
            onDismiss = viewModel::dismissAppActionDialog,
            onPin = viewModel::pinApp,
            onUnpin = viewModel::unpinApp,
            onHide = viewModel::hideApp,
            onAppInfo = { packageName ->
                viewModel.openAppInfo(context, packageName)
            },
            onUninstall = { packageName ->
                viewModel.uninstallApp(context, packageName)
            }
        )

        // Friction barrier dialog for distracting apps
        if (uiState.showFrictionDialog && uiState.selectedAppForFriction != null) {
            com.talauncher.ui.home.FrictionDialog(
                appPackageName = uiState.selectedAppForFriction!!,
                onDismiss = { viewModel.dismissFrictionDialog() },
                onProceed = { reason ->
                    viewModel.launchAppWithReason(uiState.selectedAppForFriction!!, reason)
                }
            )
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

                    // Icon-based action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Pin/Unpin button
                        IconActionButton(
                            icon = if (app.isPinned) Icons.Filled.Star else Icons.Filled.Add,
                            label = if (app.isPinned) "Unpin" else "Pin",
                            onClick = {
                                if (app.isPinned) {
                                    onUnpin(app.packageName)
                                } else {
                                    onPin(app.packageName)
                                }
                                onDismiss()
                            }
                        )

                        // Hide button (eye icon)
                        IconActionButton(
                            icon = Icons.Filled.Close,
                            label = "Hide",
                            onClick = {
                                onHide(app.packageName)
                                onDismiss()
                            }
                        )

                        // App Info button
                        IconActionButton(
                            icon = Icons.Filled.Info,
                            label = "Info",
                            onClick = {
                                onAppInfo(app.packageName)
                                onDismiss()
                            }
                        )

                        // Uninstall button
                        IconActionButton(
                            icon = Icons.Filled.Delete,
                            label = "Uninstall",
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IconActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(64.dp)
    ) {
        Card(
            modifier = Modifier
                .size(48.dp)
                .combinedClickable(onClick = onClick),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}