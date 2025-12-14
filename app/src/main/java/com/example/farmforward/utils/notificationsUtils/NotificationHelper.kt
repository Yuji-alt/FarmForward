package com.example.farmforward.utils.notificationsUtils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.farmforward.R
import com.example.farmforward.appActivity.mainActivity.MainActivity
import java.util.concurrent.TimeUnit

object NotificationHelper {

    private const val CHANNEL_ID = "farm_forward_channel"
    private const val CHANNEL_NAME = "Farm Alerts"
    private const val GROUP_KEY_FARM_ALERTS = "com.example.farmforward.FARM_ALERTS_GROUP"

    fun sendNotification(context: Context, title: String, message: String, notificationId: Int) {


        // 1. Ensure Channel Exists (Crucial)
        createNotificationChannel(context)

        // 2. Prepare Click Action (Open App)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            val targetTab = when (notificationId) {
                1001, 1002, 1003 -> R.id.nav_garden
                1004 -> R.id.nav_growth
                2001 -> R.id.nav_home
                else -> R.id.nav_home
            }
            putExtra("DESTINATION_TAB", targetTab)
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 3. Build Notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.agricultwo) // Ensure this icon exists!
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Changed to HIGH for heads-up alerts
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY_FARM_ALERTS)

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(notificationId, builder.build())

            // 4. Group Summary (Optional but good)
            val summaryNotification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.agricultwo)
                .setStyle(NotificationCompat.InboxStyle().setSummaryText("Farm Updates"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setGroup(GROUP_KEY_FARM_ALERTS)
                .setGroupSummary(true)
                .build()

            manager.notify(0, summaryNotification)

        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
    // Made public so Worker can call it directly if needed
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Changed to IMPORTANCE_HIGH so it makes sound and pops up
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Notifications for Harvest, Weather, and Schedules"
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}