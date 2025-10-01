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

/**
 * Alphabet index component for quick navigation through app lists.
 *
 * Architecture:
 * - Follows Component pattern for reusable widgets
 * - Implements gesture detection with proper touch handling
 * - Uses immutable data classes for entries (AlphabetIndexEntry from domain layer)
 * - Provides clear callback interface (Interface Segregation Principle)
 *
 * Design Pattern: Observer pattern through callbacks
 * SOLID Principles:
 * - Single Responsibility: Only handles alphabet index UI and interaction
 * - Open/Closed: Can be extended with different entry types without modification
 * - Dependency Inversion: Depends on abstractions (callbacks, domain models) not concrete implementations
 */

/**
 * Alphabet index sidebar for quick list navigation.
 *
 * This component displays a vertical list of letters/symbols that users can
 * tap or drag through to quickly navigate to sections of an app list.
 *
 * @param entries List of alphabet entries to display
 * @param activeKey Currently active/focused entry key
 * @param isEnabled Whether the index is interactive
 * @param modifier Modifier for styling
 * @param onEntryFocused Callback when an entry is focused with position fraction
 * @param onScrubbingChanged Callback when user starts/stops scrubbing gesture
 */
@Composable
fun AlphabetIndex(
    entries: List<AlphabetIndexEntry>,
    activeKey: String?,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
    onEntryFocused: (AlphabetIndexEntry, Float) -> Unit,
    onScrubbingChanged: (Boolean) -> Unit
) {
    // Track component size for position calculations
    var componentSize by remember { mutableStateOf(IntSize.Zero) }

    // Track individual entry bounds for precise touch detection
    val entryBounds = remember(entries) {
        mutableStateMapOf<String, ClosedFloatingPointRange<Float>>()
    }

    /**
     * Resolves a Y-coordinate touch position to the corresponding alphabet entry.
     * Uses entry bounds for precise detection.
     *
     * @param positionY Y-coordinate of the touch
     * @return Pair of (entry, scroll fraction) or null if no entry found
     */
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

        // Clamp Y position to component bounds
        val clampedY = positionY.coerceIn(0f, totalHeight)

        // Find the entry whose bounds contain this Y position
        val candidate = boundsList.firstOrNull { (_, bounds) ->
            clampedY in bounds
        } ?: boundsList.minByOrNull { (_, bounds) ->
            // If no exact match, find closest entry
            when {
                clampedY < bounds.start -> bounds.start - clampedY
                clampedY > bounds.endInclusive -> clampedY - bounds.endInclusive
                else -> 0f
            }
        } ?: return null

        val (entry, bounds) = candidate

        // Calculate scroll fraction based on entry center
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

                // Gesture detection: tap and drag through the index
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    onScrubbingChanged(true)

                    // Handle initial tap
                    resolveEntry(down.position.y)?.let { (entry, fraction) ->
                        onEntryFocused(entry, fraction)
                    }

                    try {
                        // Handle drag/scrubbing
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

/**
 * Individual item in the alphabet index.
 * Separated for better composability and testing.
 *
 * @param entry The alphabet entry to display
 * @param isActive Whether this entry is currently active
 * @param isEnabled Whether the index is interactive
 * @param onPositioned Callback when item is positioned (provides bounds)
 */
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
