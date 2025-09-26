package com.talauncher.data.database

import androidx.room.TypeConverter
import com.talauncher.data.model.ColorPaletteOption
import com.talauncher.data.model.MathDifficulty
import com.talauncher.data.model.UiDensityOption
import com.talauncher.data.model.WeatherDisplayOption
import com.talauncher.data.model.WeatherTemperatureUnit

class SettingsTypeConverters {
    @TypeConverter
    fun toWeatherDisplay(value: String?): WeatherDisplayOption =
        WeatherDisplayOption.fromStorageValue(value)

    @TypeConverter
    fun fromWeatherDisplay(option: WeatherDisplayOption): String = option.storageValue

    @TypeConverter
    fun toWeatherTemperatureUnit(value: String?): WeatherTemperatureUnit =
        WeatherTemperatureUnit.fromStorageValue(value)

    @TypeConverter
    fun fromWeatherTemperatureUnit(option: WeatherTemperatureUnit): String = option.storageValue

    @TypeConverter
    fun toMathDifficulty(value: String?): MathDifficulty =
        MathDifficulty.fromStorageValue(value)

    @TypeConverter
    fun fromMathDifficulty(option: MathDifficulty): String = option.storageValue

    @TypeConverter
    fun toColorPalette(value: String?): ColorPaletteOption =
        ColorPaletteOption.fromStorageValue(value)

    @TypeConverter
    fun fromColorPalette(option: ColorPaletteOption): String = option.storageValue

    @TypeConverter
    fun toUiDensity(value: String?): UiDensityOption =
        UiDensityOption.fromStorageValue(value)

    @TypeConverter
    fun fromUiDensity(option: UiDensityOption): String = option.storageValue
}
