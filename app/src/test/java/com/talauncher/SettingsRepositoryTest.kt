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

@RunWith(RobolectricTestRunner::class)
class SettingsRepositoryTest {

    @Mock
    private lateinit var settingsDao: SettingsDao

    private lateinit var repository: SettingsRepository

    private val defaultSettings = LauncherSettings()

    @Before
    fun setUp() = runTest {
        MockitoAnnotations.openMocks(this)
        repository = SettingsRepository(settingsDao)
        whenever(settingsDao.getSettingsSync()).thenReturn(defaultSettings)
    }

    @Test
    fun `getSettings returns settings from dao`() = runTest {
        val settings = defaultSettings.copy(backgroundColor = "black")
        whenever(settingsDao.getSettings()).thenReturn(flowOf(settings))

        val result = repository.getSettings().first()

        assertEquals(settings, result)
    }

    @Test
    fun `getSettingsSync returns settings synchronously`() = runTest {
        val settings = defaultSettings.copy(showWallpaper = false)
        whenever(settingsDao.getSettingsSync()).thenReturn(settings)

        val result = repository.getSettingsSync()

        assertEquals(settings, result)
    }

    @Test
    fun `getSettingsSync returns default settings when dao returns null`() = runTest {
        whenever(settingsDao.getSettingsSync()).thenReturn(null)

        val result = repository.getSettingsSync()

        assertEquals(LauncherSettings(), result)
        verify(settingsDao).insertSettings(any())
    }

    @Test
    fun `updateSettings calls dao update`() = runTest {
        val newSettings = defaultSettings.copy(
            enableMathChallenge = true
        )

        repository.updateSettings(newSettings)

        verify(settingsDao).updateSettings(newSettings)
    }

    @Test
    fun `updateBackgroundColor updates background color`() = runTest {
        repository.updateBackgroundColor("white")

        verify(settingsDao).updateSettings(any())
    }

    @Test
    fun `updateShowWallpaper updates wallpaper visibility`() = runTest {
        repository.updateShowWallpaper(false)

        verify(settingsDao).updateSettings(any())
    }

    @Test
    fun `updateColorPalette updates color palette`() = runTest {
        repository.updateColorPalette("warm")

        verify(settingsDao).updateSettings(any())
    }

    @Test
    fun `updateWallpaperBlurAmount updates blur amount`() = runTest {
        repository.updateWallpaperBlurAmount(0.5f)

        verify(settingsDao).updateSettings(any())
    }

    @Test
    fun `updateGlassmorphism updates glassmorphism`() = runTest {
        repository.updateGlassmorphism(true)

        verify(settingsDao).updateSettings(any())
    }

    @Test
    fun `updateUiDensity updates ui density`() = runTest {
        repository.updateUiDensity("compact")

        verify(settingsDao).updateSettings(any())
    }

    @Test
    fun `updateAnimationsEnabled updates animations`() = runTest {
        repository.updateAnimationsEnabled(false)

        verify(settingsDao).updateSettings(any())
    }
}