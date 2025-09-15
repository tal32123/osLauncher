package com.talauncher.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.talauncher.data.model.AppInfo
import com.talauncher.ui.theme.*
import kotlin.math.abs

@Composable
fun NewHomeScreen(
    viewModel: HomeViewModel,
    onNavigateToAppDrawer: (() -> Unit)? = null,
    onNavigateToSettings: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.refreshTime()
            kotlinx.coroutines.delay(60000)
        }
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
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    // Detect swipe down gesture - user swipes from top to bottom
                    if (dragAmount.y > 50 && change.position.y < 200) {
                        // Open notification shade
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
                    // Detect swipe right gesture - user swipes from left to right
                    else if (dragAmount.x > 100 && abs(dragAmount.y) < 50) {
                        // Navigate to app drawer
                        onNavigateToAppDrawer?.invoke()
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        // Navigate to settings on long press
                        onNavigateToSettings?.invoke()
                    }
                )
            }
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

            // Focus Mode Card - GitHub Primer style
            PrimerCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PrimerSpacing.sm),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isFocusModeEnabled) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                border = BorderStroke(
                    1.dp,
                    if (uiState.isFocusModeEnabled) PrimerBlue else MaterialTheme.colorScheme.outline
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PrimerSpacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Focus Mode",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (uiState.isFocusModeEnabled) "Distracting apps are hidden" else "All apps are visible",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.isFocusModeEnabled,
                        onCheckedChange = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.toggleFocusMode()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PrimerBlue,
                            checkedTrackColor = PrimerBlue.copy(alpha = 0.3f),
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(PrimerSpacing.xl))

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

            Spacer(modifier = Modifier.height(PrimerSpacing.xl))
        }

        // Swipe indicator - GitHub subtle style
        PrimerCard(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = PrimerSpacing.sm),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Text(
                text = "→",
                modifier = Modifier.padding(
                    horizontal = PrimerSpacing.sm,
                    vertical = PrimerSpacing.xs
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Friction barrier dialog for distracting apps
        if (uiState.showFrictionDialog && uiState.selectedAppForFriction != null) {
            FrictionDialog(
                appPackageName = uiState.selectedAppForFriction!!,
                onDismiss = { viewModel.dismissFrictionDialog() },
                onProceed = { reason ->
                    viewModel.launchAppWithReason(uiState.selectedAppForFriction!!, reason)
                }
            )
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

    // Get app name for display
    val appName = remember(appPackageName) {
        try {
            val packageManager = context.packageManager
            val appInfo = packageManager.getApplicationInfo(appPackageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            appPackageName
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Focus Mode Active",
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