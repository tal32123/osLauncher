package com.talauncher.ui.home.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.talauncher.R
import com.talauncher.data.model.AppInfo
import com.talauncher.ui.components.ActionButton
import com.talauncher.ui.components.BaseActionDialog
import com.talauncher.ui.theme.PrimerSpacing

data class AppActionHandlers(
    val onRename: (AppInfo) -> Unit,
    val onHide: (String) -> Unit,
    val onUnhide: (String) -> Unit,
    val onMarkDistracting: (String) -> Unit,
    val onUnmarkDistracting: (String) -> Unit,
    val onAppInfo: (String) -> Unit,
    val onUninstall: (String) -> Unit
)

@Composable
fun AppActionDialog(
    app: AppInfo?,
    canUninstall: Boolean,
    onDismiss: () -> Unit,
    handlers: AppActionHandlers
) {
    if (app == null) return

    AppActionDialog(
        app = app,
        canUninstall = canUninstall,
        onDismiss = onDismiss,
        onRename = handlers.onRename,
        onHide = handlers.onHide,
        onUnhide = handlers.onUnhide,
        onMarkDistracting = handlers.onMarkDistracting,
        onUnmarkDistracting = handlers.onUnmarkDistracting,
        onAppInfo = handlers.onAppInfo,
        onUninstall = handlers.onUninstall
    )
}

@Composable
fun AppActionDialog(
    app: AppInfo?,
    canUninstall: Boolean,
    onDismiss: () -> Unit,
    onRename: (AppInfo) -> Unit,
    onHide: (String) -> Unit,
    onUnhide: (String) -> Unit,
    onMarkDistracting: (String) -> Unit,
    onUnmarkDistracting: (String) -> Unit,
    onAppInfo: (String) -> Unit,
    onUninstall: (String) -> Unit
) {
    if (app == null) return

    val isHidden = app.isHidden
    val isDistracting = app.isDistracting

    BaseActionDialog(
        title = app.appName,
        onDismissRequest = onDismiss,
        testTag = "app_action_dialog",
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.app_action_dialog_cancel))
            }
        }
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

            if (isDistracting) {
                ActionButton(
                    label = stringResource(R.string.app_action_unmark_distracting_label),
                    description = stringResource(R.string.app_action_unmark_distracting_description),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onUnmarkDistracting(app.packageName)
                        onDismiss()
                    }
                )
            } else {
                ActionButton(
                    label = stringResource(R.string.app_action_mark_distracting_label),
                    description = stringResource(R.string.app_action_mark_distracting_description),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onMarkDistracting(app.packageName)
                        onDismiss()
                    }
                )
            }

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
