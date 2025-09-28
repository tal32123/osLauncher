package com.talauncher.ui.theme

import androidx.compose.ui.graphics.Color

// Modern 2025 Minimalist Color Palette
// Inspired by Material Design 3 and contemporary minimalist trends

// Core minimalist colors - sophisticated and refined
val MinimalPrimary = Color(0xFF1a1a1a)        // Deep charcoal
val MinimalSecondary = Color(0xFF6366f1)      // Modern indigo
val MinimalAccent = Color(0xFF06b6d4)         // Cyan accent
val MinimalNeutral900 = Color(0xFF111827)     // Almost black
val MinimalNeutral800 = Color(0xFF1f2937)     // Dark slate
val MinimalNeutral700 = Color(0xFF374151)     // Slate
val MinimalNeutral600 = Color(0xFF4b5563)     // Cool gray
val MinimalNeutral500 = Color(0xFF6b7280)     // Medium gray
val MinimalNeutral400 = Color(0xFF9ca3af)     // Light gray
val MinimalNeutral300 = Color(0xFFd1d5db)     // Pale gray
val MinimalNeutral200 = Color(0xFFe5e7eb)     // Very pale gray
val MinimalNeutral100 = Color(0xFFf3f4f6)     // Off white
val MinimalNeutral50 = Color(0xFFf9fafb)      // Pure white base

// Light theme colors - clean and airy
val MinimalPrimaryLight = Color(0xFF6366f1)   // Indigo primary
val MinimalSurfaceLight = Color(0xFFffffff)   // Pure white
val MinimalBackgroundLight = Color(0xFFfafafa) // Subtle warm white
val MinimalOnSurfaceLight = Color(0xFF1a1a1a) // Deep text

// Dark theme colors - sophisticated and deep
val MinimalPrimaryDark = Color(0xFF818cf8)    // Lighter indigo for dark
val MinimalSurfaceDark = Color(0xFF0f0f0f)    // Deep black
val MinimalBackgroundDark = Color(0xFF080808) // Ultra deep black
val MinimalOnSurfaceDark = Color(0xFFfafafa)  // Pure white text

// Preset color palette options for user customization
object ColorPalettes {
    // Warm minimalist palette
    val WarmMinimal = mapOf(
        "primary" to Color(0xFFf59e0b),      // Warm amber
        "surface" to Color(0xFFfffbeb),      // Warm white
        "background" to Color(0xFFfef3c7),   // Cream
        "onSurface" to Color(0xFF92400e)     // Warm brown
    )

    // Cool minimalist palette
    val CoolMinimal = mapOf(
        "primary" to Color(0xFF0ea5e9),      // Sky blue
        "surface" to Color(0xFFf0f9ff),      // Cool white
        "background" to Color(0xFFe0f2fe),   // Pale blue
        "onSurface" to Color(0xFF0c4a6e)     // Deep blue
    )

    // Monochrome palette
    val Monochrome = mapOf(
        "primary" to Color(0xFF000000),      // Pure black
        "surface" to Color(0xFFffffff),      // Pure white
        "background" to Color(0xFFf5f5f5),   // Light gray
        "onSurface" to Color(0xFF000000)     // Black text
    )

    // Available custom color options for user selection
    val CustomColorOptions = mapOf(
        "Purple" to mapOf(
            "primary" to Color(0xFF7c3aed),      // Purple
            "surface" to Color(0xFFfaf5ff),      // Purple white
            "background" to Color(0xFFf3e8ff),   // Pale purple
            "onSurface" to Color(0xFF581c87)     // Dark purple
        ),
        "Pink" to mapOf(
            "primary" to Color(0xFFec4899),      // Pink
            "surface" to Color(0xFFfdf2f8),      // Pink white
            "background" to Color(0xFFfce7f3),   // Pale pink
            "onSurface" to Color(0xFF9d174d)     // Dark pink
        ),
        "Green" to mapOf(
            "primary" to Color(0xFF10b981),      // Emerald
            "surface" to Color(0xFFf0fdf4),      // Green white
            "background" to Color(0xFFdcfce7),   // Pale green
            "onSurface" to Color(0xFF065f46)     // Dark green
        ),
        "Orange" to mapOf(
            "primary" to Color(0xFFf97316),      // Orange
            "surface" to Color(0xFFfff7ed),      // Orange white
            "background" to Color(0xFFfed7aa),   // Pale orange
            "onSurface" to Color(0xFF9a3412)     // Dark orange
        ),
        "Red" to mapOf(
            "primary" to Color(0xFFef4444),      // Red
            "surface" to Color(0xFFfef2f2),      // Red white
            "background" to Color(0xFFfecaca),   // Pale red
            "onSurface" to Color(0xFF991b1b)     // Dark red
        ),
        "Teal" to mapOf(
            "primary" to Color(0xFF14b8a6),      // Teal
            "surface" to Color(0xFFf0fdfa),      // Teal white
            "background" to Color(0xFFccfbf1),   // Pale teal
            "onSurface" to Color(0xFF0f766e)     // Dark teal
        )
    )
}

// GitHub Primer colors (legacy support)
val PrimerBlue = MinimalSecondary
val PrimerGray900 = MinimalNeutral900
val PrimerGray800 = MinimalNeutral800
val PrimerGray700 = MinimalNeutral700
val PrimerGray600 = MinimalNeutral600
val PrimerGray500 = MinimalNeutral500
val PrimerGray300 = MinimalNeutral300
val PrimerGray200 = MinimalNeutral200
val PrimerGray100 = MinimalNeutral100
val PrimerGray50 = MinimalNeutral50

val PrimerBlueDark = Color(0xFF818cf8)
val PrimerGray900Dark = MinimalNeutral50
val PrimerGray800Dark = MinimalNeutral100
val PrimerGray700Dark = MinimalNeutral300
val PrimerGray600Dark = MinimalNeutral400
val PrimerGray500Dark = MinimalNeutral500
val PrimerGray300Dark = MinimalNeutral600
val PrimerGray200Dark = MinimalNeutral700
val PrimerGray100Dark = MinimalNeutral800
val PrimerGray50Dark = MinimalNeutral900

// Status colors
val PrimerGreen = Color(0xFF059669)
val PrimerGreenDark = Color(0xFF10b981)
val PrimerRed = Color(0xFFdc2626)
val PrimerRedDark = Color(0xFFef4444)
val PrimerYellow = Color(0xFFf59e0b)
val PrimerYellowDark = Color(0xFFfbbf24)


// Legacy colors for backward compatibility
val ZenPrimary = PrimerGray900
val ZenSecondary = PrimerBlue
val ZenAccent = PrimerGray600

val ZenPrimaryDark = PrimerGray900Dark
val ZenSecondaryDark = PrimerBlueDark
val ZenAccentDark = PrimerGray600Dark

val TextPrimary = PrimerGray900
val TextSecondary = PrimerGray700
val TextTertiary = PrimerGray600

val Background = PrimerGray50
val BackgroundDark = PrimerGray50Dark
val Surface = Color(0xFFFFFFFF)
val SurfaceDark = PrimerGray100Dark

