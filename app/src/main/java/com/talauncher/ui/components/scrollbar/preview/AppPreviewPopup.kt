package com.talauncher.ui.components.scrollbar.preview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.talauncher.data.model.AppIconStyleOption
import com.talauncher.ui.components.AppIcon
import com.talauncher.ui.theme.PrimerSpacing

/**
 * Configuration for app preview popup appearance.
 *
 * This data class encapsulates all visual configuration options,
 * following the **Single Responsibility Principle** by managing only appearance settings.
 *
 * @param iconSize Size of the app icon in the preview
 * @param iconStyle Style of the app icon (original, monochrome, hidden)
 * @param cornerRadius Corner radius for the preview card
 * @param elevation Elevation for shadow effect
 * @param padding Internal padding of the preview
 * @param minWidth Minimum width of the preview popup
 * @param maxWidth Maximum width of the preview popup
 * @param showPosition Whether to show position text (e.g., "25 / 100")
 */
data class AppPreviewConfig(
    val iconSize: androidx.compose.ui.unit.Dp = 48.dp,
    val iconStyle: AppIconStyleOption = AppIconStyleOption.ORIGINAL,
    val cornerRadius: androidx.compose.ui.unit.Dp = 16.dp,
    val elevation: androidx.compose.ui.unit.Dp = 8.dp,
    val padding: androidx.compose.ui.unit.Dp = 16.dp,
    val minWidth: androidx.compose.ui.unit.Dp = 200.dp,
    val maxWidth: androidx.compose.ui.unit.Dp = 280.dp,
    val showPosition: Boolean = true
) {
    init {
        require(iconSize > 0.dp) { "Icon size must be positive" }
        require(cornerRadius >= 0.dp) { "Corner radius must be non-negative" }
        require(elevation >= 0.dp) { "Elevation must be non-negative" }
        require(padding >= 0.dp) { "Padding must be non-negative" }
        require(minWidth > 0.dp) { "Min width must be positive" }
        require(maxWidth >= minWidth) { "Max width must be greater than or equal to min width" }
    }
}

/**
 * App preview provider implementation.
 *
 * This class implements the **IPreviewProvider** interface to provide app-specific
 * preview functionality during scrollbar interaction.
 *
 * Architecture highlights:
 * - **Strategy Pattern**: Different preview strategies can be swapped
 * - **Single Responsibility**: Handles only app preview rendering
 * - **Dependency Injection**: Receives dependencies through constructor
 *
 * @param contentProvider Provider for preview content (app info, position, etc.)
 * @param positionCalculator Calculator for preview position
 * @param config Visual configuration for the preview
 */
class AppPreviewProvider(
    private val contentProvider: IPreviewContentProvider,
    private val positionCalculator: PreviewPositionCalculator = PreviewPositionCalculator(),
    private val config: AppPreviewConfig = AppPreviewConfig()
) : IPreviewProvider {

    @Composable
    override fun ProvidePreview(
        normalizedPosition: Float,
        scrollbarBounds: ScrollbarBounds,
        enableGlassmorphism: Boolean
    ) {
        // Get content for current position
        val content = contentProvider.getContentAt(normalizedPosition) ?: return

        // Render the preview popup
        AppPreviewPopup(
            content = content,
            normalizedPosition = normalizedPosition,
            scrollbarBounds = scrollbarBounds,
            enableGlassmorphism = enableGlassmorphism,
            config = config,
            positionCalculator = positionCalculator
        )
    }

    override fun calculatePreviewPosition(
        normalizedPosition: Float,
        scrollbarBounds: ScrollbarBounds,
        previewHeight: Float,
        previewWidth: Float
    ): IntOffset {
        // Note: Density should be passed from the composable context
        // This is a simplified version - actual usage should pass density from LocalDensity
        return positionCalculator.calculatePosition(
            normalizedPosition = normalizedPosition,
            scrollbarBounds = scrollbarBounds,
            previewHeight = previewHeight,
            previewWidth = previewWidth,
            density = 1f // Default density; should be overridden with actual density
        )
    }
}

/**
 * App preview popup composable.
 *
 * Displays a Material3-styled popup showing app icon, name, and position
 * during scrollbar interaction.
 *
 * Design principles:
 * - **Material3 Design**: Follows Material Design 3 guidelines
 * - **Accessibility**: Proper contrast and readable text
 * - **Responsive**: Adapts to glassmorphism and theme settings
 *
 * @param content Preview content to display
 * @param normalizedPosition Current scroll position (0f to 1f)
 * @param scrollbarBounds Bounds of the scrollbar
 * @param enableGlassmorphism Whether to apply glassmorphism effects
 * @param config Visual configuration
 * @param positionCalculator Calculator for position
 */
