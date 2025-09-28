package com.talauncher.ui.components.lib

import com.talauncher.ui.components.UiDensity
import com.talauncher.ui.components.lib.foundation.ComponentDensity

/**
 * Migration helpers to ease transition from old components to new generic components
 */

/**
 * Convert old UiDensity to new ComponentDensity
 */
fun UiDensity.toComponentDensity(): ComponentDensity {
    return when (this) {
        UiDensity.Compact -> ComponentDensity.Compact
        UiDensity.Comfortable -> ComponentDensity.Comfortable
        UiDensity.Spacious -> ComponentDensity.Spacious
    }
}