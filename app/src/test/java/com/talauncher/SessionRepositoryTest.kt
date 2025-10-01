package com.talauncher

import com.talauncher.data.database.AppSessionDao
import com.talauncher.data.model.AppSession
import com.talauncher.data.repository.SessionRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import org.junit.Assert.*

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
    fun `startSession creates new session`() = runTest {
        println("Running startSession creates new session test")
        val packageName = "com.test.app"
        val plannedDurationMinutes = 30

        whenever(appSessionDao.getActiveSessionForApp(packageName)).thenReturn(null)
        whenever(appSessionDao.insertSession(any())).thenReturn(1L)

        repository.startSession(packageName, plannedDurationMinutes)

        verify(appSessionDao).insertSession(argThat { session ->
            session.packageName == packageName &&
            session.plannedDurationMinutes == plannedDurationMinutes &&
            session.isActive &&
            session.endTime == null
        })
    }

    @Test
    fun `endSession updates session`() = runTest {
        println("Running endSession updates session test")
        val sessionId = 5L

        repository.endSession(sessionId)

        verify(appSessionDao).endSession(eq(sessionId), any())
    }
}
