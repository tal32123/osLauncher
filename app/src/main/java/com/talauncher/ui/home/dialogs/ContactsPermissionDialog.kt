package com.talauncher.ui.home.dialogs

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.talauncher.R
import com.talauncher.ui.components.BaseDialog

@Composable
fun ContactsPermissionDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    BaseDialog(
        title = stringResource(R.string.contact_permission_required_title),
        onDismissRequest = onDismiss,
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
    ) {
        Text(
            text = stringResource(R.string.contact_permission_required_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
