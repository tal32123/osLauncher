package com.talauncher.ui.components.lib.inputs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.talauncher.ui.components.lib.foundation.ComponentDefaults

/**
 * Generic, reusable text field component with glassmorphism support
 * Replaces ModernSearchField and provides consistent input styling
 */
@Composable
fun GenericTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String? = null,
    enableGlassmorphism: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isError: Boolean = false,
    supportingText: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    testTag: String? = null
) {
    val fieldColors = if (enableGlassmorphism) {
        OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedLeadingIconColor = MaterialTheme.colorScheme.onSurface,
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        OutlinedTextFieldDefaults.colors()
    }

    val decoratedModifier = if (enableGlassmorphism) {
        modifier
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.32f)
                    )
                ),
                shape = ComponentDefaults.Shapes.extraLarge
            )
            .border(
                width = ComponentDefaults.BorderWidth.thin,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                shape = ComponentDefaults.Shapes.extraLarge
            )
    } else {
        modifier
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = if (testTag != null) decoratedModifier.testTag(testTag) else decoratedModifier,
        placeholder = if (placeholder.isNotEmpty()) { { Text(placeholder) } } else null,
        label = if (label != null) { { Text(label) } } else null,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        supportingText = if (supportingText != null) { { Text(supportingText) } } else null,
        isError = isError,
        singleLine = singleLine,
        maxLines = maxLines,
        shape = ComponentDefaults.Shapes.extraLarge,
        colors = fieldColors,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions
    )
}

/**
 * Specialized search field
 */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search...",
    enableGlassmorphism: Boolean = false,
    onSearch: ((String) -> Unit)? = null,
    testTag: String? = null
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    GenericTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = placeholder,
        enableGlassmorphism = enableGlassmorphism,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                onSearch?.invoke(value.trim())
                keyboardController?.hide()
            }
        ),
        testTag = testTag
    )
}

/**
 * Numeric input field
 */
@Composable
fun NumericField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String? = null,
    min: Int? = null,
    max: Int? = null,
    testTag: String? = null
) {
    GenericTextField(
        value = value,
        onValueChange = { input ->
            // Filter to only allow numbers
            val filtered = input.filter { it.isDigit() }

            // Apply min/max constraints if provided
            val number = filtered.toIntOrNull()
            val validatedValue = when {
                number == null -> filtered
                min != null && number < min -> min.toString()
                max != null && number > max -> max.toString()
                else -> filtered
            }

            onValueChange(validatedValue)
        },
        modifier = modifier,
        placeholder = placeholder,
        label = label,
        keyboardOptions = KeyboardOptions(
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        testTag = testTag
    )
}