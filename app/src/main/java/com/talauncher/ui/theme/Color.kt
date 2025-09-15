package com.talauncher.ui.theme

import androidx.compose.ui.graphics.Color

// GitHub Primer-inspired color palette
// Based on GitHub's design system: https://primer.style/foundations/color

// Primary colors - GitHub's signature blues and grays
val PrimerBlue = Color(0xFF0969da)         // GitHub blue (primary action)
val PrimerGray900 = Color(0xFF24292f)      // Dark text (high contrast)
val PrimerGray800 = Color(0xFF32383f)      // Medium dark text
val PrimerGray700 = Color(0xFF424a53)      // Body text
val PrimerGray600 = Color(0xFF656d76)      // Muted text
val PrimerGray500 = Color(0xFF848d97)      // Placeholder text
val PrimerGray300 = Color(0xFFd0d7de)      // Borders
val PrimerGray200 = Color(0xFFd8dee4)      // Subtle borders
val PrimerGray100 = Color(0xFFf6f8fa)      // Canvas subtle
val PrimerGray50 = Color(0xFFfafbfc)       // Canvas default

// Dark mode colors
val PrimerBlueDark = Color(0xFF58a6ff)     // Blue for dark mode
val PrimerGray900Dark = Color(0xFFf0f6fc)  // Light text on dark
val PrimerGray800Dark = Color(0xFFe6edf3)  // Medium light text
val PrimerGray700Dark = Color(0xFFb1bac4)  // Body text dark
val PrimerGray600Dark = Color(0xFF8b949e)  // Muted text dark
val PrimerGray500Dark = Color(0xFF6e7681)  // Placeholder dark
val PrimerGray300Dark = Color(0xFF484f58)  // Borders dark
val PrimerGray200Dark = Color(0xFF30363d)  // Subtle borders dark
val PrimerGray100Dark = Color(0xFF21262d)  // Canvas subtle dark
val PrimerGray50Dark = Color(0xFF0d1117)   // Canvas default dark

// Status colors - GitHub's semantic colors
val PrimerGreen = Color(0xFF1a7f37)        // Success
val PrimerGreenDark = Color(0xFF3fb950)    // Success dark
val PrimerRed = Color(0xFFd1242f)          // Danger
val PrimerRedDark = Color(0xFFf85149)      // Danger dark
val PrimerYellow = Color(0xFFbf8700)       // Warning
val PrimerYellowDark = Color(0xFFd29922)   // Warning dark


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

