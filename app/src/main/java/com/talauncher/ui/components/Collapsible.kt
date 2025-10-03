package com.talauncher.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A generic collapsible component that can be expanded and collapsed.
 *
 * @param title The title text to display in the header
 * @param isExpanded Whether the content is currently expanded
 * @param onToggle Callback invoked when the header is clicked
 * @param modifier Modifier for the root component
 * @param content The content to show when expanded
 */
@Composable
fun Collapsible(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        content()
                    }
                }
            }
        }
    }
}

/**
 * Data class representing a collapsible section with its content.
 *
 * @param id Unique identifier for this section
 * @param title The title to display in the header
 * @param content The composable content to show when expanded
 */
data class CollapsibleSection(
    val id: String,
    val title: String,
    val content: @Composable () -> Unit
)

/**
 * A container component that manages a list of collapsible sections,
 * enforcing that only one section can be expanded at a time.
 *
 * @param sections List of collapsible sections to display
 * @param initialExpandedId The ID of the initially expanded section (default: first section)
 * @param modifier Modifier for the root component
 */
@Composable
fun CollapsibleSectionContainer(
    sections: List<CollapsibleSection>,
    initialExpandedId: String? = sections.firstOrNull()?.id,
    modifier: Modifier = Modifier
) {
    var expandedSectionId by remember { mutableStateOf(initialExpandedId) }

    Column(modifier = modifier) {
        sections.forEachIndexed { index, section ->
            if (index > 0) {
                Spacer(modifier = Modifier.height(12.dp))
            }

            Collapsible(
                title = section.title,
                isExpanded = expandedSectionId == section.id,
                onToggle = {
                    expandedSectionId = if (expandedSectionId == section.id) null else section.id
                }
            ) {
                section.content()
            }
        }
    }
}
