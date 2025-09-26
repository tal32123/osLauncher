package com.talauncher.data.model

import java.util.Locale

enum class WeatherDisplayOption(val storageValue: String, val label: String) {
    OFF("off", "Off"),
    DAILY("daily", "Daily"),
    HOURLY("hourly", "Hourly");

    companion object {
        fun fromStorageValue(value: String?): WeatherDisplayOption {
            val normalized = value?.lowercase(Locale.US)
            return entries.firstOrNull { it.storageValue == normalized } ?: DAILY
        }
    }
}

enum class WeatherTemperatureUnit(val storageValue: String, val symbol: String) {
    CELSIUS("celsius", "C"),
    FAHRENHEIT("fahrenheit", "F");

    companion object {
        fun fromStorageValue(value: String?): WeatherTemperatureUnit {
            val normalized = value?.lowercase(Locale.US)
            return entries.firstOrNull { it.storageValue == normalized } ?: CELSIUS
        }
    }
}

enum class MathDifficulty(val storageValue: String, val label: String) {
    EASY("easy", "Easy"),
    MEDIUM("medium", "Medium"),
    HARD("hard", "Hard");

    companion object {
        fun fromStorageValue(value: String?): MathDifficulty {
            val normalized = value?.lowercase(Locale.US)
            return entries.firstOrNull { it.storageValue == normalized } ?: EASY
        }
    }
}

enum class ColorPaletteOption(val storageValue: String, val label: String) {
    DEFAULT("default", "Default"),
    WARM("warm", "Warm"),
    COOL("cool", "Cool"),
    MONOCHROME("monochrome", "Mono"),
    NATURE("nature", "Nature");

    companion object {
        fun fromStorageValue(value: String?): ColorPaletteOption {
            val normalized = value?.lowercase(Locale.US)
            return entries.firstOrNull { it.storageValue == normalized } ?: DEFAULT
        }
    }
}

enum class UiDensityOption(val storageValue: String, val label: String) {
    COMPACT("compact", "Compact"),
    COMFORTABLE("comfortable", "Comfortable"),
    SPACIOUS("spacious", "Spacious");

    companion object {
        fun fromStorageValue(value: String?): UiDensityOption {
            val normalized = value?.lowercase(Locale.US)
            return entries.firstOrNull { it.storageValue == normalized } ?: COMFORTABLE
        }
    }
}
