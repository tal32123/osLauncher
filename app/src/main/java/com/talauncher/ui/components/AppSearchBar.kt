package com.talauncher.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardActions
import androidx.compose.ui.text.input.KeyboardOptions
import com.talauncher.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholderText: String? = null,
    onClear: () -> Unit = { onValueChange("") },
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    focusRequester: FocusRequester? = null,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    shape: Shape = OutlinedTextFieldDefaults.shape,
    additionalTrailingContent: @Composable RowScope.() -> Unit = {}
) {
    val resolvedPlaceholder = placeholderText ?: stringResource(R.string.search_apps)
    val searchContentDescription = stringResource(R.string.search_icon_content_description)
    val clearContentDescription = stringResource(R.string.clear_search_content_description)

    val focusModifier = focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.then(focusModifier),
        placeholder = {
            Text(text = resolvedPlaceholder)
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = searchContentDescription
            )
        },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                additionalTrailingContent()
                if (value.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = clearContentDescription
                        )
                    }
                }
            }
        },
        singleLine = true,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        colors = colors,
        shape = shape
    )
}
