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

enum class ColorPaletteOption(val label: String) {
    DEFAULT("Default"),
    WARM("Warm"),
    COOL("Cool"),
    BLACK_AND_WHITE("Black & White"),
    NATURE("Nature"),
    OCEANIC("Oceanic"),
    SUNSET("Sunset"),
    LAVENDER("Lavender"),
    CHERRY("Cherry"),
    CUSTOM("Custom");

    val storageValue: String
        get() = name

    companion object {
        fun fromStorageValue(value: String?): ColorPaletteOption {
            val normalized = value?.lowercase(Locale.US)
            return entries.firstOrNull { it.name.lowercase(Locale.US) == normalized } ?: DEFAULT
        }
    }
}

enum class AppIconStyleOption(val label: String) {
    ORIGINAL("Original colors"),
    BLACK_AND_WHITE("Monochrome"),
    HIDDEN("No icons");

    val storageValue: String
        get() = name

    companion object {
        fun fromStorageValue(value: String?): AppIconStyleOption {
            val normalized = value?.lowercase(Locale.US)
            return entries.firstOrNull { it.name.lowercase(Locale.US) == normalized } ?: ORIGINAL
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
            return entries.firstOrNull { it.name.lowercase(Locale.US) == normalized } ?: COMPACT
        }
    }
}

enum class AppSectionLayoutOption(val label: String, val columns: Int) {
    LIST("List (1 per row)", 1),
    GRID_3("Grid (3 per row)", 3),
    GRID_4("Grid (4 per row)", 4);

    val storageValue: String
        get() = name

    companion object {
        fun fromStorageValue(value: String?): AppSectionLayoutOption {
            val normalized = value?.lowercase(Locale.US)
            return entries.firstOrNull { it.name.lowercase(Locale.US) == normalized } ?: LIST
        }
    }
}

enum class AppDisplayStyleOption(val label: String) {
    ICON_ONLY("Icon only"),
    ICON_AND_TEXT("Icon and text"),
    TEXT_ONLY("Text only");

    val storageValue: String
        get() = name

    companion object {
        fun fromStorageValue(value: String?): AppDisplayStyleOption {
            val normalized = value?.lowercase(Locale.US)
            return entries.firstOrNull { it.name.lowercase(Locale.US) == normalized } ?: ICON_AND_TEXT
        }
    }
}

enum class IconColorOption(val label: String) {
    ORIGINAL("Original colors"),
    MONOCHROME("Monochrome");

    val storageValue: String
        get() = name

    companion object {
        fun fromStorageValue(value: String?): IconColorOption {
            val normalized = value?.lowercase(Locale.US)
            return entries.firstOrNull { it.name.lowercase(Locale.US) == normalized } ?: ORIGINAL
        }
    }
}

// News related options
enum class NewsRefreshInterval(val label: String) {
    DAILY("Daily"),
    HOURLY("Hourly");

    val storageValue: String
        get() = name

    companion object {
        fun fromStorageValue(value: String?): NewsRefreshInterval {
            val normalized = value?.lowercase(Locale.US)
            return entries.firstOrNull { it.name.lowercase(Locale.US) == normalized } ?: DAILY
        }
    }
}

enum class NewsCategory(val label: String, val wsjPath: String) {
    WORLD("World", "/news/world"),
    US("U.S.", "/news/us"),
    POLITICS("Politics", "/news/politics"),
    ECONOMY("Economy", "/news/economy"),
    BUSINESS("Business", "/news/business"),
    TECH("Technology", "/news/technology"),
    MARKETS("Markets", "/news/markets"),
    OPINION("Opinion", "/news/opinion");

    val storageValue: String
        get() = name

    companion object {
        fun fromStorageValue(value: String?): NewsCategory? {
            val normalized = value?.lowercase(Locale.US)
            return entries.firstOrNull { it.name.lowercase(Locale.US) == normalized }
        }
    }
}
