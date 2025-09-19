package com.talauncher.data.repository

import com.talauncher.data.database.AppSessionDao
import com.talauncher.data.model.AppSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class SessionRepository(private val appSessionDao: AppSessionDao) {

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val expirationEvents = MutableSharedFlow<AppSession>(extraBufferCapacity = 16)
    private val expirationJobs = mutableMapOf<Long, Job>()
    private val jobsMutex = Mutex()

    suspend fun cleanup() {
        jobsMutex.withLock {
            expirationJobs.values.forEach { it.cancel() }
            expirationJobs.clear()
        }
    }

    fun getActiveSessions(): Flow<List<AppSession>> = appSessionDao.getActiveSessions()

    suspend fun initialize() {
        val activeSessions = appSessionDao.getActiveSessions().first()
        activeSessions.forEach { scheduleExpiration(it) }
    }

    fun observeSessionExpirations(): SharedFlow<AppSession> = expirationEvents.asSharedFlow()

    suspend fun emitExpiredSessions() {
        val activeSessions = appSessionDao.getActiveSessions().first()
        activeSessions.filter { isSessionExpired(it) }.forEach { triggerExpiration(it) }
    }

    suspend fun getActiveSessionForApp(packageName: String): AppSession? =
        appSessionDao.getActiveSessionForApp(packageName)

    suspend fun getExpiredSessions(): List<AppSession> {
        val activeSessions = appSessionDao.getActiveSessions().first()
        return activeSessions.filter { isSessionExpired(it) }
    }

    suspend fun startSession(packageName: String, plannedDurationMinutes: Int): Long {
        appSessionDao.getActiveSessionForApp(packageName)?.let { endSession(it.id) }

        val session = AppSession(
            packageName = packageName,
            plannedDurationMinutes = plannedDurationMinutes,
            startTime = System.currentTimeMillis(),
            isActive = true
        )
        val sessionId = appSessionDao.insertSession(session)
        scheduleExpiration(session.copy(id = sessionId))
        return sessionId
    }

    suspend fun endSession(sessionId: Long) {
        cancelExpirationJob(sessionId)
        appSessionDao.endSession(sessionId, System.currentTimeMillis())
    }

    suspend fun endSessionForApp(packageName: String) {
        appSessionDao.getActiveSessionForApp(packageName)?.let { session ->
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

    private fun scheduleExpiration(session: AppSession) {
        coroutineScope.launch {
            cancelExpirationJob(session.id)
            val job = coroutineScope.launch {
                val remainingTime = getRemainingTime(session)
                if (remainingTime > 0) {
                    delay(remainingTime)
                }
                triggerExpiration(session)
            }
            jobsMutex.withLock {
                expirationJobs[session.id] = job
            }
        }
    }

    private suspend fun triggerExpiration(session: AppSession) {
        var sessionForEvent: AppSession? = null
        withContext(Dispatchers.IO) {
            val activeSession = appSessionDao.getActiveSessionForApp(session.packageName)
            if (activeSession?.id == session.id && activeSession.isActive) {
                val endTime = System.currentTimeMillis()
                // End session first to prevent race conditions
                appSessionDao.endSession(session.id, endTime)
                sessionForEvent = activeSession.copy(isActive = false, endTime = endTime)
            }
        }
        cancelExpirationJob(session.id)
        sessionForEvent?.let { expirationEvents.emit(it) }
    }

    private suspend fun cancelExpirationJob(sessionId: Long) {
        jobsMutex.withLock {
            expirationJobs.remove(sessionId)?.cancel()
        }
    }
}

