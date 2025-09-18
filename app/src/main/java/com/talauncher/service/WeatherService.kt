package com.talauncher.service

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.util.Log
import com.talauncher.data.model.WeatherData
import com.talauncher.data.model.WeatherHourly
import com.talauncher.data.model.WeatherDaily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class WeatherService(private val context: Context) {

    suspend fun getCurrentWeather(lat: Double, lon: Double): Result<WeatherData> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code,is_day&timezone=auto"
                val response = makeHttpRequest(url)
                val json = JSONObject(response)

                val current = json.getJSONObject("current")
                val temperature = current.getDouble("temperature_2m")
                val weatherCode = current.getInt("weather_code")
                val isDay = current.getInt("is_day") == 1

                Result.success(WeatherData(
                    temperature = temperature,
                    weatherCode = weatherCode,
                    isDay = isDay
                ))
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching current weather", e)
                Result.failure(e)
            }
        }
    }

    suspend fun getHourlyWeather(lat: Double, lon: Double): Result<List<WeatherHourly>> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&hourly=temperature_2m,weather_code,is_day&timezone=auto&forecast_days=1"
                val response = makeHttpRequest(url)
                val json = JSONObject(response)

                val hourly = json.getJSONObject("hourly")
                val times = hourly.getJSONArray("time")
                val temperatures = hourly.getJSONArray("temperature_2m")
                val weatherCodes = hourly.getJSONArray("weather_code")
                val isDayArray = hourly.getJSONArray("is_day")

                val hourlyData = mutableListOf<WeatherHourly>()
                for (i in 0 until times.length()) {
                    hourlyData.add(WeatherHourly(
                        temperature = temperatures.getDouble(i),
                        weatherCode = weatherCodes.getInt(i),
                        time = times.getString(i),
                        isDay = isDayArray.getInt(i) == 1
                    ))
                }

                Result.success(hourlyData)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching hourly weather", e)
                Result.failure(e)
            }
        }
    }

    suspend fun getDailyWeather(lat: Double, lon: Double): Result<List<WeatherDaily>> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&daily=temperature_2m_max,temperature_2m_min,weather_code&timezone=auto&forecast_days=7"
                val response = makeHttpRequest(url)
                val json = JSONObject(response)

                val daily = json.getJSONObject("daily")
                val dates = daily.getJSONArray("time")
                val tempMax = daily.getJSONArray("temperature_2m_max")
                val tempMin = daily.getJSONArray("temperature_2m_min")
                val weatherCodes = daily.getJSONArray("weather_code")

                val dailyData = mutableListOf<WeatherDaily>()
                for (i in 0 until dates.length()) {
                    dailyData.add(WeatherDaily(
                        temperatureMax = tempMax.getDouble(i),
                        temperatureMin = tempMin.getDouble(i),
                        weatherCode = weatherCodes.getInt(i),
                        date = dates.getString(i)
                    ))
                }

                Result.success(dailyData)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching daily weather", e)
                Result.failure(e)
            }
        }
    }

    fun getCurrentLocation(): Pair<Double, Double>? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        return try {
            val providers = locationManager.getProviders(true)
            var bestLocation: Location? = null

            for (provider in providers) {
                try {
                    val location = locationManager.getLastKnownLocation(provider)
                    if (location != null) {
                        if (bestLocation == null || location.accuracy < bestLocation.accuracy) {
                            bestLocation = location
                        }
                    }
                } catch (e: SecurityException) {
                    Log.w(TAG, "No permission for location provider: $provider")
                }
            }

            bestLocation?.let { Pair(it.latitude, it.longitude) }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current location", e)
            null
        }
    }

    private fun makeHttpRequest(urlString: String): String {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection

        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                response.toString()
            } else {
                throw Exception("HTTP error code: $responseCode")
            }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val TAG = "WeatherService"
    }
}