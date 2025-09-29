package com.talauncher.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import com.talauncher.ui.appdrawer.AlphabetIndexEntry
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class NiagaraFastScrollState(
    val isVisible: Boolean = false,
    val isScrolling: Boolean = false,
    val selectedEntry: AlphabetIndexEntry? = null,
    val selectedFraction: Float = 0f,
    val lastInteractionTime: Long = 0L
)

@Composable
fun NiagaraFastScroll(
    entries: List<AlphabetIndexEntry>,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
    onEntryFocused: (AlphabetIndexEntry, Float) -> Unit,
    onScrollingChanged: (Boolean) -> Unit
) {
    var fastScrollState by remember { mutableStateOf(NiagaraFastScrollState()) }
    var componentSize by remember { mutableStateOf(IntSize.Zero) }
    val entryBounds = remember(entries) { mutableStateMapOf<String, ClosedFloatingPointRange<Float>>() }
    val hapticFeedback = LocalHapticFeedback.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    // Auto-hide logic
    LaunchedEffect(fastScrollState.lastInteractionTime) {
        if (fastScrollState.lastInteractionTime > 0 && !fastScrollState.isScrolling) {
            delay(2000) // Hide after 2 seconds of inactivity
            if (System.currentTimeMillis() - fastScrollState.lastInteractionTime >= 2000) {
                fastScrollState = fastScrollState.copy(isVisible = false)
            }
        }
    }

    // Show scroll bar when list is scrolled programmatically
    LaunchedEffect(entries) {
        if (entries.isNotEmpty()) {
            fastScrollState = fastScrollState.copy(
                isVisible = true,
                lastInteractionTime = System.currentTimeMillis()
            )
        }
    }

    fun resolveEntryFromPosition(positionY: Float): Pair<AlphabetIndexEntry, Float>? {
        if (entries.isEmpty() || componentSize.height == 0 || entryBounds.isEmpty()) {
            return null
        }

        val totalHeight = componentSize.height.toFloat()
        val clampedY = positionY.coerceIn(0f, totalHeight)

        // Find the entry that contains this Y position
        val candidate = entries.mapNotNull { entry ->
            entryBounds[entry.key]?.let { bounds ->
                if (clampedY in bounds) {
                    entry to bounds
                } else {
                    null
                }
            }
        }.minByOrNull { (_, bounds) ->
            val center = (bounds.start + bounds.endInclusive) / 2f
            abs(clampedY - center)
        }

        return candidate?.let { (entry, bounds) ->
            val center = (bounds.start + bounds.endInclusive) / 2f
            val fraction = if (totalHeight > 0f) {
                (center / totalHeight).coerceIn(0f, 1f)
            } else {
                0f
            }
            entry to fraction
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(56.dp)
            .onSizeChanged { componentSize = it }
            .pointerInput(entries, isEnabled) {
                if (!isEnabled) return@pointerInput

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)

                    // Show fast scroll and start scrolling
                    fastScrollState = fastScrollState.copy(
                        isVisible = true,
                        isScrolling = true,
                        lastInteractionTime = System.currentTimeMillis()
                    )
                    onScrollingChanged(true)

                    resolveEntryFromPosition(down.position.y)?.let { (entry, fraction) ->
                        if (entry.hasApps && entry != fastScrollState.selectedEntry) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        fastScrollState = fastScrollState.copy(
                            selectedEntry = entry,
                            selectedFraction = fraction
                        )
                        onEntryFocused(entry, fraction)
                    }

                    try {
                        drag(down.id) { change ->
                            resolveEntryFromPosition(change.position.y)?.let { (entry, fraction) ->
                                if (entry.hasApps && entry != fastScrollState.selectedEntry) {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                fastScrollState = fastScrollState.copy(
                                    selectedEntry = entry,
                                    selectedFraction = fraction,
                                    lastInteractionTime = System.currentTimeMillis()
                                )
                                onEntryFocused(entry, fraction)
                            }
                            change.consume()
                        }
                    } finally {
                        // Stop scrolling
                        fastScrollState = fastScrollState.copy(
                            isScrolling = false,
                            selectedEntry = null,
                            lastInteractionTime = System.currentTimeMillis()
                        )
                        onScrollingChanged(false)
                    }
                }
            }
    ) {
        // Semi-transparent background strip
        AnimatedVisibility(
            visible = fastScrollState.isVisible && isEnabled,
            enter = fadeIn(animationSpec = tween(150)) + slideInHorizontally(
                animationSpec = tween(150),
                initialOffsetX = { it / 2 }
            ),
            exit = fadeOut(animationSpec = tween(300)) + slideOutHorizontally(
                animationSpec = tween(300),
                targetOffsetX = { it / 2 }
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        MaterialTheme.colorScheme.surface.copy(
                            alpha = if (fastScrollState.isScrolling) 0.95f else 0.7f
                        )
                    )
                    .alpha(
                        animateFloatAsState(
                            targetValue = if (fastScrollState.isScrolling) 1f else 0.8f,
                            animationSpec = tween(150),
                            label = "scroll_bar_alpha"
                        ).value
                    )
            ) {
                // Letter index
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    entries.forEachIndexed { index, entry ->
                        val isSelected = fastScrollState.isScrolling &&
                                       entry.key == fastScrollState.selectedEntry?.key

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .testTag("niagara_fast_scroll_entry_${entry.key}")
                                .onGloballyPositioned { coordinates ->
                                    val position = coordinates.positionInParent()
                                    val start = position.y
                                    val end = start + coordinates.size.height
                                    entryBounds[entry.key] = start..end
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = entry.displayLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = when {
                                    !isEnabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    entry.hasApps -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                },
                                fontSize = if (isSelected) 11.sp else 10.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Floating preview bubble
        AnimatedVisibility(
            visible = fastScrollState.isScrolling &&
                     fastScrollState.selectedEntry != null &&
                     fastScrollState.selectedEntry?.hasApps == true,
            enter = fadeIn(animationSpec = tween(100)) + scaleIn(
                animationSpec = tween(100),
                initialScale = 0.8f
            ),
            exit = fadeOut(animationSpec = tween(150)) + scaleOut(
                animationSpec = tween(150),
                targetScale = 0.8f
            ),
            modifier = Modifier
                .fillMaxSize()
                .offset(x = (-80).dp) // Position to the left of the scroll bar
        ) {
            fastScrollState.selectedEntry?.let { entry ->
                if (entry.hasApps) {
                    val offsetY = with(density) {
                        val containerHeight = componentSize.height.toDp()
                        val bubbleHeight = 64.dp
                        val center = fastScrollState.selectedFraction * containerHeight
                        val top = (center - bubbleHeight / 2f).coerceIn(0.dp, max(containerHeight - bubbleHeight, 0.dp))
                        top
                    }

                    Box(
                        modifier = Modifier
                            .offset(y = offsetY)
                            .zIndex(10f),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        NiagaraPreviewBubble(
                            letter = entry.displayLabel,
                            appName = entry.previewAppName
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NiagaraPreviewBubble(
    letter: String,
    appName: String?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                clip = false
            ),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = null,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .widthIn(min = 80.dp, max = 200.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Large letter display
            Text(
                text = letter,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            // App name preview
            if (!appName.isNullOrBlank()) {
                Text(
                    text = appName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }
    }
}