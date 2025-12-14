package com.example.farmforward.utils.weatherUtils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val PREFS_NAME = "farm_weather_cache"
    private val KEY_FORECAST = "cached_forecast_list"
    private val KEY_LOCATION = "cached_location"
    private val KEY_DATE = "cached_date_text"

    // NEW KEYS
    private val KEY_LAT = "cached_lat"
    private val KEY_LON = "cached_lon"

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val KEY_TIMESTAMP = "cached_timestamp_millis"
    private val gson = Gson()

    var cachedForecasts: List<ForecastItem>? = null
    var cachedLocationName: String? = null
    var cachedDateText: String? = null
    fun saveWeatherData(forecasts: List<ForecastItem>, location: String, dateText: String, lat: Double? = null, lon: Double? = null) {
        cachedForecasts = forecasts
        cachedLocationName = location
        cachedDateText = dateText

        val jsonForecasts = gson.toJson(forecasts)
        val editor = prefs.edit()

        editor.putString(KEY_FORECAST, jsonForecasts)
        editor.putString(KEY_LOCATION, location)
        editor.putString(KEY_DATE, dateText)
        editor.putLong(KEY_TIMESTAMP, System.currentTimeMillis())

        // Save Coordinates if provided
        if (lat != null && lon != null) {
            editor.putString(KEY_LAT, lat.toString())
            editor.putString(KEY_LON, lon.toString())
        }

        editor.apply()
    }

    fun loadCachedData() {
        if (cachedForecasts != null) return

        val jsonForecasts = prefs.getString(KEY_FORECAST, null)
        cachedLocationName = prefs.getString(KEY_LOCATION, "Unknown")
        cachedDateText = prefs.getString(KEY_DATE, "")

        if (jsonForecasts != null) {
            val type = object : TypeToken<List<ForecastItem>>() {}.type
            cachedForecasts = gson.fromJson(jsonForecasts, type)
        }
    }

    fun getSavedCoordinates(): Pair<Double, Double>? {
        val latStr = prefs.getString(KEY_LAT, null)
        val lonStr = prefs.getString(KEY_LON, null)

        if (latStr != null && lonStr != null) {
            return Pair(latStr.toDouble(), lonStr.toDouble())
        }
        return null
    }

    fun getLatestForecastCondition(): String {
        loadCachedData()
        val items = cachedForecasts
        if (items.isNullOrEmpty()) return "Unknown"
        return items[0].weather.firstOrNull()?.main ?: "Unknown"
    }
    fun getLastUpdateTimestamp(): Long {
        return prefs.getLong(KEY_TIMESTAMP, 0L)
    }
}