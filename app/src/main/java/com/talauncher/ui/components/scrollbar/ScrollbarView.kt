package com.talauncher.ui.components.scrollbar

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.talauncher.ui.theme.PrimerSpacing
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Main scrollbar component with Material Design 3 styling.
 *
 * This composable implements a fully-featured, customizable scrollbar that can overlay
 * any scrollable content. It follows Material Design guidelines and integrates seamlessly
 * with the app's theme system.
 *
 * Architecture:
 * - Uses Box layout for layering content and scrollbar
 * - Delegates state management to ScrollbarStateManager (Mediator pattern)
 * - Separates visual rendering from interaction logic (Single Responsibility)
 * - Provides extensive customization through config (Open/Closed principle)
 *
 * Features:
 * - Smooth animations for all state transitions
 * - Auto-hide functionality with configurable delay
 * - Haptic feedback on interaction (optional)
 * - Scroll preview overlay (optional)
 * - Drag-to-scroll and tap-to-jump functionality
 * - Full Material Theme integration
 * - Accessibility support with minimum touch targets
 *
 * @param scrollOffset Current scroll offset in pixels
 * @param maxScrollOffset Maximum scroll offset in pixels
 * @param viewportHeight Height of the visible viewport in pixels
 * @param contentHeight Total height of the scrollable content in pixels
 * @param onScrollToPosition Callback when scroll position should change (receives target offset)
 * @param modifier Modifier for the container
 * @param config Scrollbar configuration for appearance and behavior
 * @param previewProvider Provider for scroll preview items (optional)
 * @param enabled Whether the scrollbar is interactive
 * @param testTag Test tag for UI testing
 * @param content The scrollable content to overlay with the scrollbar
 */
