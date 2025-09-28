package com.talauncher.ui.components.lib.foundation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Default values and styling constants for TALauncher UI Components Library
 * Following Material Design 3 principles with custom adaptations
 */
object ComponentDefaults {

    object Shapes {
        val small = RoundedCornerShape(6.dp)    // Small components, chips
        val medium = RoundedCornerShape(8.dp)   // Cards, buttons
        val large = RoundedCornerShape(12.dp)   // Large cards, modals
        val extraLarge = RoundedCornerShape(16.dp) // Search fields, containers
    }

    object Spacing {
        val xs = 4.dp
        val sm = 8.dp
        val md = 12.dp
        val lg = 16.dp
        val xl = 20.dp
        val xxl = 24.dp
    }

    object Elevation {
        val none = 0.dp
        val small = 2.dp
        val medium = 4.dp
        val large = 8.dp
    }

    object BorderWidth {
        val thin = 1.dp
        val medium = 2.dp
        val thick = 4.dp
    }

    @Composable
    fun borderStroke(
        width: Dp = BorderWidth.thin,
        color: Color = MaterialTheme.colorScheme.outlineVariant
    ): BorderStroke = BorderStroke(width, color)
}

/**
 * Density settings for component sizing
 */
enum class ComponentDensity {
    Compact,
    Comfortable,
    Spacious
}

/**
 * Component variant types for consistent styling
 */
enum class ComponentVariant {
    Primary,
    Secondary,
    Tertiary,
    Ghost
}