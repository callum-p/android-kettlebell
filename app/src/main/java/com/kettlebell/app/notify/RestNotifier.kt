package com.kettlebell.app.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kettlebell.app.R
import com.kettlebell.app.debug.AppLogger

/** Posts a heads-up notification (with sound + vibration) when a rest period ends. */
object RestNotifier {

    private const val CHANNEL_ID = "rest_timer"
    private const val NOTIFICATION_ID = 1001

    /** Create the notification channel. Safe to call repeatedly. */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Rest timer",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Alerts you when a rest period is over"
                enableVibration(true)
                enableLights(true)
            }
            context.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }

    fun notifyRestComplete(context: Context, message: String) {
        if (!hasPermission(context)) return
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Rest complete 💪")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }.onFailure { AppLogger.e("RestNotifier", "Failed to post notification", it) }
    }

    fun cancel(context: Context) {
        runCatching { NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID) }
    }

    private fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
}
