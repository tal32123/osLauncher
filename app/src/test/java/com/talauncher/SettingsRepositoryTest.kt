package com.talauncher

import com.talauncher.data.database.SettingsDao
import com.talauncher.data.model.LauncherSettings
import com.talauncher.data.repository.SettingsRepository
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
 * Unit tests for SettingsRepository
 * Tests settings persistence, validation, and default value handling
 */
@RunWith(RobolectricTestRunner::class)
class SettingsRepositoryTest {

    @Mock
    private lateinit var settingsDao: SettingsDao

    private lateinit var repository: SettingsRepository

    private val defaultSettings = LauncherSettings(
        id = 1,
        darkMode = false,
        showAppCounts = true,
        gridSize = 4,
        autoHideNavigationBar = false,
        showRecentApps = true,
        enableTimeLimitPrompt = false,
        enableMathChallenge = false,
        mathDifficulty = "easy",
        sessionExpiryCountdownSeconds = 5,
        recentAppsLimit = 10
    )

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        repository = SettingsRepository(settingsDao)
    }

    @Test
    fun `getSettings returns settings from dao`() = runTest {
        // Given
        val settings = defaultSettings.copy(darkMode = true, gridSize = 5)
        whenever(settingsDao.getSettings()).thenReturn(flowOf(settings))

        // When
        val result = repository.getSettings().first()

        // Then
        assertEquals(settings, result)
    }

    @Test
    fun `getSettingsSync returns settings synchronously`() = runTest {
        // Given
        val settings = defaultSettings.copy(showAppCounts = false)
        whenever(settingsDao.getSettingsSync()).thenReturn(settings)

        // When
        val result = repository.getSettingsSync()

        // Then
        assertEquals(settings, result)
    }

    @Test
    fun `getSettingsSync returns default settings when dao returns null`() = runTest {
        // Given
        whenever(settingsDao.getSettingsSync()).thenReturn(null)

        // When
        val result = repository.getSettingsSync()

        // Then
        assertEquals(defaultSettings, result)
    }

    @Test
    fun `updateSettings calls dao upsert`() = runTest {
        // Given
        val newSettings = defaultSettings.copy(
            darkMode = true,
            gridSize = 6,
            enableMathChallenge = true
        )

        // When
        repository.updateSettings(newSettings)

        // Then
        verify(settingsDao).upsertSettings(newSettings)
    }

    @Test
    fun `updateDarkMode toggles dark mode setting`() = runTest {
        // Given
        val currentSettings = defaultSettings.copy(darkMode = false)
        whenever(settingsDao.getSettingsSync()).thenReturn(currentSettings)

        // When
        repository.updateDarkMode(true)

        // Then
        verify(settingsDao).upsertSettings(argThat { settings ->
            settings.darkMode == true
        })
    }

    @Test
    fun `updateGridSize validates and updates grid size`() = runTest {
        // Given
        val currentSettings = defaultSettings.copy(gridSize = 4)
        whenever(settingsDao.getSettingsSync()).thenReturn(currentSettings)

        // When
        repository.updateGridSize(5)

        // Then
        verify(settingsDao).upsertSettings(argThat { settings ->
            settings.gridSize == 5
        })
    }

    @Test
    fun `updateGridSize clamps grid size to valid range`() = runTest {
        // Given
        val currentSettings = defaultSettings.copy(gridSize = 4)
        whenever(settingsDao.getSettingsSync()).thenReturn(currentSettings)

        // When - Test upper bound
        repository.updateGridSize(10)

        // Then
        verify(settingsDao).upsertSettings(argThat { settings ->
            settings.gridSize == 8 // Should be clamped to max
        })

        // When - Test lower bound
        repository.updateGridSize(1)

        // Then
        verify(settingsDao, times(2)).upsertSettings(argThat { settings ->
            settings.gridSize == 2 // Should be clamped to min
        })
    }

    @Test
    fun `updateShowAppCounts updates app count visibility`() = runTest {
        // Given
        val currentSettings = defaultSettings.copy(showAppCounts = true)
        whenever(settingsDao.getSettingsSync()).thenReturn(currentSettings)

        // When
        repository.updateShowAppCounts(false)

        // Then
        verify(settingsDao).upsertSettings(argThat { settings ->
            settings.showAppCounts == false
        })
    }

    @Test
    fun `updateAutoHideNavigationBar updates navigation bar setting`() = runTest {
        // Given
        val currentSettings = defaultSettings.copy(autoHideNavigationBar = false)
        whenever(settingsDao.getSettingsSync()).thenReturn(currentSettings)

        // When
        repository.updateAutoHideNavigationBar(true)

        // Then
        verify(settingsDao).upsertSettings(argThat { settings ->
            settings.autoHideNavigationBar == true
        })
    }

    @Test
    fun `updateShowRecentApps updates recent apps visibility`() = runTest {
        // Given
        val currentSettings = defaultSettings.copy(showRecentApps = true)
        whenever(settingsDao.getSettingsSync()).thenReturn(currentSettings)

        // When
        repository.updateShowRecentApps(false)

        // Then
        verify(settingsDao).upsertSettings(argThat { settings ->
            settings.showRecentApps == false
        })
    }

    @Test
    fun `updateEnableTimeLimitPrompt updates time limit prompt setting`() = runTest {
        // Given
        val currentSettings = defaultSettings.copy(enableTimeLimitPrompt = false)
        whenever(settingsDao.getSettingsSync()).thenReturn(currentSettings)

        // When
        repository.updateEnableTimeLimitPrompt(true)

        // Then
        verify(settingsDao).upsertSettings(argThat { settings ->
            settings.enableTimeLimitPrompt == true
        })
    }

    @Test
    fun `updateEnableMathChallenge updates math challenge setting`() = runTest {
        // Given
        val currentSettings = defaultSettings.copy(enableMathChallenge = false)
        whenever(settingsDao.getSettingsSync()).thenReturn(currentSettings)

        // When
        repository.updateEnableMathChallenge(true)

        // Then
        verify(settingsDao).upsertSettings(argThat { settings ->
            settings.enableMathChallenge == true
        })
    }

    @Test
    fun `updateMathDifficulty updates math difficulty with valid values`() = runTest {
        // Given
        val currentSettings = defaultSettings.copy(mathDifficulty = "easy")
        whenever(settingsDao.getSettingsSync()).thenReturn(currentSettings)

        // When - Test valid difficulties
        repository.updateMathDifficulty("medium")
        verify(settingsDao).upsertSettings(argThat { settings ->
            settings.mathDifficulty == "medium"
        })

        repository.updateMathDifficulty("hard")
        verify(settingsDao, times(2)).upsertSettings(argThat { settings ->
            settings.mathDifficulty == "hard"
        })
    }

    @Test
    fun `updateMathDifficulty defaults to easy for invalid values`() = runTest {
        // Given
        val currentSettings = defaultSettings.copy(mathDifficulty = "easy")
        whenever(settingsDao.getSettingsSync()).thenReturn(currentSettings)

        // When - Test invalid difficulty
        repository.updateMathDifficulty("invalid")

        // Then - Should default to "easy"
        verify(settingsDao).upsertSettings(argThat { settings ->
            settings.mathDifficulty == "easy"
        })
    }

    @Test
    fun `updateSessionExpiryCountdown validates and clamps countdown seconds`() = runTest {
        // Given
        val currentSettings = defaultSettings.copy(sessionExpiryCountdownSeconds = 5)
        whenever(settingsDao.getSettingsSync()).thenReturn(currentSettings)

        // When - Test valid value
        repository.updateSessionExpiryCountdown(10)

        // Then
        verify(settingsDao).upsertSettings(argThat { settings ->
            settings.sessionExpiryCountdownSeconds == 10
        })

        // When - Test upper bound clamping
        repository.updateSessionExpiryCountdown(50)

        // Then - Should be clamped to 30
        verify(settingsDao, times(2)).upsertSettings(argThat { settings ->
            settings.sessionExpiryCountdownSeconds == 30
        })

        // When - Test lower bound clamping
        repository.updateSessionExpiryCountdown(-5)

        // Then - Should be clamped to 0
        verify(settingsDao, times(3)).upsertSettings(argThat { settings ->
            settings.sessionExpiryCountdownSeconds == 0
        })
    }

    @Test
    fun `updateRecentAppsLimit validates and clamps recent apps limit`() = runTest {
        // Given
        val currentSettings = defaultSettings.copy(recentAppsLimit = 10)
        whenever(settingsDao.getSettingsSync()).thenReturn(currentSettings)

        // When - Test valid value
        repository.updateRecentAppsLimit(20)

        // Then
        verify(settingsDao).upsertSettings(argThat { settings ->
            settings.recentAppsLimit == 20
        })

        // When - Test upper bound clamping
        repository.updateRecentAppsLimit(100)

        // Then - Should be clamped to 50
        verify(settingsDao, times(2)).upsertSettings(argThat { settings ->
            settings.recentAppsLimit == 50
        })

        // When - Test lower bound clamping
        repository.updateRecentAppsLimit(0)

        // Then - Should be clamped to 1
        verify(settingsDao, times(3)).upsertSettings(argThat { settings ->
            settings.recentAppsLimit == 1
        })
    }

    @Test
    fun `multiple setting updates preserve other settings`() = runTest {
        // Given
        val currentSettings = defaultSettings.copy(
            darkMode = false,
            gridSize = 4,
            enableMathChallenge = false
        )
        whenever(settingsDao.getSettingsSync()).thenReturn(currentSettings)

        // When - Update dark mode
        repository.updateDarkMode(true)

        // Then - Other settings should be preserved
        verify(settingsDao).upsertSettings(argThat { settings ->
            settings.darkMode == true &&
            settings.gridSize == 4 &&
            settings.enableMathChallenge == false &&
            settings.mathDifficulty == "easy"
        })

        // When - Update grid size
        repository.updateGridSize(6)

        // Then - Previous dark mode change should be preserved
        verify(settingsDao, times(2)).upsertSettings(argThat { settings ->
            settings.gridSize == 6 &&
            settings.enableMathChallenge == false &&
            settings.mathDifficulty == "easy"
        })
    }
}