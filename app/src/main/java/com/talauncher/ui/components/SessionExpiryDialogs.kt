package com.talauncher.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import com.talauncher.ui.home.MotivationalQuotesProvider

@Composable
fun SessionExpiryCountdownDialog(
    appName: String,
    remainingSeconds: Int,
    totalSeconds: Int
) {
    val progress = remember(remainingSeconds, totalSeconds) {
        if (totalSeconds <= 0) 1f else 1f - (remainingSeconds.coerceAtLeast(0) / totalSeconds.toFloat())
    }
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val quotesProvider = remember(context) { MotivationalQuotesProvider(context) }
    val motivationalQuote = remember(appName, quotesProvider) {
        quotesProvider.getRandomQuote()
    }

    // Responsive sizing based on screen width
    val dialogPadding = if (configuration.screenWidthDp > 600) 32.dp else 16.dp
    val cardPadding = if (configuration.screenWidthDp > 600) 24.dp else 16.dp

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dialogPadding),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(cardPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Time's up",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                if (motivationalQuote.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = motivationalQuote,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(cardPadding)
                        )
                    }
                }

                Text(
                    text = "${appName} will close soon.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "${remainingSeconds.coerceAtLeast(0)}s remaining",
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun SessionExpiryActionDialog(
    appName: String,
    showMathChallengeOption: Boolean,
    onExtend: () -> Unit,
    onClose: () -> Unit,
    onMathChallenge: (() -> Unit)?
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Continue with $appName?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Choose how you'd like to proceed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!showMathChallengeOption) {
                    Button(
                        onClick = onExtend,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Set a new timer")
                    }
                }
                if (showMathChallengeOption && onMathChallenge != null) {
                    Button(
                        onClick = onMathChallenge,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Text("Solve a math challenge")
                    }
                }
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close app")
                }
            }
        }
    }
}