@Composable
fun ScrollbarView(
    scrollOffset: Float,
    maxScrollOffset: Float,
    viewportHeight: Float,
    contentHeight: Float,
    onScrollToPosition: (Float) -> Unit,
    modifier: Modifier = Modifier,
    config: ScrollbarConfig = ScrollbarDefaults.config(),
    previewProvider: ScrollPreviewProvider = EmptyScrollPreviewProvider,
    enabled: Boolean = true,
    testTag: String = "scrollbar",
    content: @Composable BoxScope.() -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val view = LocalView.current

    // State manager coordinates all scrollbar state
    val stateManager = rememberScrollbarStateManager(
        config = config,
        coroutineScope = coroutineScope,
        onScrollToPosition = onScrollToPosition
    )

    // Track the height of the scrollbar track
    var trackHeightPx by remember { mutableStateOf(0f) }

    // Update scroll position in state manager
    LaunchedEffect(scrollOffset, maxScrollOffset, viewportHeight, contentHeight) {
        stateManager.updateScrollPosition(
            offset = scrollOffset,
            maxOffset = maxScrollOffset,
            viewportHeight = viewportHeight,
            contentHeight = contentHeight
        )
    }

    // Calculate visual state
    val visualState by rememberScrollbarVisualState(
        stateManager = stateManager,
        trackHeight = trackHeightPx
    )

    // Determine if preview should be shown
    val showPreview by remember {
        derivedStateOf {
            config.enableScrollPreview &&
                    stateManager.interactionState is ScrollbarInteractionState.Dragging
        }
    }

    // Get current preview item
    val currentPreview by remember {
        derivedStateOf {
            if (showPreview) {
                previewProvider.getPreviewAt(stateManager.scrollPosition.normalizedPosition)
            } else {
                null
            }
        }
    }

    Box(modifier = modifier) {
        // Main content
        content()

        // Scrollbar overlay
        if (enabled && stateManager.scrollPosition.isScrollable) {
            ScrollbarOverlay(
                visualState = visualState,
                config = config,
                stateManager = stateManager,
                trackHeightPx = trackHeightPx,
                onTrackHeightChanged = { trackHeightPx = it },
                view = view,
                testTag = testTag
            )

            // Preview overlay
            if (currentPreview != null) {
                ScrollPreviewOverlay(
                    preview = currentPreview!!,
                    config = config,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }
    }
}

/**
 * Scrollbar overlay component.
 *
 * This composable renders the scrollbar track and thumb,
 * handling all pointer input and visual animations.
 *
 * @param visualState Current visual state for rendering
 * @param config Scrollbar configuration
 * @param stateManager State manager for interaction handling
 * @param trackHeightPx Current track height in pixels
 * @param onTrackHeightChanged Callback when track height changes
 * @param view Current Android View for haptic feedback
 * @param testTag Test tag for UI testing
 */
@Composable
private fun BoxScope.ScrollbarOverlay(
    visualState: ScrollbarVisualState,
    config: ScrollbarConfig,
    stateManager: ScrollbarStateManager,
    trackHeightPx: Float,
    onTrackHeightChanged: (Float) -> Unit,
    view: android.view.View,
    testTag: String
) {
    val density = LocalDensity.current

    // Animate thumb offset for smooth scrolling
    val animatedThumbOffset by animateFloatAsState(
        targetValue = visualState.thumbOffsetY,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 100,
            easing = androidx.compose.animation.core.LinearEasing
        ),
        label = "thumbOffset"
    )

    // Animate thumb height for smooth size changes
    val animatedThumbHeight by animateFloatAsState(
        targetValue = visualState.thumbHeight,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 150,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "thumbHeight"
    )

    // Animate color transitions
    val animatedColor by animateFloatAsState(
        targetValue = if (stateManager.interactionState is ScrollbarInteractionState.Dragging) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 200,
            easing = androidx.compose.animation.core.LinearEasing
        ),
        label = "colorTransition"
    )

    val currentThumbColor = lerp(
        config.thumbColor,
        config.thumbColorDragging,
        animatedColor
    )

    AnimatedVisibility(
        visible = visualState.isVisible && visualState.alpha > 0f,
        enter = fadeIn(
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = ScrollbarDefaults.Durations.FadeInMillis
            )
        ),
        exit = fadeOut(
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = ScrollbarDefaults.Durations.FadeOutMillis
            )
        ),
        modifier = Modifier.align(Alignment.CenterEnd)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(config.padding)
                .onSizeChanged { size ->
                    onTrackHeightChanged(size.height.toFloat())
                }
        ) {
            // Track (optional)
            if (config.trackColor != null) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(config.trackWidth)
                        .clip(RoundedCornerShape(config.trackCornerRadius))
                        .background(config.trackColor)
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                if (config.enableHapticFeedback) {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                }
                                stateManager.handleTrackTap(offset.y, trackHeightPx)
                            }
                        }
                )
            }

            // Thumb
            val thumbHeightDp = with(density) { animatedThumbHeight.toDp() }

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = 0,
                            y = animatedThumbOffset.roundToInt()
                        )
                    }
                    .width(config.thumbWidth)
                    .padding(
                        start = (config.trackWidth - config.thumbWidth) / 2
                    )
                    .clip(RoundedCornerShape(config.thumbCornerRadius))
                    .background(currentThumbColor)
                    .graphicsLayer {
                        alpha = visualState.alpha
                    }
                    .pointerInput(Unit) {
                        detectScrollbarDrag(
                            onDragStart = { offset ->
                                if (config.enableHapticFeedback) {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                }
                                stateManager.handleTouchDown(offset.y, animatedThumbOffset)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                stateManager.handleTouchMove(change.position.y, trackHeightPx)
                            },
                            onDragEnd = {
                                if (config.enableHapticFeedback) {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                }
                                stateManager.handleTouchUp()
                            }
                        )
                    }
                    .testTag("${testTag}_thumb")
            ) {
                // Spacer to set the thumb height
                androidx.compose.foundation.layout.Spacer(
                    modifier = Modifier
                        .width(config.thumbWidth)
                        .height(thumbHeightDp)
                )
            }
        }
    }
}

