package com.example.farmforward.utils

data class WeatherResponse(
    val list: List<ForecastItem>
)

data class ForecastItem(
    val dt_txt: String,
    val main: MainInfo,
    val weather: List<WeatherInfo>
)

data class MainInfo(
    val temp: Double
)

data class WeatherInfo(
    val description: String
)
