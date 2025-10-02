package com.talauncher.ui.home.dialogs

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.talauncher.R
import com.talauncher.data.model.AppInfo
import com.talauncher.ui.components.BaseDialog
import com.talauncher.ui.theme.PrimerButton
import com.talauncher.ui.theme.PrimerSecondaryButton
import com.talauncher.ui.theme.PrimerShapes

interface RenameDialogState {
    val newName: String
    fun onNameChange(value: String)
}

data class DefaultRenameDialogState(
    override val newName: String,
    private val onValueChange: (String) -> Unit
) : RenameDialogState {
    override fun onNameChange(value: String) {
        onValueChange(value)
    }
}

@Composable
fun RenameAppDialog(
    app: AppInfo?,
    state: RenameDialogState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (app == null) return

    BaseDialog(
        title = stringResource(R.string.rename_app_dialog_title, app.appName),
        onDismissRequest = onDismiss,
        testTag = "rename_app_dialog",
        confirmButton = {
            PrimerButton(
                onClick = onConfirm,
                enabled = state.newName.trim().isNotEmpty(),
                modifier = Modifier.testTag("rename_confirm_button")
            ) {
                Text(stringResource(R.string.rename_app_dialog_confirm))
            }
        },
        dismissButton = {
            PrimerSecondaryButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("rename_cancel_button")
            ) {
                Text(stringResource(R.string.app_action_dialog_cancel))
            }
        }
    ) {
        Text(
            text = stringResource(R.string.rename_app_dialog_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = state.newName,
            onValueChange = state::onNameChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("rename_input_field"),
            label = { Text(stringResource(R.string.rename_app_dialog_field_label)) },
            placeholder = { Text(stringResource(R.string.rename_app_dialog_placeholder)) },
            supportingText = { Text(stringResource(R.string.rename_app_dialog_supporting_text)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { if (state.newName.trim().isNotEmpty()) onConfirm() }
            ),
            singleLine = true,
            shape = PrimerShapes.small,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                cursorColor = MaterialTheme.colorScheme.primary
            )
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
    RenameAppDialog(
        app = app,
        state = DefaultRenameDialogState(newName, onNameChange),
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}
