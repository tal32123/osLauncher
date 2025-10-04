package com.talauncher.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.talauncher.data.model.NewsArticle

@Composable
fun NewsCarousel(
    articles: List<NewsArticle>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Carousel(
        items = articles,
        modifier = modifier,
        autoScrollMillis = 8000L
    ) { article ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = article.link.isNotBlank()) {
                    runCatching {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.link))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                }
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = article.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

