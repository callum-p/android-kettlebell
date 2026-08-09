package com.kettlebell.app.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

/**
 * Schedules a daily, repeating workout reminder using an inexact alarm.
 *
 * Inexact repeating alarms need no special runtime permission (unlike exact
 * alarms on Android 12+) and the OS batches them to save battery — perfectly
 * adequate for a once-a-day nudge.
 */
object ReminderScheduler {

    private const val REQUEST_CODE = 2001

    fun schedule(context: Context, hour: Int, minute: Int) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = pendingIntent(context)
        manager.cancel(pending)
        manager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            nextTriggerMillis(hour, minute),
            AlarmManager.INTERVAL_DAY,
            pending,
        )
    }

    fun cancel(context: Context) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        manager.cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun nextTriggerMillis(hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }
}
