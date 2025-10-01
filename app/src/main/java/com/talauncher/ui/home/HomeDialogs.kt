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
import com.talauncher.ui.components.ActionButton
import com.talauncher.ui.theme.PrimerButton
import com.talauncher.ui.theme.PrimerSecondaryButton
import com.talauncher.ui.theme.PrimerShapes
import com.talauncher.ui.theme.PrimerSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
                text = stringResource(R.string.friction_dialog_title),
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
                Text(stringResource(R.string.friction_dialog_continue))
            }
        },
        dismissButton = {
            PrimerSecondaryButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("friction_cancel_button")
            ) {
                Text(stringResource(R.string.friction_dialog_cancel))
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
            text = stringResource(R.string.friction_dialog_marked_distracting, appName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.friction_dialog_why),
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
            placeholder = { Text(stringResource(R.string.friction_dialog_reason_placeholder)) },
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
                Text(stringResource(R.string.app_action_dialog_cancel))
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
            text = stringResource(R.string.app_action_dialog_choose),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(PrimerSpacing.sm)
        ) {
            ActionButton(
                label = stringResource(R.string.rename_app_action_label),
                description = stringResource(R.string.rename_app_action_description),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onRename(app)
                    onDismiss()
                }
            )

            if (isHidden) {
                ActionButton(
                    label = stringResource(R.string.app_action_unhide_label),
                    description = stringResource(R.string.app_action_unhide_description),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onUnhide(app.packageName)
                        onDismiss()
                    }
                )
            } else {
                ActionButton(
                    label = stringResource(R.string.app_action_hide_label),
                    description = stringResource(R.string.app_action_hide_description),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hide_app_button"),
                    onClick = {
                        onHide(app.packageName)
                        onDismiss()
                    }
                )
            }

            ActionButton(
                label = stringResource(R.string.app_action_info_label),
                description = stringResource(R.string.app_action_info_description),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onAppInfo(app.packageName)
                    onDismiss()
                }
            )

            if (canUninstall) {
                ActionButton(
                    label = stringResource(R.string.app_action_uninstall_label),
                    description = stringResource(R.string.app_action_uninstall_description),
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

private suspend fun getAppDisplayName(context: Context, packageName: String): String {
    return try {
        val packageManager = context.packageManager
        val appInfo = packageManager.getApplicationInfo(packageName, 0)
        packageManager.getApplicationLabel(appInfo).toString()
    } catch (e: Exception) {
        packageName
    }
}
