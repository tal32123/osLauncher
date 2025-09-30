package com.talauncher.utils

import android.content.Context
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.talauncher.MainActivity
import com.talauncher.data.database.LauncherDatabase
import com.talauncher.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

private fun getSettingsRepository(): SettingsRepository {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = LauncherDatabase.getDatabase(context)
    return SettingsRepository(database.settingsDao())
}

fun markOnboardingComplete() {
    runBlocking {
        withContext(Dispatchers.IO) {
            getSettingsRepository().completeOnboarding()
        }
    }
}

fun resetOnboardingState() {
    runBlocking {
        withContext(Dispatchers.IO) {
            val repository = getSettingsRepository()
            val settings = repository.getSettingsSync()
            repository.updateSettings(settings.copy(isOnboardingCompleted = false))
        }
    }
}

fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.skipOnboardingIfNeeded() {
    markOnboardingComplete()

    waitUntil(timeoutMillis = 10_000) {
        try {
            onNodeWithTag("launcher_navigation_pager").assertExists()
            true
        } catch (error: AssertionError) {
            false
        }
    }

    waitForIdle()
}
