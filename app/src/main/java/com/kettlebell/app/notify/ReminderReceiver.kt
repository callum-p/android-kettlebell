package com.kettlebell.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Fires on the daily reminder alarm and posts the notification. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        RestNotifier.ensureChannel(context)
        RestNotifier.notifyReminder(context)
    }
}
