package com.example.farmforward.utils.weatherUtils

object WeatherCache {
    private val cache = mutableMapOf<String, Pair<Long, String>>()

    private const val CACHE_DURATION = 3 * 60 * 60 * 1000L

    fun getValidWeather(key: String): String? {
        val data = cache[key] ?: return null
        val (timestamp, weather) = data

        return if (System.currentTimeMillis() - timestamp < CACHE_DURATION) {
            weather
        } else {
            cache.remove(key)
            null
        }
    }

    fun saveWeather(key: String, weather: String) {
        cache[key] = Pair(System.currentTimeMillis(), weather)
    }

    fun clear() {
        cache.clear()
    }
}