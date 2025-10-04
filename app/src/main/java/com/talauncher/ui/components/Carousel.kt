package com.talauncher.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun <T> Carousel(
    items: List<T>,
    modifier: Modifier = Modifier,
    autoScrollMillis: Long = 0,
    itemContent: @Composable (T) -> Unit
) {
    if (items.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { items.size })
    val scope = rememberCoroutineScope()

    LaunchedEffect(items.size, autoScrollMillis) {
        if (autoScrollMillis > 0 && items.size > 1) {
            while (true) {
                delay(autoScrollMillis)
                scope.launch {
                    val next = (pagerState.currentPage + 1) % items.size
                    pagerState.animateScrollToPage(next)
                }
            }
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 24.dp, max = 80.dp)
    ) { page ->
        Box(modifier = Modifier.fillMaxWidth()) {
            itemContent(items[page])
        }
    }
}

