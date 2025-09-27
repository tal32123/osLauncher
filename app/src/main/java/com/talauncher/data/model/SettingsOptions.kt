package com.talauncher.data.model

import java.util.Locale

enum class WeatherDisplayOption(val label: String) {
    OFF("Off"),
    DAILY("Daily"),
    HOURLY("Hourly");

    val storageValue: String
        get() = name

    companion object {
        fun fromStorageValue(value: String?): WeatherDisplayOption {
            val normalized = value?.lowercase(Locale.US)
            return entries.firstOrNull { it.name.lowercase(Locale.US) == normalized } ?: DAILY
        }
    }
}

enum class WeatherTemperatureUnit(val symbol: String) {
    CELSIUS("C"),
    FAHRENHEIT("F");

    val storageValue: String
        get() = name

    companion object {
        fun fromStorageValue(value: String?): WeatherTemperatureUnit {
            val normalized = value?.lowercase(Locale.US)
            return entries.firstOrNull { it.name.lowercase(Locale.US) == normalized } ?: CELSIUS
        }
    }
}

enum class MathDifficulty(val label: String) {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard");

    val storageValue: String
        get() = name

    companion object {
        fun fromStorageValue(value: String?): MathDifficulty {
            val normalized = value?.lowercase(Locale.US)
            return entries.firstOrNull { it.name.lowercase(Locale.US) == normalized } ?: EASY
        }
    }
}

enum class ColorPaletteOption(val label: String) {
    DEFAULT("Default"),
    WARM("Warm"),
    COOL("Cool"),
    MONOCHROME("Mono"),
    NATURE("Nature"),
    OCEANIC("Oceanic"),
    SUNSET("Sunset"),
    FOREST("Forest"),
    LAVENDER("Lavender"),
    CHERRY("Cherry");

    val storageValue: String
        get() = name

    companion object {
        fun fromStorageValue(value: String?): ColorPaletteOption {
            val normalized = value?.lowercase(Locale.US)
            return entries.firstOrNull { it.name.lowercase(Locale.US) == normalized } ?: DEFAULT
        }
    }
}

enum class ThemeModeOption(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark");

    val storageValue: String
        get() = name

    companion object {
        fun fromStorageValue(value: String?): ThemeModeOption {
            val normalized = value?.lowercase(Locale.US)
            return entries.firstOrNull { it.name.lowercase(Locale.US) == normalized } ?: SYSTEM
        }
    }
}

enum class UiDensityOption(val label: String) {
    COMPACT("Compact"),
    COMFORTABLE("Comfortable"),
    SPACIOUS("Spacious");

    val storageValue: String
        get() = name

    companion object {
        fun fromStorageValue(value: String?): UiDensityOption {
            val normalized = value?.lowercase(Locale.US)
            return entries.firstOrNull { it.name.lowercase(Locale.US) == normalized } ?: COMFORTABLE
        }
    }
}
