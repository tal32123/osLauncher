package com.talauncher.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import com.talauncher.ui.home.MotivationalQuotesProvider

@Composable
fun TimeLimitDialog(
    appName: String,
    usageMinutes: Int?,
    timeLimitMinutes: Int,
    isUsingDefaultLimit: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
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
                    text = "Pause before opening $appName",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (motivationalQuote.isNotBlank()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
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

                val usageText = when {
                    usageMinutes == null -> "We couldn't detect today's usage. Enable usage access to see your progress."
                    usageMinutes == 0 -> "You haven't spent any time in $appName today."
                    else -> "You've already used ${formatMinutesLabel(usageMinutes)} in $appName today."
                }

                Text(
                    text = usageText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val limitLabel = buildString {
                    append("Today's limit: ${formatMinutesLabel(timeLimitMinutes)}")
                    if (isUsingDefaultLimit) {
                        append(" (default)")
                    }
                }

                Text(
                    text = limitLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Text(
                    text = "Are you sure you want to open it right now?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Not now")
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Yes, open")
                    }
                }
            }
        }
    }
}

private fun formatMinutesLabel(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "$hours hr ${minutes} min"
        hours > 0 -> if (hours == 1) "1 hour" else "$hours hours"
        else -> "$minutes min"
    }
}