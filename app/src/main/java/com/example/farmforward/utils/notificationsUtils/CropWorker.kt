package com.example.farmforward.utils.notificationsUtils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.farmforward.database.roomDatabase.AppDatabase
import java.util.concurrent.TimeUnit

class CropWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val database = AppDatabase.getDatabase(applicationContext)
            val cropDao = database.cropDao()
            val allCrops = cropDao.getAllCrops() //
            val today = System.currentTimeMillis()

            for (crop in allCrops) {
                // 1. Harvest Logic
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
                    } else if (crop.maxdate != null && today > crop.maxdate) {
                        NotificationHelper.sendNotification(
                            applicationContext,
                            "Urgent: Crop Overdue",
                            "${crop.cropName} is past its harvest date.",
                            crop.id
                        )
                    }
                }

                // 2. Planting Logic (Tomorrow's plan)
                if (crop.date > today) {
                    val diff = crop.date - today
                    val daysLeft = TimeUnit.MILLISECONDS.toDays(diff)

                    if (daysLeft == 1L) {
                        NotificationHelper.sendNotification(
                            applicationContext,
                            "Planting Tomorrow",
                            "Reminder: Planting scheduled for ${crop.cropName} tomorrow.",
                            crop.id + 100000
                        )
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}