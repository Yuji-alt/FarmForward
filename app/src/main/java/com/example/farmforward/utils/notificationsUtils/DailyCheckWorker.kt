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
                return Result.failure()
            }
        }

        return try {
            val database = AppDatabase.getDatabase(applicationContext)
            val cropDao = database.cropDao()

            val allCrops = cropDao.getAllCrops()

            val today = System.currentTimeMillis()

            var unsyncedCount = 0

            for (crop in allCrops) {
                if (crop.isSynced == 0) {
                    unsyncedCount++
                }
                if (crop.mindate != null) {
                    val diff = crop.mindate - today
                    val daysLeft = TimeUnit.MILLISECONDS.toDays(diff)

                    if (daysLeft == 0L) {
                        NotificationHelper.sendNotification(
                            applicationContext,
                            "Ready to Harvest!",
                            "${crop.cropName} is ready for harvest today.",
                            crop.id
                        )
                    } else if (daysLeft in 1..3) {
                        NotificationHelper.sendNotification(
                            applicationContext,
                            "Harvest Coming Soon",
                            "${crop.cropName} will be ready in $daysLeft days.",
                            crop.id
                        )
                    } else if (crop.maxdate != null && today > crop.maxdate) {
                        NotificationHelper.sendNotification(
                            applicationContext,
                            "Urgent: Crop Overdue",
                            "${crop.cropName} is past its harvest date. Check it now!",
                            crop.id
                        )
                    }
                }

                if (crop.date > today) {
                    val diff = crop.date - today
                    val daysLeft = TimeUnit.MILLISECONDS.toDays(diff)

                    if (daysLeft == 1L) {
                        NotificationHelper.sendNotification(
                            applicationContext,
                            "Planting Tomorrow",
                            "Reminder: You have a scheduled planting for ${crop.cropName} tomorrow.",
                            crop.id + 100000 // Increased offset to prevent ID conflict
                        )
                    }
                }
            }

            if (unsyncedCount > 0) {
                NotificationHelper.sendNotification(
                    applicationContext,
                    "Backup Your Farm",
                    "You have $unsyncedCount unsynced items. Connect to Wi-Fi to save them.",
                    99999
                )
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}