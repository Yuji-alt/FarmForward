package com.example.farmforward.utils

import android.content.Context
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.farmforward.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherController(private val context: Context, private val container: LinearLayout) {

    fun displayForecast(forecasts: List<ForecastItem>) {
        container.removeAllViews()
        val inflater = LayoutInflater.from(context)

        val outputFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

        for (forecast in forecasts) {
            val itemView = inflater.inflate(R.layout.item_weather_forecast, container, false)
            val timeText = itemView.findViewById<TextView>(R.id.timeText)
            val tempText = itemView.findViewById<TextView>(R.id.tempText)
            val descText = itemView.findViewById<TextView>(R.id.descText)
            val iconView = itemView.findViewById<ImageView>(R.id.weatherIcon)

            val temp = forecast.mainStats.temp
            val desc = forecast.weather.firstOrNull()?.description ?: "clear sky"

            val date = Date(forecast.dt * 1000)
            val time = outputFormat.format(date)
            // -------------------------------------------------------------

            timeText.text = time
            tempText.text = "${temp.toInt()}°C"
            descText.text = desc

            val drawableId = getDrawableForWeather(desc)
            iconView.setImageResource(drawableId)

            container.addView(itemView)
        }
    }

    private fun getDrawableForWeather(description: String): Int {
        val desc = description.lowercase()
        return when {
            desc.contains("thunderstorm") -> R.drawable.thunder_storm
            desc.contains("rain") -> R.drawable.rain
            desc.contains("cloud") -> R.drawable.few_clouds
            desc.contains("sky") -> R.drawable.clear_sky
            else -> R.drawable.clear_sky
        }
    }
}