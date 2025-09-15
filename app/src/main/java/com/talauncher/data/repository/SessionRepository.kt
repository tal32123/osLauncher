package com.talauncher.data.repository

import com.talauncher.data.database.AppSessionDao
import com.talauncher.data.model.AppSession
import kotlinx.coroutines.flow.Flow

class SessionRepository(private val appSessionDao: AppSessionDao) {

    fun getActiveSessions(): Flow<List<AppSession>> = appSessionDao.getActiveSessions()

    suspend fun getActiveSessionForApp(packageName: String): AppSession? =
        appSessionDao.getActiveSessionForApp(packageName)

    suspend fun startSession(packageName: String, plannedDurationMinutes: Int): Long {
        val session = AppSession(
            packageName = packageName,
            plannedDurationMinutes = plannedDurationMinutes,
            startTime = System.currentTimeMillis(),
            isActive = true
        )
        return appSessionDao.insertSession(session)
    }

    suspend fun endSession(sessionId: Long) {
        appSessionDao.endSession(sessionId, System.currentTimeMillis())
    }

    suspend fun endSessionForApp(packageName: String) {
        val activeSession = getActiveSessionForApp(packageName)
        activeSession?.let { session ->
            endSession(session.id)
        }
    }

    suspend fun cleanupOldSessions() {
        val cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L) // 7 days ago
        appSessionDao.deleteOldSessions(cutoffTime)
    }

    fun isSessionExpired(session: AppSession): Boolean {
        val currentTime = System.currentTimeMillis()
        val sessionDuration = currentTime - session.startTime
        val plannedDuration = session.plannedDurationMinutes * 60 * 1000L
        return sessionDuration >= plannedDuration
    }

    fun getRemainingTime(session: AppSession): Long {
        val currentTime = System.currentTimeMillis()
        val sessionDuration = currentTime - session.startTime
        val plannedDuration = session.plannedDurationMinutes * 60 * 1000L
        return maxOf(0, plannedDuration - sessionDuration)
    }
}