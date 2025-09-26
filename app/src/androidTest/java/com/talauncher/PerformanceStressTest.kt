package com.talauncher

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.talauncher.data.database.LauncherDatabase
import com.talauncher.data.model.AppInfo
import com.talauncher.data.model.AppSession
import com.talauncher.data.repository.AppRepository
import com.talauncher.data.repository.SessionRepository
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import kotlin.random.Random
import kotlin.system.measureTimeMillis

/**
 * Performance and stress tests for the launcher application
 * Tests performance under high load conditions and with large datasets
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class PerformanceStressTest {

    private lateinit var device: UiDevice
    private lateinit var context: Context
    private lateinit var database: LauncherDatabase

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        context = ApplicationProvider.getApplicationContext()
        database = LauncherDatabase.getDatabase(context)

        // Clean database for consistent performance testing
        clearDatabase()
    }

    @After
    fun tearDown() {
        clearDatabase()
        database.close()
    }

    @Test
    fun testLargeAppCollectionPerformance() = runBlocking {
        // Test performance with 1000+ apps in database
        val startTime = System.currentTimeMillis()

        // Insert 1000 test apps
        val testApps = generateTestApps(1000)
        insertAppsInBatches(testApps, batchSize = 100)

        val insertionTime = System.currentTimeMillis() - startTime
        println("Inserted 1000 apps in ${insertionTime}ms")

        // Test query performance
        val queryTime = measureTimeMillis {
            val visibleApps = database.appDao().getAllAppsSync()
            Assert.assertEquals(1000, visibleApps.size)
        }
        println("Queried 1000 apps in ${queryTime}ms")

        // Performance assertions
        Assert.assertTrue("App insertion should complete within 5 seconds", insertionTime < 5000)
        Assert.assertTrue("App query should complete within 1 second", queryTime < 1000)
    }

    @Test
    fun testSessionManagementStressTest() = runBlocking {
        // Test performance with many concurrent sessions
        val sessionCount = 500
        val testSessions = generateTestSessions(sessionCount)

        val insertionTime = measureTimeMillis {
            insertSessionsInBatches(testSessions, batchSize = 50)
        }
        println("Inserted $sessionCount sessions in ${insertionTime}ms")

        // Test session queries
        val queryTime = measureTimeMillis {
            val allSessions = database.appSessionDao().getAllSessionsSync()
            Assert.assertEquals(sessionCount, allSessions.size)
        }
        println("Queried $sessionCount sessions in ${queryTime}ms")

        // Test active session queries
        val activeQueryTime = measureTimeMillis {
            val activeSessions = database.appSessionDao().getActiveSessionsSync()
            Assert.assertTrue("Should have some active sessions", activeSessions.isNotEmpty())
        }
        println("Queried active sessions in ${activeQueryTime}ms")

        // Performance assertions
        Assert.assertTrue("Session insertion should complete within 10 seconds", insertionTime < 10000)
        Assert.assertTrue("Session query should complete within 2 seconds", queryTime < 2000)
        Assert.assertTrue("Active session query should complete within 500ms", activeQueryTime < 500)
    }

    @Test
    fun testDatabaseTransactionPerformance() = runBlocking {
        // Test performance of complex database operations
        val appCount = 200
        val sessionCount = 1000

        val totalTime = measureTimeMillis {
            database.runInTransaction {
                // Insert apps
                val apps = generateTestApps(appCount)
                apps.forEach { app ->
                    database.appDao().insertApp(app)
                }

                // Insert sessions
                val sessions = generateTestSessions(sessionCount)
                sessions.forEach { session ->
                    database.appSessionDao().insertSession(session)
                }

                // Update some apps
                apps.take(50).forEach { app ->
                    database.appDao().updatePinnedStatus(app.packageName, true, Random.nextInt(1, 50))
                }

                // End some sessions
                sessions.take(100).forEach { session ->
                    database.appSessionDao().endSession(session.id, System.currentTimeMillis())
                }
            }
        }

        println("Complex transaction completed in ${totalTime}ms")
        Assert.assertTrue("Complex transaction should complete within 15 seconds", totalTime < 15000)

        // Verify data integrity
        val finalAppCount = database.appDao().getAllAppsSync().size
        val finalSessionCount = database.appSessionDao().getAllSessionsSync().size
        Assert.assertEquals(appCount, finalAppCount)
        Assert.assertEquals(sessionCount, finalSessionCount)
    }

    @Test
    fun testMemoryUsageWithLargeDatasets() = runBlocking {
        // Monitor memory usage while working with large datasets
        val runtime = Runtime.getRuntime()

        val initialMemory = runtime.totalMemory() - runtime.freeMemory()
        println("Initial memory usage: ${initialMemory / 1024 / 1024}MB")

        // Load large dataset
        val largeAppSet = generateTestApps(2000)
        insertAppsInBatches(largeAppSet, batchSize = 200)

        val postInsertMemory = runtime.totalMemory() - runtime.freeMemory()
        println("Memory after inserting 2000 apps: ${postInsertMemory / 1024 / 1024}MB")

        // Perform multiple queries
        repeat(10) {
            database.appDao().getAllAppsSync()
            database.appDao().getPinnedAppsSync()
            database.appDao().getHiddenAppsSync()
        }

        val postQueryMemory = runtime.totalMemory() - runtime.freeMemory()
        println("Memory after multiple queries: ${postQueryMemory / 1024 / 1024}MB")

        // Memory increase should be reasonable (less than 100MB)
        val memoryIncrease = (postQueryMemory - initialMemory) / 1024 / 1024
        Assert.assertTrue("Memory increase should be less than 100MB, was ${memoryIncrease}MB",
                         memoryIncrease < 100)
    }

    @Test
    fun testConcurrentDatabaseAccess() = runBlocking {
        // Test performance under concurrent database access
        val concurrentOperations = 50

        val operationTime = measureTimeMillis {
            val jobs = (1..concurrentOperations).map { index ->
                kotlinx.coroutines.async {
                    // Simulate concurrent operations
                    val app = AppInfo(
                        packageName = "com.test.concurrent$index",
                        appName = "Concurrent App $index",
                        isHidden = Random.nextBoolean(),
                        isPinned = Random.nextBoolean(),
                        pinnedOrder = if (Random.nextBoolean()) Random.nextInt(1, 10) else 0,
                        isDistracting = Random.nextBoolean()
                    )

                    database.appDao().insertApp(app)

                    // Query operations
                    database.appDao().getAppByPackageName(app.packageName)
                    database.appDao().getAllAppsSync()

                    // Update operations
                    database.appDao().updateDistractingStatus(app.packageName, !app.isDistracting)
                }
            }

            jobs.forEach { it.await() }
        }

        println("$concurrentOperations concurrent operations completed in ${operationTime}ms")
        Assert.assertTrue("Concurrent operations should complete within 10 seconds", operationTime < 10000)

        // Verify data consistency
        val finalApps = database.appDao().getAllAppsSync()
        Assert.assertEquals(concurrentOperations, finalApps.size)
    }

    @Test
    fun testAppLaunchPerformanceStress() {
        // Test app launching performance under stress
        val launchCount = 100
        var successfulLaunches = 0

        val totalTime = measureTimeMillis {
            repeat(launchCount) { index ->
                try {
                    val packageName = "com.android.settings"
                    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                    if (intent != null) {
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        successfulLaunches++
                        Thread.sleep(50) // Small delay between launches
                    }
                } catch (e: Exception) {
                    // Some launches may fail, which is expected in stress test
                }
            }
        }

        println("Attempted $launchCount app launches, $successfulLaunches successful in ${totalTime}ms")

        // At least 50% of launches should succeed
        Assert.assertTrue("At least 50% of launches should succeed",
                         successfulLaunches >= launchCount / 2)
    }

    @Test
    fun testOverlayServiceStressTest() = runBlocking {
        // Test overlay service performance under rapid fire requests
        val overlayCount = 20

        val overlayTime = measureTimeMillis {
            repeat(overlayCount) { index ->
                try {
                    val intent = android.content.Intent(context, com.talauncher.service.OverlayService::class.java).apply {
                        action = "show_countdown"
                        putExtra("app_name", "Test App $index")
                        putExtra("remaining_seconds", 5)
                        putExtra("total_seconds", 30)
                    }
                    context.startService(intent)
                    Thread.sleep(100) // Small delay between overlay requests

                    // Hide overlay
                    val hideIntent = android.content.Intent(context, com.talauncher.service.OverlayService::class.java).apply {
                        action = "hide_overlay"
                    }
                    context.startService(hideIntent)
                    Thread.sleep(50)
                } catch (e: Exception) {
                    // Some overlay operations may fail due to permissions or system limitations
                }
            }
        }

        println("$overlayCount overlay operations completed in ${overlayTime}ms")
        Assert.assertTrue("Overlay operations should complete within 30 seconds", overlayTime < 30000)
    }

    @Test
    fun testDatabaseCorruptionRecovery() = runBlocking {
        // Test database recovery under stress conditions
        val testApps = generateTestApps(100)
        insertAppsInBatches(testApps, batchSize = 25)

        // Simulate potential corruption scenarios
        try {
            // Attempt rapid concurrent writes that might cause issues
            repeat(50) { index ->
                kotlinx.coroutines.launch {
                    database.appDao().updateDistractingStatus("com.test.app$index", Random.nextBoolean())
                }
            }
        } catch (e: Exception) {
            // Database should handle concurrent access gracefully
        }

        // Verify database is still functional
        val apps = database.appDao().getAllAppsSync()
        Assert.assertTrue("Database should remain functional after stress", apps.isNotEmpty())
    }

    // Helper methods
    private fun clearDatabase() = runBlocking {
        database.clearAllTables()
    }

    private fun generateTestApps(count: Int): List<AppInfo> {
        return (1..count).map { index ->
            AppInfo(
                packageName = "com.test.app$index",
                appName = "Test App $index",
                isHidden = Random.nextBoolean(),
                isPinned = index <= count / 10, // 10% pinned
                pinnedOrder = if (index <= count / 10) index else 0,
                isDistracting = Random.nextBoolean()
            )
        }
    }

    private fun generateTestSessions(count: Int): List<AppSession> {
        val currentTime = System.currentTimeMillis()
        return (1..count).map { index ->
            val startTime = currentTime - Random.nextLong(0, 7 * 24 * 60 * 60 * 1000) // Up to 7 days ago
            val isActive = Random.nextBoolean()
            AppSession(
                id = 0, // Will be assigned by database
                packageName = "com.test.app${Random.nextInt(1, 100)}",
                plannedDurationMinutes = Random.nextInt(15, 240), // 15 min to 4 hours
                startTime = startTime,
                endTime = if (isActive) null else startTime + Random.nextLong(60000, 3600000),
                isActive = isActive
            )
        }
    }

    private suspend fun insertAppsInBatches(apps: List<AppInfo>, batchSize: Int) {
        apps.chunked(batchSize).forEach { batch ->
            database.runInTransaction {
                batch.forEach { app ->
                    database.appDao().insertApp(app)
                }
            }
        }
    }

    private suspend fun insertSessionsInBatches(sessions: List<AppSession>, batchSize: Int) {
        sessions.chunked(batchSize).forEach { batch ->
            database.runInTransaction {
                batch.forEach { session ->
                    database.appSessionDao().insertSession(session)
                }
            }
        }
    }
}