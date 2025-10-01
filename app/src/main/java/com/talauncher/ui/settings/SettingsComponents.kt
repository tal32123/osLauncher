package com.talauncher.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.talauncher.R
import com.talauncher.data.model.InstalledApp
import com.talauncher.ui.components.SettingsLazyColumn
import kotlin.math.roundToInt

@Composable
fun DefaultTimeLimitCard(
    sliderValue: Float,
    displayedMinutes: Int,
    onSliderValueChange: (Float) -> Unit,
    onSliderChangeFinished: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_default_time_limit),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = stringResource(R.string.settings_default_time_limit_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = formatMinutesLabel(displayedMinutes),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Slider(
                value = sliderValue,
                onValueChange = onSliderValueChange,
                valueRange = 5f..180f,
                onValueChangeFinished = onSliderChangeFinished
            )

            Text(
                text = stringResource(R.string.settings_default_time_limit_custom_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EditTimeLimitDialog(
    appName: String,
    defaultMinutes: Int,
    initialMinutes: Int,
    isUsingDefault: Boolean,
    onUseDefault: () -> Unit,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var minutesText by remember(initialMinutes) { mutableStateOf(initialMinutes.toString()) }
    var isError by remember { mutableStateOf(false) }
    var isCurrentlyUsingDefault by remember(initialMinutes, isUsingDefault) {
        mutableStateOf(isUsingDefault)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.settings_time_limit_dialog_title_app, appName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.settings_time_limit_dialog_message),
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = minutesText,
                    onValueChange = {
                        minutesText = it
                        if (isError) {
                            isError = false
                        }
                        val parsed = it.toIntOrNull()
                        isCurrentlyUsingDefault = parsed == defaultMinutes
                    },
                    label = { Text(stringResource(R.string.settings_time_limit_dialog_minutes_label)) },
                    singleLine = true,
                    isError = isError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.testTag("time_limit_input")
                )

                TextButton(onClick = onUseDefault) {
                    Text(stringResource(R.string.settings_time_limit_dialog_use_default, formatMinutesLabel(defaultMinutes)))
                }

                if (isError) {
                    Text(
                        text = stringResource(R.string.settings_time_limit_dialog_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (isCurrentlyUsingDefault) {
                    Text(
                        text = stringResource(R.string.settings_time_limit_dialog_using_default),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val minutes = minutesText.toIntOrNull()
                    if (minutes != null && minutes in 5..480) {
                        onConfirm(minutes)
                    } else {
                        isError = true
                    }
                },
                modifier = Modifier.testTag("time_limit_save_button")
            ) {
                Text(stringResource(R.string.settings_time_limit_dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_time_limit_dialog_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionTab(
    title: String,
    subtitle: String,
    apps: List<InstalledApp>,
    selectedApps: Set<String>,
    onToggleApp: (String) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isLoading: Boolean,
    headerContent: (@Composable ColumnScope.() -> Unit)? = null,
    timeLimitInfoProvider: ((InstalledApp) -> Pair<Int, Boolean>)? = null,
    onEditTimeLimit: ((InstalledApp) -> Unit)? = null
) {
    Column {
        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        headerContent?.let {
            it()
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.settings_search_placeholder)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.settings_search_placeholder)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.settings_search_clear)
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            SettingsLazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                extraBottomPadding = 16.dp
            ) {
                items(apps) { app ->
                    val info = timeLimitInfoProvider?.invoke(app)
                    val minutes = info?.first
                    val usesDefault = info?.second ?: true
                    AppSelectionItem(
                        app = app,
                        isSelected = selectedApps.contains(app.packageName),
                        onToggle = { onToggleApp(app.packageName) },
                        timeLimitMinutes = minutes,
                        usesDefaultTimeLimit = usesDefault,
                        onEditTimeLimit = onEditTimeLimit?.let { handler ->
                            { handler(app) }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionItem(
    app: InstalledApp,
    isSelected: Boolean,
    onToggle: () -> Unit,
    timeLimitMinutes: Int? = null,
    usesDefaultTimeLimit: Boolean = true,
    onEditTimeLimit: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    fontSize = 16.sp
                )
                if (isSelected && timeLimitMinutes != null) {
                    val limitLabel = if (usesDefaultTimeLimit) {
                        stringResource(R.string.settings_app_time_limit_default, timeLimitMinutes)
                    } else {
                        stringResource(R.string.settings_app_time_limit, timeLimitMinutes)
                    }
                    Text(
                        text = limitLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isSelected && timeLimitMinutes != null && onEditTimeLimit != null) {
                IconButton(
                    onClick = onEditTimeLimit,
                    modifier = Modifier.testTag("edit_time_limit_${app.packageName}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.settings_edit_time_limit_description)
                    )
                }
            }
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                modifier = Modifier.testTag("app_selection_checkbox_${app.packageName}")
            )
        }
    }
}

/**
 * Formats minutes into a human-readable label.
 */
fun formatMinutesLabel(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "$hours hr $minutes min"
        hours > 0 -> if (hours == 1) "1 hour" else "$hours hours"
        else -> "$minutes min"
    }
}
