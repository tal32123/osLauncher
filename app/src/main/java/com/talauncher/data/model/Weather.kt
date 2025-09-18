package com.talauncher.data.model

data class WeatherData(
    val temperature: Double,
    val weatherCode: Int,
    val isDay: Boolean,
    val location: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class WeatherHourly(
    val temperature: Double,
    val weatherCode: Int,
    val time: String,
    val isDay: Boolean
)

data class WeatherDaily(
    val temperatureMax: Double,
    val temperatureMin: Double,
    val weatherCode: Int,
    val date: String
)

enum class WeatherCondition(val code: Int, val description: String, val icon: String) {
    CLEAR_SKY(0, "Clear sky", "☀"),
    MAINLY_CLEAR(1, "Mainly clear", "🌤"),
    PARTLY_CLOUDY(2, "Partly cloudy", "⛅"),
    OVERCAST(3, "Overcast", "☁"),
    FOG(45, "Fog", "🌫"),
    DEPOSITING_RIME_FOG(48, "Depositing rime fog", "🌫"),
    LIGHT_DRIZZLE(51, "Light drizzle", "🌦"),
    MODERATE_DRIZZLE(53, "Moderate drizzle", "🌦"),
    DENSE_DRIZZLE(55, "Dense drizzle", "🌧"),
    LIGHT_FREEZING_DRIZZLE(56, "Light freezing drizzle", "🌨"),
    DENSE_FREEZING_DRIZZLE(57, "Dense freezing drizzle", "🌨"),
    SLIGHT_RAIN(61, "Slight rain", "🌧"),
    MODERATE_RAIN(63, "Moderate rain", "🌧"),
    HEAVY_RAIN(65, "Heavy rain", "🌧"),
    LIGHT_FREEZING_RAIN(66, "Light freezing rain", "🌨"),
    HEAVY_FREEZING_RAIN(67, "Heavy freezing rain", "🌨"),
    SLIGHT_SNOW_FALL(71, "Slight snow fall", "🌨"),
    MODERATE_SNOW_FALL(73, "Moderate snow fall", "❄"),
    HEAVY_SNOW_FALL(75, "Heavy snow fall", "❄"),
    SNOW_GRAINS(77, "Snow grains", "❄"),
    SLIGHT_RAIN_SHOWERS(80, "Slight rain showers", "🌦"),
    MODERATE_RAIN_SHOWERS(81, "Moderate rain showers", "🌧"),
    VIOLENT_RAIN_SHOWERS(82, "Violent rain showers", "⛈"),
    SLIGHT_SNOW_SHOWERS(85, "Slight snow showers", "🌨"),
    HEAVY_SNOW_SHOWERS(86, "Heavy snow showers", "❄"),
    THUNDERSTORM(95, "Thunderstorm", "⛈"),
    THUNDERSTORM_WITH_HAIL(96, "Thunderstorm with slight hail", "⛈"),
    THUNDERSTORM_WITH_HEAVY_HAIL(99, "Thunderstorm with heavy hail", "⛈");

    companion object {
        fun fromCode(code: Int): WeatherCondition {
            return values().find { it.code == code } ?: CLEAR_SKY
        }

        fun getSimpleIcon(code: Int, isDay: Boolean = true): String {
            return when (code) {
                0 -> if (isDay) "☀" else "🌙"
                1, 2 -> if (isDay) "⛅" else "☁"
                3 -> "☁"
                45, 48 -> "🌫"
                51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> "🌧"
                71, 73, 75, 77, 85, 86 -> "❄"
                95, 96, 99 -> "⛈"
                else -> if (isDay) "☀" else "🌙"
            }
        }
    }
}