package com.talauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_sessions")
data class AppSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val plannedDurationMinutes: Int,
    val startTime: Long,
    val endTime: Long? = null,
    val isActive: Boolean = true
)