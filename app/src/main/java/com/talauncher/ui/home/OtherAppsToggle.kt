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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.talauncher.R
import com.talauncher.ui.components.ModernGlassCard
import com.talauncher.ui.theme.PrimerSpacing
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
        stringResource(R.string.home_other_apps_summary_single)
    } else {
        stringResource(R.string.home_other_apps_summary_plural, hiddenCount)
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
                    text = stringResource(R.string.home_other_apps_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = if (isExpanded)
                        stringResource(R.string.home_other_apps_hide)
                    else
                        stringResource(R.string.home_other_apps_show),
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
                    text = stringResource(R.string.home_other_apps_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
