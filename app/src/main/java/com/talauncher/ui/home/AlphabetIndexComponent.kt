package com.talauncher.ui.home

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.talauncher.ui.theme.PrimerSpacing
import com.talauncher.domain.model.AlphabetIndexEntry
import com.talauncher.domain.model.RECENT_APPS_INDEX_KEY
@Composable
fun AlphabetIndex(
    entries: List<AlphabetIndexEntry>,
    activeKey: String?,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
    onEntryFocused: (AlphabetIndexEntry, Float) -> Unit,
    onScrubbingChanged: (Boolean) -> Unit
) {
    var componentSize by remember { mutableStateOf(IntSize.Zero) }
    val entryBounds = remember(entries) {
        mutableStateMapOf<String, ClosedFloatingPointRange<Float>>()
    }

    fun resolveEntry(positionY: Float): Pair<AlphabetIndexEntry, Float>? {
        if (entries.isEmpty() || componentSize.height == 0 || entryBounds.isEmpty()) {
            return null
        }

        val totalHeight = componentSize.height.toFloat()
        val boundsList = entries.mapNotNull { entry ->
            entryBounds[entry.key]?.let { bounds -> entry to bounds }
        }

        if (boundsList.isEmpty()) {
            return null
        }

        val clampedY = positionY.coerceIn(0f, totalHeight)
        val candidate = boundsList.firstOrNull { (_, bounds) ->
            clampedY in bounds
        } ?: boundsList.minByOrNull { (_, bounds) ->
            when {
                clampedY < bounds.start -> bounds.start - clampedY
                clampedY > bounds.endInclusive -> clampedY - bounds.endInclusive
                else -> 0f
            }
        } ?: return null

        val (entry, bounds) = candidate
        val center = (bounds.start + bounds.endInclusive) / 2f
        val fraction = if (totalHeight > 0f) {
            (center / totalHeight).coerceIn(0f, 1f)
        } else {
            0f
        }

        return entry to fraction
    }

    Box(
        modifier = modifier
            .width(48.dp)
            .fillMaxHeight()
            .onSizeChanged { componentSize = it }
            .pointerInput(entries, isEnabled) {
                if (!isEnabled) return@pointerInput

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    onScrubbingChanged(true)

                    resolveEntry(down.position.y)?.let { (entry, fraction) ->
                        onEntryFocused(entry, fraction)
                    }

                    try {
                        drag(down.id) { change ->
                            resolveEntry(change.position.y)?.let { (entry, fraction) ->
                                onEntryFocused(entry, fraction)
                            }
                            change.consume()
                        }
                    } finally {
                        onScrubbingChanged(false)
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = PrimerSpacing.md),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            entries.forEach { entry ->
                AlphabetIndexItem(
                    entry = entry,
                    isActive = isEnabled && entry.hasApps && entry.key == activeKey,
                    isEnabled = isEnabled,
                    onPositioned = { bounds ->
                        entryBounds[entry.key] = bounds
                    }
                )
            }
        }
    }
}

@Composable
private fun AlphabetIndexItem(
    entry: AlphabetIndexEntry,
    isActive: Boolean,
    isEnabled: Boolean,
    onPositioned: (ClosedFloatingPointRange<Float>) -> Unit
) {
    val color = when {
        !isEnabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        entry.hasApps && isActive -> MaterialTheme.colorScheme.onSurface
        entry.hasApps -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
    }

    Text(
        text = entry.displayLabel,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .testTag("alphabet_index_entry_${entry.key}")
            .onGloballyPositioned { coordinates ->
                val position = coordinates.positionInParent()
                val start = position.y
                val end = start + coordinates.size.height
                onPositioned(start..end)
            }
    )
}
