package com.example.farmforward.utils.weatherUtils

import android.content.Context
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.farmforward.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WeatherController(private val context: Context, private val container: LinearLayout) {

    fun displayForecast(forecasts: List<ForecastItem>) {
        container.removeAllViews()
        val inflater = LayoutInflater.from(context)

        val outputFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

        // Helpers for Date comparison
        val nowCal = Calendar.getInstance()
        val itemCal = Calendar.getInstance()

        for (forecast in forecasts) {
            val itemView = inflater.inflate(R.layout.item_weather_forecast, container, false)

            // 1. Bind Views (Including your new ID)
            val dayLabelText = itemView.findViewById<TextView>(R.id.todayORtom) // NEW
            val timeText = itemView.findViewById<TextView>(R.id.timeText)
            val tempText = itemView.findViewById<TextView>(R.id.tempText)
            val descText = itemView.findViewById<TextView>(R.id.descText)
            val iconView = itemView.findViewById<ImageView>(R.id.weatherIcon)

            // 2. Extract Data
            val temp = forecast.main.temp
            val desc = forecast.weather.firstOrNull()?.description ?: "clear"
            val date = Date(forecast.dt * 1000)
            val time = outputFormat.format(date)

            // 3. Logic: Today vs Tomorrow
            itemCal.time = date
            val isToday = nowCal.get(Calendar.DAY_OF_YEAR) == itemCal.get(Calendar.DAY_OF_YEAR) &&
                    nowCal.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR)

            // 4. Set Texts
            dayLabelText.text = if (isToday) "Today" else "Tom"
            timeText.text = time
            tempText.text = "${temp.toInt()}°C"
            descText.text = desc

            // 5. Set Icon
            val drawableId = getDrawableForWeather(desc)
            iconView.setImageResource(drawableId)
            val color = androidx.core.content.ContextCompat.getColor(itemView.context, R.color.cafenoir)
            iconView.setColorFilter(color)

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