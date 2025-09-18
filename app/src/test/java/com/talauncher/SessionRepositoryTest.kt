package com.talauncher

import com.talauncher.data.database.AppSessionDao
import com.talauncher.data.model.AppSession
import com.talauncher.data.repository.SessionRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.junit.Assert.*

/**
 * Unit tests for SessionRepository
 * Tests session management, timing, and expiration logic
 */
@RunWith(RobolectricTestRunner::class)
class SessionRepositoryTest {

    @Mock
    private lateinit var appSessionDao: AppSessionDao

    private lateinit var repository: SessionRepository

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        repository = SessionRepository(appSessionDao)
    }

    @Test
    fun `startSession creates new session with correct timing`() = runTest {
        // Given
        val packageName = "com.test.app"
        val plannedDurationMinutes = 30
        val expectedDurationMs = 30 * 60 * 1000L

        whenever(appSessionDao.getActiveSessionForApp(packageName)).thenReturn(null)
        whenever(appSessionDao.insertSession(any())).thenReturn(1L)

        // When
        repository.startSession(packageName, plannedDurationMinutes)

        // Then
        verify(appSessionDao).insertSession(argThat { session ->
            session.packageName == packageName &&
            session.plannedDurationMs == expectedDurationMs &&
            session.isActive &&
            session.endTime == null
        })
    }

    @Test
    fun `startSession ends existing active session before starting new one`() = runTest {
        // Given
        val packageName = "com.test.app"
        val existingSession = AppSession(
            id = 1L,
            packageName = packageName,
            startTime = System.currentTimeMillis() - 10000,
            plannedDurationMs = 600000,
            isActive = true
        )

        whenever(appSessionDao.getActiveSessionForApp(packageName)).thenReturn(existingSession)
        whenever(appSessionDao.insertSession(any())).thenReturn(2L)

        // When
        repository.startSession(packageName, 15)

        // Then
        verify(appSessionDao).endSession(eq(1L), any())
        verify(appSessionDao).insertSession(any())
    }

    @Test
    fun `endSession updates session with end time and marks as inactive`() = runTest {
        // Given
        val sessionId = 5L
        val endTime = System.currentTimeMillis()

        // When
        repository.endSession(sessionId, endTime)

        // Then
        verify(appSessionDao).endSession(sessionId, endTime)
    }

    @Test
    fun `getActiveSessionForApp returns current active session`() = runTest {
        // Given
        val packageName = "com.test.app"
        val activeSession = AppSession(
            id = 3L,
            packageName = packageName,
            startTime = System.currentTimeMillis() - 5000,
            plannedDurationMs = 1800000,
            isActive = true
        )

        whenever(appSessionDao.getActiveSessionForApp(packageName)).thenReturn(activeSession)

        // When
        val result = repository.getActiveSessionForApp(packageName)

        // Then
        assertEquals(activeSession, result)
    }

    @Test
    fun `getActiveSessionForApp returns null when no active session`() = runTest {
        // Given
        val packageName = "com.test.app"

        whenever(appSessionDao.getActiveSessionForApp(packageName)).thenReturn(null)

        // When
        val result = repository.getActiveSessionForApp(packageName)

        // Then
        assertNull(result)
    }

    @Test
    fun `getAllSessions returns all sessions from dao`() = runTest {
        // Given
        val sessions = listOf(
            AppSession(
                id = 1L,
                packageName = "com.test.app1",
                startTime = System.currentTimeMillis() - 3600000,
                plannedDurationMs = 1800000,
                isActive = false,
                endTime = System.currentTimeMillis() - 1800000
            ),
            AppSession(
                id = 2L,
                packageName = "com.test.app2",
                startTime = System.currentTimeMillis() - 1800000,
                plannedDurationMs = 3600000,
                isActive = true
            )
        )

        whenever(appSessionDao.getAllSessions()).thenReturn(flowOf(sessions))

        // When
        val result = repository.getAllSessions().first()

        // Then
        assertEquals(sessions, result)
    }

    @Test
    fun `getSessionsForApp returns sessions for specific package`() = runTest {
        // Given
        val packageName = "com.test.app"
        val sessions = listOf(
            AppSession(
                id = 1L,
                packageName = packageName,
                startTime = System.currentTimeMillis() - 7200000,
                plannedDurationMs = 1800000,
                isActive = false,
                endTime = System.currentTimeMillis() - 5400000
            ),
            AppSession(
                id = 2L,
                packageName = packageName,
                startTime = System.currentTimeMillis() - 1800000,
                plannedDurationMs = 3600000,
                isActive = false,
                endTime = System.currentTimeMillis() - 600000
            )
        )

        whenever(appSessionDao.getSessionsForApp(packageName)).thenReturn(flowOf(sessions))

        // When
        val result = repository.getSessionsForApp(packageName).first()

        // Then
        assertEquals(sessions, result)
        assertTrue(result.all { it.packageName == packageName })
    }

    @Test
    fun `getRemainingTime returns correct remaining time for active session`() = runTest {
        // Given
        val currentTime = System.currentTimeMillis()
        val session = AppSession(
            id = 1L,
            packageName = "com.test.app",
            startTime = currentTime - 300000, // 5 minutes ago
            plannedDurationMs = 1800000, // 30 minutes planned
            isActive = true
        )

        // When
        val remainingTime = repository.getRemainingTime(session)

        // Then
        // Should have approximately 25 minutes (1500 seconds) remaining
        assertTrue("Remaining time should be around 1500 seconds", remainingTime in 1400..1600)
    }

    @Test
    fun `getRemainingTime returns zero for expired session`() = runTest {
        // Given
        val currentTime = System.currentTimeMillis()
        val session = AppSession(
            id = 1L,
            packageName = "com.test.app",
            startTime = currentTime - 3600000, // 1 hour ago
            plannedDurationMs = 1800000, // 30 minutes planned
            isActive = true
        )

        // When
        val remainingTime = repository.getRemainingTime(session)

        // Then
        assertEquals(0L, remainingTime)
    }

    @Test
    fun `getRemainingTime returns zero for inactive session`() = runTest {
        // Given
        val currentTime = System.currentTimeMillis()
        val session = AppSession(
            id = 1L,
            packageName = "com.test.app",
            startTime = currentTime - 300000,
            plannedDurationMs = 1800000,
            isActive = false,
            endTime = currentTime - 60000
        )

        // When
        val remainingTime = repository.getRemainingTime(session)

        // Then
        assertEquals(0L, remainingTime)
    }

    @Test
    fun `isSessionExpired returns true for expired session`() = runTest {
        // Given
        val currentTime = System.currentTimeMillis()
        val session = AppSession(
            id = 1L,
            packageName = "com.test.app",
            startTime = currentTime - 3600000, // 1 hour ago
            plannedDurationMs = 1800000, // 30 minutes planned
            isActive = true
        )

        // When
        val isExpired = repository.isSessionExpired(session)

        // Then
        assertTrue(isExpired)
    }

    @Test
    fun `isSessionExpired returns false for active session within time limit`() = runTest {
        // Given
        val currentTime = System.currentTimeMillis()
        val session = AppSession(
            id = 1L,
            packageName = "com.test.app",
            startTime = currentTime - 300000, // 5 minutes ago
            plannedDurationMs = 1800000, // 30 minutes planned
            isActive = true
        )

        // When
        val isExpired = repository.isSessionExpired(session)

        // Then
        assertFalse(isExpired)
    }

    @Test
    fun `isSessionExpired returns false for inactive session`() = runTest {
        // Given
        val currentTime = System.currentTimeMillis()
        val session = AppSession(
            id = 1L,
            packageName = "com.test.app",
            startTime = currentTime - 3600000,
            plannedDurationMs = 1800000,
            isActive = false,
            endTime = currentTime - 1800000
        )

        // When
        val isExpired = repository.isSessionExpired(session)

        // Then
        assertFalse(isExpired)
    }

    @Test
    fun `extendSession updates session duration correctly`() = runTest {
        // Given
        val sessionId = 10L
        val additionalMinutes = 15
        val expectedAdditionalMs = 15 * 60 * 1000L

        // When
        repository.extendSession(sessionId, additionalMinutes)

        // Then
        verify(appSessionDao).extendSession(sessionId, expectedAdditionalMs)
    }

    @Test
    fun `getRecentSessions returns limited number of recent sessions`() = runTest {
        // Given
        val limit = 5
        val recentSessions = (1..5).map { id ->
            AppSession(
                id = id.toLong(),
                packageName = "com.test.app$id",
                startTime = System.currentTimeMillis() - (id * 3600000),
                plannedDurationMs = 1800000,
                isActive = false,
                endTime = System.currentTimeMillis() - (id * 1800000)
            )
        }

        whenever(appSessionDao.getRecentSessions(limit)).thenReturn(flowOf(recentSessions))

        // When
        val result = repository.getRecentSessions(limit).first()

        // Then
        assertEquals(recentSessions, result)
        assertEquals(limit, result.size)
    }

    @Test
    fun `cleanupOldSessions removes sessions older than cutoff`() = runTest {
        // Given
        val cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000) // 7 days ago

        // When
        repository.cleanupOldSessions(cutoffTime)

        // Then
        verify(appSessionDao).deleteSessionsOlderThan(cutoffTime)
    }
}