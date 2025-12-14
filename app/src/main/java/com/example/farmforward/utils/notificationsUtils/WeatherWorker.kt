package com.example.farmforward.utils.notificationsUtils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.farmforward.utils.weatherUtils.WeatherRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import androidx.core.content.edit

@HiltWorker
class WeatherWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val weatherRepository: WeatherRepository
) : CoroutineWorker(context, params) {

    private val TAG = "WeatherWorker"
    private val PREFS_NAME = "weather_notifications"
    private val KEY_LAST_CONDITION = "last_condition"
    private val KEY_LAST_NOTIFY_TIME = "last_notify_time"
    private val NOTIFY_COOLDOWN_HOURS = 6

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting Weather Worker...")

        // 1. Permission Check
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return Result.success()
            }
        }

        return try {
            // 2. Load Cache
            weatherRepository.loadCachedData()
            val lastUpdate = weatherRepository.getLastUpdateTimestamp()
            val currentTime = System.currentTimeMillis()

            // 3. Stale Check (> 24 Hours)
            // If data is older than 24h, we assume it's invalid and stop (to avoid wrong info).
            val hoursSinceUpdate = TimeUnit.MILLISECONDS.toHours(currentTime - lastUpdate)
            if (lastUpdate == 0L || hoursSinceUpdate > 24) {
                return Result.success()
            }

            // 4. Get Current Condition
            val currentCondition = weatherRepository.getLatestForecastCondition()
            if (currentCondition == "Unknown") return Result.success()

            // 5. Always Send Notification (Smart Filter Removed)
            if (shouldNotify(currentCondition)) {
                sendAlert(currentCondition)
                Log.d(TAG, "Weather notification sent: $currentCondition")
            } else {
                Log.d(TAG, "Weather notification skipped (no change / cooldown)")
            }
            Log.d(TAG, "weather check complete.")


            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Weather worker failed", e)
            return Result.retry()
        }
    }
    private fun shouldNotify(newCondition: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val lastCondition = prefs.getString(KEY_LAST_CONDITION, null)
        val lastNotifyTime = prefs.getLong(KEY_LAST_NOTIFY_TIME, 0L)
        val now = System.currentTimeMillis()

        // 1. First time → allow notify
        if (lastCondition == null || lastNotifyTime == 0L) {
            saveNotificationState(newCondition, now)
            return true
        }

        // 2. Condition changed → notify
        if (!lastCondition.equals(newCondition, ignoreCase = true)) {
            saveNotificationState(newCondition, now)
            return true
        }

        // 3. Cooldown passed → notify
        val hoursSinceLast =
            TimeUnit.MILLISECONDS.toHours(now - lastNotifyTime)

        if (hoursSinceLast >= NOTIFY_COOLDOWN_HOURS) {
            saveNotificationState(newCondition, now)
            return true
        }

        // Otherwise → skip notification
        return false
    }
    private fun saveNotificationState(condition: String, time: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putString(KEY_LAST_CONDITION, condition)
                    .putLong(KEY_LAST_NOTIFY_TIME, time)
            }
    }

    private fun sendAlert(condition: String) {
        val message = when {
            condition.contains("Rain", ignoreCase = true) || condition.contains("Drizzle", ignoreCase = true) ->
                "🌧️ Rain expected. Postpone harvest and spraying."
            condition.contains("Thunderstorm", ignoreCase = true) ->
                "⛈️ Storm alert! Secure loose equipment immediately."
            condition.contains("Clear", ignoreCase = true) || condition.contains("Sunny", ignoreCase = true) ->
                "☀️ Clear skies! Good conditions for field work."
            condition.contains("Clouds", ignoreCase = true) ->
                "☁️ Cloudy forecast. Safe for transplanting."
            condition.contains("Snow", ignoreCase = true) ->
                "❄️ Snow alert. Ensure crops are covered."
            else -> "Weather Update: Expect $condition."
        }
        NotificationHelper.sendNotification(context, "Farm Weather Alert", message, 2001)
    }
}