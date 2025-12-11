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

object NotificationHelper {

    private const val CHANNEL_ID = "farm_forward_channel"
    private const val CHANNEL_NAME = "Farm Alerts"
    private const val GROUP_KEY_FARM_ALERTS = "com.example.farmforward.FARM_ALERTS_GROUP"

    fun sendNotification(context: Context, title: String, message: String, notificationId: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        createNotificationChannel(context)

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
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.agricultwo)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY_FARM_ALERTS)

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(notificationId, builder.build())
            val summaryNotification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.agricultwo)
                .setStyle(NotificationCompat.InboxStyle()
                    .setSummaryText("Farm Updates"))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setGroup(GROUP_KEY_FARM_ALERTS)
                .setGroupSummary(true)
                .build()

            manager.notify(0, summaryNotification)

        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Notifications for Harvest, Weather, and Schedules"
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}