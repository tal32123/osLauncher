package com.talauncher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.talauncher.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GoogleSearchItem(
    query: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PrimerCard(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = PrimerBlue.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, PrimerBlue.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PrimerSpacing.md)
                .heightIn(min = PrimerListItemDefaults.minHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🔍",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(end = PrimerSpacing.sm)
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Search Google",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = PrimerBlue
                )
                Text(
                    text = "\"$query\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}