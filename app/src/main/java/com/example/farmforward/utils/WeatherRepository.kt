package com.example.farmforward.utils

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

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    var cachedForecasts: List<ForecastItem>? = null
    var cachedLocationName: String? = null
    var cachedDateText: String? = null

    fun saveWeatherData(forecasts: List<ForecastItem>, location: String, dateText: String) {
        cachedForecasts = forecasts
        cachedLocationName = location
        cachedDateText = dateText

        val jsonForecasts = gson.toJson(forecasts)
        prefs.edit().apply {
            putString(KEY_FORECAST, jsonForecasts)
            putString(KEY_LOCATION, location)
            putString(KEY_DATE, dateText)
            apply()
        }
    }

    fun loadCachedData() {
        // If memory is already set, do nothing
        if (cachedForecasts != null) return

        val jsonForecasts = prefs.getString(KEY_FORECAST, null)
        cachedLocationName = prefs.getString(KEY_LOCATION, "Unknown")
        cachedDateText = prefs.getString(KEY_DATE, "")

        if (jsonForecasts != null) {
            val type = object : TypeToken<List<ForecastItem>>() {}.type
            cachedForecasts = gson.fromJson(jsonForecasts, type)
        }
    }
}