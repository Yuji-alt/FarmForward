package com.example.farmforward.fragmentController

import android.content.Context
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.farmforward.R
import com.example.farmforward.utils.ForecastItem

class WeatherController(private val context: Context, private val container: LinearLayout) {

    fun displayForecast(forecasts: List<ForecastItem>) {
        container.removeAllViews()
        val inflater = LayoutInflater.from(context)
        val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        inputFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val outputFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())

        for (forecast in forecasts) {
            val itemView = inflater.inflate(R.layout.item_weather_forecast, container, false)
            val timeText = itemView.findViewById<TextView>(R.id.timeText)
            val tempText = itemView.findViewById<TextView>(R.id.tempText)
            val descText = itemView.findViewById<TextView>(R.id.descText)
            val iconView = itemView.findViewById<ImageView>(R.id.weatherIcon)
            val temp = forecast.main.temp
            val desc = forecast.weather.firstOrNull()?.description ?: "clear sky"

            var time: String
            try {
                val date = inputFormat.parse(forecast.dt_txt)
                time = outputFormat.format(date)
            } catch (e: Exception) {
                time = "00:00"
            }

            timeText.text = time
            tempText.text = "${temp}°C"
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