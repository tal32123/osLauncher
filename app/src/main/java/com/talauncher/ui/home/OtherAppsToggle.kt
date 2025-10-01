package com.talauncher.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.talauncher.ui.components.ModernGlassCard
import com.talauncher.ui.theme.PrimerSpacing

/**
 * Toggle component for showing/hiding the hidden apps list.
 *
 * Architecture:
 * - Follows Component pattern for reusable UI elements
 * - Single Responsibility: Only handles the toggle UI
 * - Stateless component with callback for state management
 *
 * Design Pattern: Stateless Component with callback
 * SOLID: Single Responsibility, Open/Closed
 *
 * @param hiddenCount Number of hidden apps
 * @param isExpanded Whether the hidden apps list is currently expanded
 * @param enableGlassmorphism Whether to enable glassmorphism effect
 * @param onToggle Callback when toggle is clicked
 * @param modifier Modifier for styling
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OtherAppsToggle(
    hiddenCount: Int,
    isExpanded: Boolean,
    enableGlassmorphism: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val summaryText = if (hiddenCount == 1) {
        "1 app hidden from the main list."
    } else {
        "$hiddenCount apps hidden from the main list."
    }

    ModernGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("other_apps_toggle")
            .combinedClickable(onClick = onToggle),
        enableGlassmorphism = enableGlassmorphism
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PrimerSpacing.md)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Other Apps",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = if (isExpanded) "Hide" else "Show",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(PrimerSpacing.xs))

            Text(
                text = summaryText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!isExpanded) {
                Spacer(modifier = Modifier.height(PrimerSpacing.xs))
                Text(
                    text = "Tap to reveal these hidden apps.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
