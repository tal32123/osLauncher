package com.talauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "news_articles")
data class NewsArticle(
    @PrimaryKey val link: String,
    val title: String,
    val category: String?,
    val publishedAtMillis: Long,
)

