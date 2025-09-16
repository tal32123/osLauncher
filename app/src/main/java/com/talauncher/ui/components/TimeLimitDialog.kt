package com.talauncher.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import com.talauncher.ui.home.MotivationalQuotesProvider

@Composable
fun TimeLimitDialog(
    appName: String,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var durationText by remember { mutableStateOf("30") }
    var isError by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val quotesProvider = remember(context) { MotivationalQuotesProvider(context) }
    val motivationalQuote = remember(appName, quotesProvider) {
        quotesProvider.getRandomQuote()
    }

    // Responsive sizing based on screen width
    val dialogPadding = if (configuration.screenWidthDp > 600) 32.dp else 16.dp
    val cardPadding = if (configuration.screenWidthDp > 600) 24.dp else 16.dp

    // Prevent back button from dismissing the dialog
    BackHandler {
        // Do nothing - force user to make a choice
    }

    Dialog(
        onDismissRequest = {
            // Prevent dismissing by clicking outside
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dialogPadding),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(cardPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "How long will you use $appName?",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (motivationalQuote.isNotBlank()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = motivationalQuote,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(cardPadding)
                        )
                    }
                }

                Text(
                    text = "Set a time limit to help manage your usage",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = durationText,
                    onValueChange = {
                        durationText = it
                        isError = false
                    },
                    label = { Text("Minutes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = isError,
                    supportingText = if (isError) {
                        { Text("Please enter a valid number") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val duration = durationText.toIntOrNull()
                            if (duration != null && duration > 0) {
                                onConfirm(duration)
                            } else {
                                isError = true
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Start")
                    }
                }
            }
        }
    }
}