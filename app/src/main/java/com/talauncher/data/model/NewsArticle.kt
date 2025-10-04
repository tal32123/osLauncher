package com.talauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "news_articles")
data class NewsArticle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val link: String,
    val category: String?,
    val publishedAtMillis: Long,
)

