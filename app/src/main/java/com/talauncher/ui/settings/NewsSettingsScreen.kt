package com.talauncher.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.talauncher.data.model.NewsCategory
import com.talauncher.data.model.NewsRefreshInterval
import com.talauncher.ui.components.CheckboxItem
import com.talauncher.ui.components.SettingsLazyColumn
import com.talauncher.ui.components.SettingsSectionCard

@Composable
fun NewsSettingsScreen(
    selectedCategories: Set<NewsCategory>,
    onToggleCategory: (NewsCategory) -> Unit,
    refreshInterval: NewsRefreshInterval,
    onUpdateRefreshInterval: (NewsRefreshInterval) -> Unit
) {
    SettingsLazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            SettingsSectionCard(title = "Refresh Frequency") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = refreshInterval == NewsRefreshInterval.DAILY,
                        onClick = { onUpdateRefreshInterval(NewsRefreshInterval.DAILY) },
                        label = { Text("Daily") }
                    )
                    FilterChip(
                        selected = refreshInterval == NewsRefreshInterval.HOURLY,
                        onClick = { onUpdateRefreshInterval(NewsRefreshInterval.HOURLY) },
                        label = { Text("Hourly") }
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }
        item {
            SettingsSectionCard(title = "Categories") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    NewsCategory.entries.forEach { category ->
                        CheckboxItem(
                            label = category.label,
                            checked = selectedCategories.contains(category),
                            onCheckedChange = { onToggleCategory(category) }
                        )
                    }
                    if (selectedCategories.isEmpty()) {
                        Text(
                            text = "Select at least one category to enable news.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

