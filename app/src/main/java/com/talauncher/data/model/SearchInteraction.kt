package com.talauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_interactions")
data class SearchInteractionEntity(
    @PrimaryKey val itemKey: String,
    val lastUsedAt: Long
)
