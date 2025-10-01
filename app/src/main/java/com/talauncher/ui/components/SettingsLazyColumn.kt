package com.talauncher.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Generic lazy column optimized for settings screens with navigation bar padding.
 */
@Composable
fun SettingsLazyColumn(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    extraBottomPadding: Dp = 24.dp,
    content: LazyListScope.() -> Unit
) {
    val layoutDirection = LocalLayoutDirection.current
    val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues()
    val contentPadding = PaddingValues(
        start = navigationBarsPadding.calculateStartPadding(layoutDirection),
        top = navigationBarsPadding.calculateTopPadding(),
        end = navigationBarsPadding.calculateEndPadding(layoutDirection),
        bottom = navigationBarsPadding.calculateBottomPadding() + extraBottomPadding
    )

    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement,
        content = content
    )
}
