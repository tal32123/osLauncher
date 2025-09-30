package com.talauncher

import com.talauncher.data.database.SettingsDao
import com.talauncher.data.model.ColorPaletteOption
import com.talauncher.data.model.AppIconStyleOption
import com.talauncher.data.model.LauncherSettings
import com.talauncher.data.model.UiDensityOption
import com.talauncher.data.repository.SettingsRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.junit.Assert.assertEquals

@RunWith(RobolectricTestRunner::class)
class SettingsRepositoryTest {

    @Mock
    private lateinit var settingsDao: SettingsDao

    private lateinit var repository: SettingsRepository

    private val defaultSettings = LauncherSettings()

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        repository = SettingsRepository(settingsDao)
    }

    @Test
    fun `getSettings returns settings from dao`() = runTest {
        println("Running getSettings returns settings from dao test")
        val settings = defaultSettings.copy(backgroundColor = "black")
        whenever(settingsDao.getSettings()).thenReturn(flowOf(settings))

        val result = repository.getSettings().first()

        assertEquals(settings, result)
    }

    @Test
    fun `getSettingsSync returns settings synchronously`() = runTest {
        println("Running getSettingsSync returns settings synchronously test")
        whenever(settingsDao.getSettingsSync()).thenReturn(defaultSettings)

        val result = repository.getSettingsSync()

        assertEquals(defaultSettings, result)
    }

    @Test
    fun `getSettingsSync returns default settings when dao returns null`() = runTest {
        println("Running getSettingsSync returns default settings when dao returns null test")
        whenever(settingsDao.getSettingsSync()).thenReturn(null)

        val result = repository.getSettingsSync()

        assertEquals(LauncherSettings(), result)
        verify(settingsDao).insertSettings(any())
    }

    @Test
    fun `updateSettings calls dao update`() = runTest {
        println("Running updateSettings calls dao update test")
        val newSettings = defaultSettings.copy(enableMathChallenge = true)

        repository.updateSettings(newSettings)

        verify(settingsDao).updateSettings(newSettings)
    }

    @Test
    fun `updateBackgroundColor normalizes value`() = runTest {
        println("Running updateBackgroundColor normalizes value test")
        whenever(settingsDao.getSettingsSync()).thenReturn(defaultSettings)

        repository.updateBackgroundColor("white")

        verify(settingsDao).updateSettings(check { assertEquals("white", it.backgroundColor) })
    }

    @Test
    fun `updateShowWallpaper updates wallpaper visibility`() = runTest {
        println("Running updateShowWallpaper updates wallpaper visibility test")
        whenever(settingsDao.getSettingsSync()).thenReturn(defaultSettings)

        repository.updateShowWallpaper(false)

        verify(settingsDao).updateSettings(check { assertEquals(false, it.showWallpaper) })
    }

    @Test
    fun `updateColorPalette updates color palette`() = runTest {
        println("Running updateColorPalette updates color palette test")
        whenever(settingsDao.getSettingsSync()).thenReturn(defaultSettings)

        repository.updateColorPalette(ColorPaletteOption.WARM)

        verify(settingsDao).updateSettings(check { assertEquals(ColorPaletteOption.WARM, it.colorPalette) })
    }

    @Test
    fun `updateAppIconStyle updates icon style`() = runTest {
        println("Running updateAppIconStyle updates icon style test")
        whenever(settingsDao.getSettingsSync()).thenReturn(defaultSettings)

        repository.updateAppIconStyle(AppIconStyleOption.ORIGINAL)

        verify(settingsDao).updateSettings(check { assertEquals(AppIconStyleOption.ORIGINAL, it.appIconStyle) })
    }

    @Test
    fun `setCustomPalette updates palette and custom colors together`() = runTest {
        println("Running setCustomPalette updates palette and custom colors together test")
        whenever(settingsDao.getSettingsSync()).thenReturn(defaultSettings)

        repository.setCustomPalette(
            palette = ColorPaletteOption.CUSTOM,
            customColorOption = "Purple",
            customPrimaryColor = "#123456",
            customSecondaryColor = "#654321"
        )

        verify(settingsDao).updateSettings(
            check {
                assertEquals(ColorPaletteOption.CUSTOM, it.colorPalette)
                assertEquals("Purple", it.customColorOption)
                assertEquals("#123456", it.customPrimaryColor)
                assertEquals("#654321", it.customSecondaryColor)
            }
        )
    }

    @Test
    fun `updateWallpaperBlurAmount clamps value`() = runTest {
        println("Running updateWallpaperBlurAmount clamps value test")
        whenever(settingsDao.getSettingsSync()).thenReturn(defaultSettings)

        repository.updateWallpaperBlurAmount(1.5f)

        verify(settingsDao).updateSettings(check { assertEquals(1.0f, it.wallpaperBlurAmount) })
    }

    @Test
    fun `updateGlassmorphism updates glassmorphism`() = runTest {
        println("Running updateGlassmorphism updates glassmorphism test")
        whenever(settingsDao.getSettingsSync()).thenReturn(defaultSettings)

        repository.updateGlassmorphism(true)

        verify(settingsDao).updateSettings(check { assertEquals(true, it.enableGlassmorphism) })
    }

    @Test
    fun `updateUiDensity updates ui density`() = runTest {
        println("Running updateUiDensity updates ui density test")
        whenever(settingsDao.getSettingsSync()).thenReturn(defaultSettings)

        repository.updateUiDensity(UiDensityOption.COMPACT)

        verify(settingsDao).updateSettings(check { assertEquals(UiDensityOption.COMPACT, it.uiDensity) })
    }

    @Test
    fun `updateAnimationsEnabled updates animations`() = runTest {
        println("Running updateAnimationsEnabled updates animations test")
        whenever(settingsDao.getSettingsSync()).thenReturn(defaultSettings)

        repository.updateAnimationsEnabled(false)

        verify(settingsDao).updateSettings(check { assertEquals(false, it.enableAnimations) })
    }
}
