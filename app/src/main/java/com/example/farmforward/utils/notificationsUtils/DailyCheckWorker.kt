package com.example.farmforward.utils.notificationsUtils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.farmforward.database.roomDatabase.AppDatabase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class DailyCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d("DailyCheckWorker", "Starting daily crop check...")

        // 1. Check Notification Permission
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w("DailyCheckWorker", "Notification permission not granted")
                return Result.success()
            }
        }

        return try {
            // 2. Get User ID correctly from "user_session" (matches SessionManager.kt)
            val prefs = applicationContext.getSharedPreferences("user_session", Context.MODE_PRIVATE)
            val userId = prefs.getInt("user_id", -1)

            Log.d("DailyCheckWorker", "WORKER IS RUNNING FOR USER ID: $userId")

            // 3. Setup Database
            val database = AppDatabase.getDatabase(applicationContext)
            val cropDao = database.cropDao()

            // 4. Filter Crops by User ID
            val allCrops = if (userId != -1) {
                cropDao.getCropsForUserList(userId)
            } else {
                Log.e("DailyCheckWorker", "No valid user found. Skipping.")
                emptyList()
            }

            val today = System.currentTimeMillis()
            val readyList = mutableListOf<String>()
            val soonList = mutableListOf<String>()
            val overdueList = mutableListOf<String>()
            val plantingList = mutableListOf<String>()
            var unsyncedCount = 0

            Log.d("DailyCheckWorker", "Found ${allCrops.size} crops to check.")

            for (crop in allCrops) {
                if (crop.isSynced == 0) unsyncedCount++

                // HARVEST LOGIC
                if (crop.mindate != null) {
                    val diff = crop.mindate - today
                    val daysLeft = TimeUnit.MILLISECONDS.toDays(diff)

                    // Priority 1: Overdue (Max date passed)
                    if ((crop.maxdate != null && today > crop.maxdate && crop.harvestedDate == null)) {
                        overdueList.add(crop.cropName)
                    }
                    // Priority 2: Ready Now (Today is past mindate)
                    else if (daysLeft <= 0L && crop.harvestedDate == null) {
                        readyList.add(crop.cropName)
                    }
                    // Priority 3: Coming Soon (1 to 3 days left)
                    else if (daysLeft in 1..3 && crop.harvestedDate == null) {
                        soonList.add("${crop.cropName} (${daysLeft}d)")
                    }
                }

                // PLANTING LOGIC
                if (crop.date > today) {
                    val diff = crop.date - today
                    val daysLeft = TimeUnit.MILLISECONDS.toDays(diff)
                    if (daysLeft == 1L) {
                        plantingList.add(crop.cropName)
                    }
                }
            }

            // 5. Send Notifications
            if (readyList.isNotEmpty()) {
                val title = "Ready to Harvest! 🌾"
                val message = "Harvest now: ${readyList.joinToString(", ")}"
                NotificationHelper.sendNotification(applicationContext, title, message, 1001)
            }
            if (soonList.isNotEmpty()) {
                val title = "Harvest Coming Soon ⏳"
                val message = "Upcoming: ${soonList.joinToString(", ")}"
                NotificationHelper.sendNotification(applicationContext, title, message, 1002)
            }
            if (overdueList.isNotEmpty()) {
                val title = "Urgent: Crops Overdue ⚠️"
                val message = "Action needed for: ${overdueList.joinToString(", ")}"
                NotificationHelper.sendNotification(applicationContext, title, message, 1003)
            }
            if (plantingList.isNotEmpty()) {
                val title = "Planting Reminder 🌱"
                val message = "Tomorrow is planting day for: ${plantingList.joinToString(", ")}"
                NotificationHelper.sendNotification(applicationContext, title, message, 1004)
            }
            if (unsyncedCount > 0) {
                NotificationHelper.sendNotification(
                    applicationContext,
                    "Backup Your Farm ☁️",
                    "You have $unsyncedCount unsynced items. Login to sync.",
                    1005
                )
            }

            Log.d("DailyCheckWorker", "Daily check complete.")
            Result.success()

        } catch (e: Exception) {
            Log.e("DailyCheckWorker", "Crash in worker: ${e.message}")
            e.printStackTrace()
            Result.retry()
        }
    }
}