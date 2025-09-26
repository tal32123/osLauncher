package com.talauncher.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.talauncher.data.model.WeatherCondition
import com.talauncher.data.model.WeatherData
import com.talauncher.data.model.WeatherTemperatureUnit
import kotlin.math.roundToInt

@Composable
fun WeatherDisplay(
    weatherData: WeatherData?,
    modifier: Modifier = Modifier,
    showTemperature: Boolean = true,
    temperatureUnit: WeatherTemperatureUnit = WeatherTemperatureUnit.CELSIUS,
    dailyHigh: Double? = null,
    dailyLow: Double? = null
) {
    if (weatherData != null) {
        val convertedTemperature = convertTemperature(weatherData.temperature, temperatureUnit)
        val unitSuffix = getTemperatureSuffix(temperatureUnit)
        val showDailyRange = dailyHigh != null && dailyLow != null
        val convertedHigh = dailyHigh?.let { convertTemperature(it, temperatureUnit).roundToInt() }
        val convertedLow = dailyLow?.let { convertTemperature(it, temperatureUnit).roundToInt() }
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Weather icon (black and white)
            Text(
                text = WeatherCondition.getSimpleIcon(weatherData.weatherCode, weatherData.isDay),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (showDailyRange && convertedHigh != null && convertedLow != null) {
                Text(
                    text = "H ${convertedHigh}°$unitSuffix  L ${convertedLow}°$unitSuffix",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            } else if (showTemperature) {
                Text(
                    text = "${convertedTemperature.roundToInt()}°$unitSuffix",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
fun WeatherIcon(
    weatherCode: Int,
    isDay: Boolean = true,
    modifier: Modifier = Modifier
) {
    Text(
        text = WeatherCondition.getSimpleIcon(weatherCode, isDay),
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier
    )
}

private fun convertTemperature(temperatureCelsius: Double, unit: WeatherTemperatureUnit): Double {
    return when (unit) {
        WeatherTemperatureUnit.FAHRENHEIT -> (temperatureCelsius * 9 / 5) + 32
        WeatherTemperatureUnit.CELSIUS -> temperatureCelsius
    }
}

private fun getTemperatureSuffix(unit: WeatherTemperatureUnit): String {
    return when (unit) {
        WeatherTemperatureUnit.FAHRENHEIT -> "F"
        WeatherTemperatureUnit.CELSIUS -> "C"
    }
}
