package com.talauncher.ui.home

import androidx.compose.runtime.Composable
import com.talauncher.data.model.AppInfo

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
    onPin: (String) -> Unit,
    onUnpin: (String) -> Unit,
    onAppInfo: (String) -> Unit,
    onUninstall: (String) -> Unit
) = com.talauncher.ui.home.dialogs.AppActionDialog(
    app = app,
    canUninstall = canUninstall,
    onDismiss = onDismiss,
    onRename = onRename,
    onHide = onHide,
    onUnhide = onUnhide,
    onMarkDistracting = onMarkDistracting,
    onUnmarkDistracting = onUnmarkDistracting,
    onPin = onPin,
    onUnpin = onUnpin,
    onAppInfo = onAppInfo,
    onUninstall = onUninstall
)

@Composable
fun RenameAppDialog(
    app: AppInfo?,
    newName: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) = com.talauncher.ui.home.dialogs.RenameAppDialog(
    app = app,
    newName = newName,
    onNameChange = onNameChange,
    onConfirm = onConfirm,
    onDismiss = onDismiss
)

@Composable
fun ContactsPermissionDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) = com.talauncher.ui.home.dialogs.ContactsPermissionDialog(
    onDismiss = onDismiss,
    onConfirm = onConfirm
)
