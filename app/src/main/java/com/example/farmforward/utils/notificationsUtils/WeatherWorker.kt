package com.example.farmforward.utils.notificationsUtils

import android.content.Context
import androidx.hilt.work.HiltWorker // Correct import for Workers
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.farmforward.utils.weatherUtils.WeatherRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class WeatherWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val weatherRepository: WeatherRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val condition = weatherRepository.getLatestForecastCondition()

            val message = when {
                condition.contains("Rain", ignoreCase = true) ->
                    "🌧️ Rain expected. Postpone harvest and spraying."
                condition.contains("Clear", ignoreCase = true) ->
                    "☀️ Clear skies! Perfect time for harvest or irrigation."
                condition.contains("Clouds", ignoreCase = true) ->
                    "☁️ Cloudy today. Good day for field maintenance."
                else -> "Farm Weather Update: $condition today."
            }
            NotificationHelper.sendNotification(
                context,
                "Farm Weather Alert",
                message,
                1001
            )

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}