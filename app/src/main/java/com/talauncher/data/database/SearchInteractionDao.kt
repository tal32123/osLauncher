package com.talauncher.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.talauncher.data.model.SearchInteractionEntity

@Dao
interface SearchInteractionDao {
    @Query("SELECT * FROM search_interactions WHERE itemKey IN (:keys)")
    suspend fun getInteractions(keys: List<String>): List<SearchInteractionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInteraction(interaction: SearchInteractionEntity)
}
