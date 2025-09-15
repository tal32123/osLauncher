package com.talauncher.ui.theme

import androidx.compose.ui.unit.dp

// GitHub Primer spacing system
// Based on 4dp base unit with multipliers: https://primer.style/foundations/spacing

object PrimerSpacing {
    // Base spacing unit
    val base = 4.dp

    // Primer spacing scale
    val xs = 4.dp      // 1 unit
    val sm = 8.dp      // 2 units
    val md = 16.dp     // 4 units
    val lg = 24.dp     // 6 units
    val xl = 32.dp     // 8 units
    val xxl = 40.dp    // 10 units
    val xxxl = 48.dp   // 12 units

    // Component-specific spacing
    val componentPadding = md
    val cardPadding = lg
    val screenPadding = lg
    val listItemPadding = md
    val buttonPadding = sm

    // Layout spacing
    val sectionSpacing = xl
    val itemSpacing = sm
    val tightSpacing = xs
}