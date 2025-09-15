package com.talauncher.data.database

import androidx.room.*
import com.talauncher.data.model.AppSession
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSessionDao {
    @Query("SELECT * FROM app_sessions WHERE isActive = 1")
    fun getActiveSessions(): Flow<List<AppSession>>

    @Query("SELECT * FROM app_sessions WHERE packageName = :packageName AND isActive = 1 LIMIT 1")
    suspend fun getActiveSessionForApp(packageName: String): AppSession?

    @Insert
    suspend fun insertSession(session: AppSession): Long

    @Update
    suspend fun updateSession(session: AppSession)

    @Query("UPDATE app_sessions SET isActive = 0, endTime = :endTime WHERE id = :sessionId")
    suspend fun endSession(sessionId: Long, endTime: Long)

    @Query("DELETE FROM app_sessions WHERE endTime IS NOT NULL AND endTime < :cutoffTime")
    suspend fun deleteOldSessions(cutoffTime: Long)
}