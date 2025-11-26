package com.example.farmforward.utils.weatherUtils

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    val list: List<ForecastItem>
)

data class ForecastItem(
    @SerializedName("dt") val dt: Long,
    @SerializedName("main") val mainStats: MainStats,
    @SerializedName("weather") val weather: List<Weather>
)

data class MainStats(
    val temp: Double,
    val humidity: Int
)

data class Weather(
    @SerializedName("id") val id: Int,
    @SerializedName("main") val main: String,
    @SerializedName("description") val description: String,
    @SerializedName("icon") val icon: String
)