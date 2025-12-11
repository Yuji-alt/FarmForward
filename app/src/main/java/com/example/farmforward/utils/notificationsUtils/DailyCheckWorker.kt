package com.example.farmforward.utils.notificationsUtils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.farmforward.database.roomDatabase.AppDatabase
import java.util.concurrent.TimeUnit

class DailyCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return Result.success()
            }
        }

        return try {
            val database = AppDatabase.getDatabase(applicationContext)
            val cropDao = database.cropDao()
            val allCrops = cropDao.getAllCrops()
            val today = System.currentTimeMillis()
            val readyList = mutableListOf<String>()
            val soonList = mutableListOf<String>()
            val overdueList = mutableListOf<String>()
            val plantingList = mutableListOf<String>()
            var unsyncedCount = 0

            for (crop in allCrops) {
                if (crop.isSynced == 0) unsyncedCount++
                if (crop.mindate != null) {
                    val diff = crop.mindate - today
                    val daysLeft = TimeUnit.MILLISECONDS.toDays(diff)
                    if (crop.maxdate != null && today > crop.maxdate && crop.harvestedDate == null) {
                        overdueList.add(crop.cropName)
                    }
                    else if (daysLeft <= 0L && crop.harvestedDate == null) {
                        readyList.add(crop.cropName)
                    }
                    else if (daysLeft in 1..3 && crop.harvestedDate == null) {
                        soonList.add("${crop.cropName} (${daysLeft}d)")
                    }
                }

                if (crop.date > today) {
                    val diff = crop.date - today
                    val daysLeft = TimeUnit.MILLISECONDS.toDays(diff)

                    if (daysLeft == 1L) {
                        plantingList.add(crop.cropName)
                    }
                }
            }

            if (readyList.isNotEmpty()) {
                val title = "Ready to Harvest! 🌾"
                val message = if (readyList.size == 1) {
                    "${readyList[0]} is ready for harvest today."
                } else {
                    "You have ${readyList.size} crops ready: ${readyList.joinToString(", ")}"
                }
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
                val message = "Scheduled for tomorrow: ${plantingList.joinToString(", ")}"
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

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}