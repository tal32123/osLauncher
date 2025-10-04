package com.talauncher.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.talauncher.ui.theme.PrimerSpacing
import com.talauncher.domain.model.AlphabetIndexEntry
import com.talauncher.domain.model.SectionIndex
import com.talauncher.infrastructure.fastscroll.A11yAnnouncerImpl
import com.talauncher.infrastructure.fastscroll.HapticsImpl
import com.talauncher.ui.fastscroll.FastScrollController
@Composable
fun AlphabetIndex(
    entries: List<AlphabetIndexEntry>,
    activeKey: String?,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
    onEntryFocused: (AlphabetIndexEntry, Float) -> Unit,
    onScrubbingChanged: (Boolean) -> Unit,
    // Sidebar styling controls
    activeScale: Float = 1.4f,
    popOutDp: Float = 16f,
    waveSpread: Float = 1.5f
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
            val activeIndex = remember(activeKey, entries) {
                entries.indexOfFirst { it.key == activeKey }.takeIf { it >= 0 }
            }

            entries.forEachIndexed { index, entry ->
                AlphabetIndexItem(
                    entry = entry,
                    isActive = isEnabled && entry.hasApps && entry.key == activeKey,
                    isEnabled = isEnabled,
                    index = index,
                    activeIndex = activeIndex,
                    activeScale = activeScale,
                    popOutDp = popOutDp,
                    waveSpread = waveSpread,
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
    index: Int,
    activeIndex: Int?,
    activeScale: Float,
    popOutDp: Float,
    waveSpread: Float,
    onPositioned: (ClosedFloatingPointRange<Float>) -> Unit
) {
    val baseColorEnabled = MaterialTheme.colorScheme.onSurfaceVariant
    val baseColorDisabled = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)

    // Wave influence based on distance from active index
    val distance = remember(activeIndex, index) {
        activeIndex?.let { kotlin.math.abs(index - it) } ?: Int.MAX_VALUE
    }
    val influence = remember(distance, waveSpread) {
        if (activeIndex == null || distance == Int.MAX_VALUE) 0f
        else if (waveSpread <= 0f) {
            if (distance == 0) 1f else 0f
        } else {
            val rawInfluence = kotlin.math.exp(-distance / waveSpread)
            // Apply minimum threshold to prevent too many items from being affected
            // This is especially important for high waveSpread values (3.0+)
            if (rawInfluence < 0.1f) 0f else rawInfluence
        }
    }

    val scale = 1f + (activeScale - 1f) * influence

    val density = LocalDensity.current
    val translationX = with(density) { (-popOutDp * influence).dp.toPx() }

    // Fade entries based on influence for a calm effect
    val alpha = if (!isEnabled) 0.3f else 0.4f + 0.6f * influence
    val color = if (!entry.hasApps) baseColorDisabled else baseColorEnabled.copy(alpha = alpha)

    Text(
        text = entry.displayLabel,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = translationX
            )
            .testTag("alphabet_index_entry_${entry.key}")
            .onGloballyPositioned { coordinates ->
                val position = coordinates.positionInParent()
                val start = position.y
                val end = start + coordinates.size.height
                onPositioned(start..end)
            }
    )
}

/**
 * Enhanced alphabet fast scroller with per-app targeting.
 *
 * Architecture:
 * - Uses FastScrollController for state management and coordination
 * - Integrates haptic feedback and accessibility announcements
 * - Provides per-app scrubbing with O(1) touch mapping
 *
 * Features:
 * - Per-app targeting (not just section headers)
 * - Haptic feedback on letter/app changes
 * - TalkBack accessibility support
 * - 60fps smooth scrolling with throttling
 * - No additional overlay or bubble indicators
 *
 * Performance:
 * - Zero allocations during drag
 * - Throttled scroll updates (max 1 per 16ms)
 * - Precomputed section index with O(1) mapping
 *
 * @param sectionIndex Precomputed section index with prefix sums
 * @param isEnabled Whether the fast scroller is enabled
 * @param modifier Modifier for the container
 * @param onScrollToIndex Callback to scroll the list to a specific index
 */
