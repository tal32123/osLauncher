package com.talauncher.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Generic grid layout for chips with customizable columns and spacing.
 */
@Composable
fun <T> ChipGrid(
    items: List<T>,
    modifier: Modifier = Modifier,
    itemsPerRow: Int = 2,
    horizontalSpacing: Dp = 8.dp,
    verticalSpacing: Dp = 8.dp,
    itemContent: @Composable (T) -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(verticalSpacing)
    ) {
        items.chunked(itemsPerRow).forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing)
            ) {
                rowItems.forEach { item ->
                    itemContent(item)
                }
            }
        }
    }
}