/**
 * Scroll preview overlay component.
 *
 * This composable displays a preview of the content at the current scroll position,
 * appearing when the user drags the scrollbar thumb.
 *
 * @param preview Preview item to display
 * @param config Scrollbar configuration
 * @param modifier Modifier for the preview container
 */
@Composable
private fun ScrollPreviewOverlay(
    preview: ScrollPreviewItem,
    config: ScrollbarConfig,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(end = config.trackWidth + config.padding * 2 + PrimerSpacing.md)
            .testTag("scrollbar_preview"),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 4.dp,
        tonalElevation = 2.dp
    ) {
        Text(
            text = preview.text,
            modifier = Modifier.padding(
                horizontal = PrimerSpacing.md,
                vertical = PrimerSpacing.sm
            ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Extension function to detect scrollbar drag gestures.
 *
 * This follows the Extension Function pattern to add drag detection
 * while keeping the main composable clean and focused.
 *
 * @param onDragStart Callback when drag starts
 * @param onDrag Callback during drag
 * @param onDragEnd Callback when drag ends
 */
private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectScrollbarDrag(
    onDragStart: (Offset) -> Unit,
    onDrag: (androidx.compose.ui.input.pointer.PointerInputChange, Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    detectDragGestures(
        onDragStart = onDragStart,
        onDrag = onDrag,
        onDragEnd = onDragEnd,
        onDragCancel = onDragEnd
    )
}

/**
 * Linear interpolation between two colors.
 *
 * This utility function follows the Single Responsibility Principle,
 * handling only color interpolation logic.
 *
 * @param start Starting color
 * @param end Ending color
 * @param fraction Interpolation fraction (0f to 1f)
 * @return Interpolated color
 */
private fun lerp(start: Color, end: Color, fraction: Float): Color {
    val clampedFraction = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * clampedFraction,
        green = start.green + (end.green - start.green) * clampedFraction,
        blue = start.blue + (end.blue - start.blue) * clampedFraction,
        alpha = start.alpha + (end.alpha - start.alpha) * clampedFraction
    )
}

/**
 * Simplified scrollbar for cases where you just need basic overlay.
 *
 * This composable provides a simpler API for common use cases,
 * following the Facade Pattern to hide complexity.
 *
 * @param scrollState LazyListState or similar scroll state
 * @param modifier Modifier for the container
 * @param config Scrollbar configuration
 * @param content Scrollable content
 */
@Composable
fun SimpleScrollbar(
    scrollState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier,
    config: ScrollbarConfig = ScrollbarDefaults.config(),
    content: @Composable BoxScope.() -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val layoutInfo = scrollState.layoutInfo

    val scrollOffset = remember(scrollState.firstVisibleItemIndex, scrollState.firstVisibleItemScrollOffset) {
        derivedStateOf {
            scrollState.firstVisibleItemIndex * layoutInfo.visibleItemsInfo.firstOrNull()?.size?.toFloat()
                .let { it ?: 0f } + scrollState.firstVisibleItemScrollOffset.toFloat()
        }
    }

    val contentHeight = remember {
        derivedStateOf {
            layoutInfo.totalItemsCount * layoutInfo.visibleItemsInfo.firstOrNull()?.size?.toFloat()
                .let { it ?: 0f }
        }
    }

    val viewportHeight = layoutInfo.viewportSize.height.toFloat()
    val maxOffset = (contentHeight.value - viewportHeight).coerceAtLeast(0f)

    ScrollbarView(
        scrollOffset = scrollOffset.value,
        maxScrollOffset = maxOffset,
        viewportHeight = viewportHeight,
        contentHeight = contentHeight.value,
        onScrollToPosition = { targetOffset ->
            // Calculate target item and offset
            val itemSize = layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0
            if (itemSize > 0) {
                val targetIndex = (targetOffset / itemSize).toInt()
                val targetItemOffset = (targetOffset % itemSize).toInt()
                coroutineScope.launch {
                    scrollState.scrollToItem(targetIndex, targetItemOffset)
                }
            }
        },
        modifier = modifier,
        config = config,
        content = content
    )
}