@Composable
fun EnhancedAlphabetFastScroller(
    sectionIndex: SectionIndex,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
    onScrollToIndex: (Int) -> Unit = {},
    onActiveGlobalIndexChanged: (Int?) -> Unit = {},
    // Sidebar styling controls
    activeScale: Float = 1.4f,
    popOutDp: Float = 16f,
    waveSpread: Float = 1.5f
) {
    val context = LocalContext.current
    val view = LocalView.current

    // Initialize controller with haptics and accessibility
    val controller = remember {
        FastScrollController(
            haptics = HapticsImpl(context),
            a11yAnnouncer = A11yAnnouncerImpl(view)
        )
    }

    // Update section index when it changes
    LaunchedEffect(sectionIndex) {
        controller.setSectionIndex(sectionIndex)
    }

    // Collect state
    val state by controller.state.collectAsState()

    var railSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .width(48.dp)
            .fillMaxHeight()
            .onSizeChanged { railSize = it }
            .pointerInput(sectionIndex, isEnabled) {
                if (!isEnabled || sectionIndex.isEmpty) return@pointerInput

                try {
                    awaitEachGesture {
                        try {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            controller.onTouchStart()

                            // Handle initial touch
                            try {
                                controller.onTouch(
                                    touchY = down.position.y,
                                    railHeight = railSize.height.toFloat(),
                                    railTopPadding = PrimerSpacing.md.toPx(),
                                    railBottomPadding = PrimerSpacing.md.toPx(),
                                    onScrollToIndex = onScrollToIndex
                                )
                                // Notify active target for grid highlight
                                onActiveGlobalIndexChanged(controller.state.value.currentTarget?.globalIndex)
                            } catch (e: Exception) {
                                android.util.Log.e("EnhancedFastScroller", "Error handling initial touch", e)
                            }

                            try {
                                drag(down.id) { change ->
                                    try {
                                        controller.onTouch(
                                            touchY = change.position.y,
                                            railHeight = railSize.height.toFloat(),
                                            railTopPadding = PrimerSpacing.md.toPx(),
                                            railBottomPadding = PrimerSpacing.md.toPx(),
                                            onScrollToIndex = onScrollToIndex
                                        )
                                        // Notify active target for grid highlight
                                        onActiveGlobalIndexChanged(controller.state.value.currentTarget?.globalIndex)
                                        change.consume()
                                    } catch (e: Exception) {
                                        android.util.Log.e("EnhancedFastScroller", "Error during drag", e)
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("EnhancedFastScroller", "Error in drag gesture", e)
                            } finally {
                                try {
                                    controller.onTouchEnd()
                                    onActiveGlobalIndexChanged(null)
                                } catch (e: Exception) {
                                    android.util.Log.e("EnhancedFastScroller", "Error in onTouchEnd", e)
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("EnhancedFastScroller", "Error in gesture handling", e)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("EnhancedFastScroller", "Critical error in pointerInput", e)
                }
            }
    ) {
        // Letter rail
        // Determine active letter index for wave effect
        val activeIndex = remember(state.currentLetter, sectionIndex) {
            sectionIndex.sections.indexOfFirst { it.key == state.currentLetter }.takeIf { it >= 0 }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = PrimerSpacing.md),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            sectionIndex.sections.forEachIndexed { idx, section ->
                EnhancedAlphabetItem(
                    section = section,
                    isActive = isEnabled && state.currentLetter == section.key,
                    isEnabled = isEnabled,
                    index = idx,
                    activeIndex = activeIndex,
                    activeScale = activeScale,
                    popOutDp = popOutDp,
                    waveSpread = waveSpread
                )
            }
        }

    }
}

/**
 * Individual letter item for the enhanced fast scroller.
 */
@Composable
private fun EnhancedAlphabetItem(
    section: com.talauncher.domain.model.Section,
    isActive: Boolean,
    isEnabled: Boolean,
    index: Int,
    activeIndex: Int?,
    activeScale: Float,
    popOutDp: Float,
    waveSpread: Float
) {
    // Wave influence based on distance from active index
    val distance = remember(activeIndex, index) {
        activeIndex?.let { kotlin.math.abs(index - it) } ?: Int.MAX_VALUE
    }
    val influenceBase = remember(distance, waveSpread) {
        if (activeIndex == null || distance == Int.MAX_VALUE) 0f
        else if (waveSpread <= 0f) {
            if (distance == 0) 1f else 0f
        } else {
            val rawInfluence = kotlin.math.exp(-distance / waveSpread)
            // Apply minimum threshold to prevent too many items from being affected
            // This is especially important for high waveSpread values (3.0+)
            if (rawInfluence < 0.1f) 0f else rawInfluence
        }
    }
    val influence = if (!isEnabled) 0f else influenceBase

    val scale by animateFloatAsState(
        targetValue = 1f + (activeScale - 1f) * influence,
        animationSpec = tween(120),
        label = "letter_scale"
    )

    val offsetX by animateFloatAsState(
        targetValue = -popOutDp * influence,
        animationSpec = tween(120),
        label = "letter_offset"
    )

    val alpha by animateFloatAsState(
        targetValue = when {
            !isEnabled -> 0.3f
            else -> 0.4f + 0.6f * influence
        }.coerceIn(0.3f, 1f),
        animationSpec = tween(100),
        label = "letter_alpha"
    )

    Text(
        text = section.displayLabel,
        style = if (isActive) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .offset(x = offsetX.dp)
            .scale(scale)
            .alpha(alpha)
            .testTag("enhanced_alphabet_item_${section.key}")
    )
}

