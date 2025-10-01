package com.talauncher.ui.home

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import com.talauncher.R
import com.talauncher.data.model.AppInfo
import com.talauncher.domain.quotes.QuotesProvider
import com.talauncher.ui.theme.PrimerButton
import com.talauncher.ui.theme.PrimerSecondaryButton
import com.talauncher.ui.theme.PrimerShapes
import com.talauncher.ui.theme.PrimerSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Dialog components for the Home screen.
 *
 * Architecture:
 * - Follows Single Responsibility Principle - each dialog has one purpose
 * - Uses composition over inheritance for dialog structure
 * - Implements proper state management with remember and mutableStateOf
 * - Separates UI from business logic (QuotesProvider)
 */

/**
 * Dialog shown when user tries to open a "distracting" app.
 * Implements a friction barrier requiring the user to provide a reason.
 *
 * @param appPackageName The package name of the app being opened
 * @param onDismiss Callback when user dismisses the dialog
 * @param onProceed Callback when user proceeds with their reason
 * @param quotesProvider Optional provider for motivational quotes (follows DIP)
 */
@Composable
fun FrictionDialog(
    appPackageName: String,
    onDismiss: () -> Unit,
    onProceed: (String) -> Unit,
    quotesProvider: QuotesProvider? = null
) {
    var reason by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Use provided quotes provider or create default one
    val effectiveQuotesProvider = remember(context, quotesProvider) {
        quotesProvider ?: MotivationalQuotesProvider(context)
    }

    val motivationalQuote = remember(appPackageName, effectiveQuotesProvider) {
        effectiveQuotesProvider.getRandomQuote()
    }

    // Get app name for display
    var appName by remember(appPackageName) { mutableStateOf(appPackageName) }

    LaunchedEffect(appPackageName) {
        appName = withContext(Dispatchers.IO) {
            getAppDisplayName(context, appPackageName)
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
            FrictionDialogContent(
                appName = appName,
                reason = reason,
                onReasonChange = { reason = it },
                motivationalQuote = motivationalQuote
            )
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

@Composable
private fun FrictionDialogContent(
    appName: String,
    reason: String,
    onReasonChange: (String) -> Unit,
    motivationalQuote: String
) {
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
            onValueChange = onReasonChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("friction_reason_input"),
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
}

/**
 * Dialog for app-related actions (rename, hide, info, uninstall).
 * Implements Strategy pattern for different actions.
 *
 * @param app The app to perform actions on
 * @param canUninstall Whether the app can be uninstalled
 * @param onDismiss Callback to dismiss the dialog
 * @param onRename Callback to rename the app
 * @param onHide Callback to hide the app
 * @param onUnhide Callback to unhide the app
 * @param onAppInfo Callback to view app info
 * @param onUninstall Callback to uninstall the app
 */
@Composable
fun AppActionDialog(
    app: AppInfo?,
    canUninstall: Boolean,
    onDismiss: () -> Unit,
    onRename: (AppInfo) -> Unit,
    onHide: (String) -> Unit,
    onUnhide: (String) -> Unit,
    onAppInfo: (String) -> Unit,
    onUninstall: (String) -> Unit
) {
    if (app == null) return

    val isHidden = app.isHidden

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
            AppActionDialogContent(
                app = app,
                isHidden = isHidden,
                canUninstall = canUninstall,
                onRename = onRename,
                onHide = onHide,
                onUnhide = onUnhide,
                onAppInfo = onAppInfo,
                onUninstall = onUninstall,
                onDismiss = onDismiss
            )
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

@Composable
private fun AppActionDialogContent(
    app: AppInfo,
    isHidden: Boolean,
    canUninstall: Boolean,
    onRename: (AppInfo) -> Unit,
    onHide: (String) -> Unit,
    onUnhide: (String) -> Unit,
    onAppInfo: (String) -> Unit,
    onUninstall: (String) -> Unit,
    onDismiss: () -> Unit
) {
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

            if (isHidden) {
                ActionTextButton(
                    label = "Unhide app",
                    description = "Move this app back to the main list.",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onUnhide(app.packageName)
                        onDismiss()
                    }
                )
            } else {
                ActionTextButton(
                    label = "Hide app",
                    description = "Move this app to the hidden list.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hide_app_button"),
                    onClick = {
                        onHide(app.packageName)
                        onDismiss()
                    }
                )
            }

            ActionTextButton(
                label = "View app info",
                description = "Open the system settings page for this app.",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onAppInfo(app.packageName)
                    onDismiss()
                }
            )

            if (canUninstall) {
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
    }
}

/**
 * Reusable action button with label and description.
 * Follows Component pattern for consistent styling.
 */
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

/**
 * Dialog for contacts permission request.
 * Follows Interface Segregation Principle with specific callbacks.
 */
@Composable
fun ContactsPermissionDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.contact_permission_required_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.contact_permission_required_dismiss))
            }
        }
    )
}

/**
 * Helper function to get app display name from package manager.
 * Extracted for reusability and testability.
 */
private suspend fun getAppDisplayName(context: Context, packageName: String): String {
    return try {
        val packageManager = context.packageManager
        val appInfo = packageManager.getApplicationInfo(packageName, 0)
        packageManager.getApplicationLabel(appInfo).toString()
    } catch (e: Exception) {
        packageName
    }
}
