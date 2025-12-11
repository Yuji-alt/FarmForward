package com.example.farmforward.utils.notificationsUtils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.farmforward.BuildConfig
import com.example.farmforward.utils.otherUtils.RetrofitClient
import com.example.farmforward.utils.weatherUtils.WeatherRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltWorker
class WeatherWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val weatherRepository: WeatherRepository
) : CoroutineWorker(context, params) {

    private val TAG = "WeatherWorker"

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting Weather Worker...")
        return try {
            // Check Notification Permission (Android 13+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Notification Permission NOT granted. Stopping worker.")
                    return Result.failure() // Better to return failure or success depending on if you want retry
                }
            }

            var condition = "Unknown"
            val apiKey = BuildConfig.WEATHER_API_KEY
            var targetLat: Double? = null
            var targetLon: Double? = null

            // 1. Try to get LIVE location
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                try {
                    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                    // Use CancellationTokenSource if needed, passing null for now
                    val location = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
                    if (location != null) {
                        targetLat = location.latitude
                        targetLon = location.longitude
                        Log.d(TAG, "Acquired Live Location: $targetLat, $targetLon")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to get live location: ${e.message}")
                }
            }

            // 2. Fallback to SAVED location
            if (targetLat == null) {
                val savedCoords = weatherRepository.getSavedCoordinates()
                if (savedCoords != null) {
                    targetLat = savedCoords.first
                    targetLon = savedCoords.second
                    Log.d(TAG, "Using Saved Location: $targetLat, $targetLon")
                }
            }

            // 3. If we have coordinates, Fetch API (Using SUSPEND)
            if (targetLat != null && targetLon != null) {
                try {
                    // Call the new suspend function directly
                    val response = RetrofitClient.instance.getForecastByCoordinates(targetLat, targetLon, apiKey)

                    if (response.isSuccessful && response.body() != null) {
                        val allForecasts = response.body()?.list ?: emptyList()

                        if (allForecasts.isNotEmpty()) {
                            val now = System.currentTimeMillis()
                            // Filter for forecasts in the future (allowing 1 hour buffer)
                            val futureForecasts = allForecasts.filter { (it.dt * 1000L) >= (now - 3600000) }
                            val displayList = futureForecasts.take(9)

                            weatherRepository.saveWeatherData(displayList, "Current Location", getWeatherDay(), targetLat, targetLon)
                            condition = displayList.firstOrNull()?.weather?.firstOrNull()?.main ?: "Unknown"
                            Log.d(TAG, "API Success. Condition: $condition")
                        }
                    } else {
                        Log.e(TAG, "API Error: ${response.code()} ${response.message()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Network Exception: ${e.message}")
                    e.printStackTrace()
                }
            } else {
                Log.w(TAG, "No coordinates found (Live or Saved). Skipping API call.")
            }

            // 4. Final Fallback: Use cache if API failed
            if (condition == "Unknown") {
                Log.d(TAG, "Condition unknown. Checking cache...")
                weatherRepository.loadCachedData()
                val cached = weatherRepository.cachedForecasts

                if (!cached.isNullOrEmpty()) {
                    condition = cached[0].weather.firstOrNull()?.main ?: "Unknown"
                    Log.d(TAG, "Loaded from Cache: $condition")
                }
            }

            // 5. Send Notification
            if (condition != "Unknown") {
                val message = when {
                    condition.contains("Rain", ignoreCase = true) || condition.contains("Drizzle", ignoreCase = true) ->
                        "🌧️ Rain expected. Postpone harvest and spraying."
                    condition.contains("Thunderstorm", ignoreCase = true) ->
                        "⛈️ Storm alert! Secure loose equipment."
                    condition.contains("Clear", ignoreCase = true) || condition.contains("Sunny", ignoreCase = true) ->
                        "☀️ Clear skies! Perfect time for harvest."
                    condition.contains("Clouds", ignoreCase = true) ->
                        "☁️ Cloudy today. Good day for field maintenance."
                    condition.contains("Snow", ignoreCase = true) ->
                        "❄️ Snow expected. Protect sensitive crops."
                    else -> "Farm Weather Update: Expect $condition today."
                }
                NotificationHelper.sendNotification(context, "Farm Weather Alert", message, 2001)
            } else {
                Log.w(TAG, "Condition is still Unknown. No notification sent.")
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun getWeatherDay(): String {
        val format = SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault())
        return format.format(Date())
    }
}