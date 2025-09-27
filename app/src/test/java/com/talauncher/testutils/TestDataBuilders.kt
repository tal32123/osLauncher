package com.talauncher.testutils

import com.talauncher.data.model.AppInfo
import com.talauncher.data.model.AppSession
import com.talauncher.data.model.LauncherSettings

/**
 * Test data builders for creating consistent test objects
 * Follows the Test Data Builder pattern for maintainable test data creation
 */

/**
 * Builder for AppInfo test data
 */
class AppInfoBuilder {
    private var packageName: String = "com.example.test"
    private var appName: String = "Test App"
    private var isHidden: Boolean = false
    private var isDistracting: Boolean = false
    private var timeLimitMinutes: Int? = null

    fun withPackageName(packageName: String) = apply { this.packageName = packageName }
    fun withAppName(appName: String) = apply { this.appName = appName }
    fun withHidden(isHidden: Boolean) = apply { this.isHidden = isHidden }
    fun withDistracting(isDistracting: Boolean) = apply { this.isDistracting = isDistracting }
    fun withTimeLimitMinutes(timeLimitMinutes: Int?) = apply { this.timeLimitMinutes = timeLimitMinutes }

    fun build() = AppInfo(
        packageName = packageName,
        appName = appName,
        isHidden = isHidden,
        isDistracting = isDistracting,
        timeLimitMinutes = timeLimitMinutes
    )
}

/**
 * Builder for AppSession test data
 */
class AppSessionBuilder {
    private var id: Long = 0
    private var packageName: String = "com.example.test"
    private var plannedDurationMinutes: Int = 30
    private var startTime: Long = System.currentTimeMillis()
    private var endTime: Long? = null
    private var isActive: Boolean = true

    fun withId(id: Long) = apply { this.id = id }
    fun withPackageName(packageName: String) = apply { this.packageName = packageName }
    fun withPlannedDurationMinutes(plannedDurationMinutes: Int) = apply { this.plannedDurationMinutes = plannedDurationMinutes }
    fun withStartTime(startTime: Long) = apply { this.startTime = startTime }
    fun withEndTime(endTime: Long?) = apply { this.endTime = endTime }
    fun withActive(isActive: Boolean) = apply { this.isActive = isActive }

    fun build() = AppSession(
        id = id,
        packageName = packageName,
        plannedDurationMinutes = plannedDurationMinutes,
        startTime = startTime,
        endTime = endTime,
        isActive = isActive
    )
}

/**
 * Builder for LauncherSettings test data
 */
class LauncherSettingsBuilder {
    private var id: Int = 1
    private var showTimeOnHomeScreen: Boolean = true
    private var showDateOnHomeScreen: Boolean = true
    private var isOnboardingCompleted: Boolean = false
    private var backgroundColor: String = "system"
    private var enableHapticFeedback: Boolean = true

    fun withId(id: Int) = apply { this.id = id }
    fun withShowTimeOnHomeScreen(showTimeOnHomeScreen: Boolean) = apply { this.showTimeOnHomeScreen = showTimeOnHomeScreen }
    fun withShowDateOnHomeScreen(showDateOnHomeScreen: Boolean) = apply { this.showDateOnHomeScreen = showDateOnHomeScreen }
    fun withOnboardingCompleted(isOnboardingCompleted: Boolean) = apply { this.isOnboardingCompleted = isOnboardingCompleted }
    fun withBackgroundColor(backgroundColor: String) = apply { this.backgroundColor = backgroundColor }
    fun withHapticFeedback(enableHapticFeedback: Boolean) = apply { this.enableHapticFeedback = enableHapticFeedback }

    fun build() = LauncherSettings(
        id = id,
        showTimeOnHomeScreen = showTimeOnHomeScreen,
        showDateOnHomeScreen = showDateOnHomeScreen,
        isOnboardingCompleted = isOnboardingCompleted,
        backgroundColor = backgroundColor,
        enableHapticFeedback = enableHapticFeedback
    )
}

/**
 * Convenience functions for quick test data creation
 */
fun testApp(builder: AppInfoBuilder.() -> Unit = {}): AppInfo =
    AppInfoBuilder().apply(builder).build()

fun testSession(builder: AppSessionBuilder.() -> Unit = {}): AppSession =
    AppSessionBuilder().apply(builder).build()

fun testSettings(builder: LauncherSettingsBuilder.() -> Unit = {}): LauncherSettings =
    LauncherSettingsBuilder().apply(builder).build()

/**
 * Common test data sets
 */
object TestData {
    val sampleApps = listOf(
        testApp {
            withPackageName("com.android.settings")
            withAppName("Settings")
        },
        testApp {
            withPackageName("com.android.chrome")
            withAppName("Chrome")
        },
        testApp {
            withPackageName("com.spotify.music")
            withAppName("Spotify")
            withHidden(true)
            withDistracting(true)
            withTimeLimitMinutes(60)
        }
    )

    val sampleSessions = listOf(
        testSession {
            withPackageName("com.android.settings")
            withStartTime(System.currentTimeMillis() - 60000)
            withPlannedDurationMinutes(30)
            withActive(false)
        },
        testSession {
            withPackageName("com.android.chrome")
            withStartTime(System.currentTimeMillis() - 120000)
            withPlannedDurationMinutes(45)
            withActive(true)
        }
    )

    val sampleSettings = testSettings {
        withOnboardingCompleted(true)
        withBackgroundColor("black")
        withHapticFeedback(false)
    }
}