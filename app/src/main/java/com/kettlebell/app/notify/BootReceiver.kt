package com.kettlebell.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kettlebell.app.data.SettingsStore

/** Re-schedules the daily reminder after a device reboot, since repeating alarms are cleared. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val settings = SettingsStore(context)
        if (settings.reminderEnabled.value) {
            ReminderScheduler.schedule(
                context,
                settings.reminderHour.value,
                settings.reminderMinute.value,
            )
        }
    }
}