@Composable
private fun AppPreviewPopup(
    content: PreviewContent,
    normalizedPosition: Float,
    scrollbarBounds: ScrollbarBounds,
    enableGlassmorphism: Boolean,
    config: AppPreviewConfig,
    positionCalculator: PreviewPositionCalculator
) {
    val density = LocalDensity.current

    // Calculate preview dimensions
    val previewWidth = with(density) { config.maxWidth.toPx() }
    val previewHeight = with(density) {
        // Approximate height based on content
        (config.iconSize + config.padding * 2 + 40.dp).toPx()
    }

    // Calculate position with actual screen density
    val position = positionCalculator.calculatePosition(
        normalizedPosition = normalizedPosition,
        scrollbarBounds = scrollbarBounds,
        previewHeight = previewHeight,
        previewWidth = previewWidth,
        density = density.density
    )

    // Render popup at calculated position
    Box(
        modifier = Modifier.offset { position }
    ) {
        PreviewCard(
            content = content,
            enableGlassmorphism = enableGlassmorphism,
            config = config
        )
    }
}

/**
 * Preview card composable with Material3 styling.
 *
 * This composable creates the visual representation of the preview,
 * following Material Design 3 guidelines for elevation, color, and typography.
 *
 * @param content Preview content to display
 * @param enableGlassmorphism Whether to apply glassmorphism effects
 * @param config Visual configuration
 */
@Composable
private fun PreviewCard(
    content: PreviewContent,
    enableGlassmorphism: Boolean,
    config: AppPreviewConfig
) {
    val shape = RoundedCornerShape(config.cornerRadius)

    // Background styling based on glassmorphism setting
    val containerColor = if (enableGlassmorphism) {
        Color.Transparent
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    val borderColor = if (enableGlassmorphism) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    val backgroundBrush = if (enableGlassmorphism) {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
            )
        )
    } else {
        null
    }

    // Main preview card
    Surface(
        modifier = Modifier
            .shadow(
                elevation = if (enableGlassmorphism) 4.dp else config.elevation,
                shape = shape
            )
            .then(
                if (backgroundBrush != null) {
                    Modifier.background(backgroundBrush, shape)
                } else {
                    Modifier
                }
            )
            .border(
                border = BorderStroke(1.dp, borderColor),
                shape = shape
            ),
        shape = shape,
        color = containerColor,
        tonalElevation = if (enableGlassmorphism) 0.dp else 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(config.padding)
                .then(
                    Modifier
                        // Ensure minimum width
                        .size(width = config.minWidth, height = androidx.compose.ui.unit.Dp.Unspecified)
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // App icon
            if (config.iconStyle != AppIconStyleOption.HIDDEN) {
                AppIcon(
                    packageName = content.appInfo.packageName,
                    appName = content.appInfo.appName,
                    iconStyle = config.iconStyle,
                    iconSize = config.iconSize
                )

                Spacer(modifier = Modifier.width(PrimerSpacing.md))
            }

            // App info column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // App name
                Text(
                    text = content.appInfo.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Position text (e.g., "25 / 100")
                if (config.showPosition) {
                    Spacer(modifier = Modifier.size(4.dp))

                    Text(
                        text = content.previewText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Factory function to create an AppPreviewProvider with default configuration.
 *
 * This follows the **Factory Pattern** for simplified object creation.
 *
 * @param appList List of apps to provide preview for
 * @param config Optional visual configuration
 * @param positionConfig Optional position configuration
 * @return Configured AppPreviewProvider instance
 */
fun createAppPreviewProvider(
    appList: List<com.talauncher.data.model.AppInfo>,
    config: AppPreviewConfig = AppPreviewConfig(),
    positionConfig: PreviewPositionConfig = PreviewPositionConfig()
): AppPreviewProvider {
    val contentProvider = appList.toPreviewContentProvider()
    val positionCalculator = PreviewPositionCalculator(positionConfig)

    return AppPreviewProvider(
        contentProvider = contentProvider,
        positionCalculator = positionCalculator,
        config = config
    )
}

/**
 * Factory function to create an empty preview provider.
 *
 * Returns a provider that renders nothing, following the **Null Object Pattern**.
 *
 * @return Empty preview provider
 */
fun createEmptyPreviewProvider(): IPreviewProvider = EmptyPreviewProvider
