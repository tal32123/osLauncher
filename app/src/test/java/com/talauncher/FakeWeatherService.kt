package com.talauncher

import android.content.Context
import com.talauncher.data.model.WeatherDaily
import com.talauncher.data.model.WeatherData
import com.talauncher.data.model.WeatherHourly
import com.talauncher.service.WeatherService

/**
 * Lightweight weather helper used in tests so they do not make real HTTP calls.
 */
class FakeWeatherService(context: Context) : WeatherService(context) {
    override suspend fun getCurrentWeather(lat: Double, lon: Double): Result<WeatherData> =
        Result.success(WeatherData(temperature = 0.0, weatherCode = 0, isDay = true))

    override suspend fun getHourlyWeather(lat: Double, lon: Double): Result<List<WeatherHourly>> =
        Result.success(emptyList())

    override suspend fun getDailyWeather(lat: Double, lon: Double): Result<List<WeatherDaily>> =
        Result.success(emptyList())

    override suspend fun getCurrentLocation(): Pair<Double, Double>? = null
}
