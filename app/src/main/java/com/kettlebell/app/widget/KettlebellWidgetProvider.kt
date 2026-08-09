package com.kettlebell.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.kettlebell.app.MainActivity
import com.kettlebell.app.R

/**
 * A small home-screen widget showing total workouts and this week's count.
 *
 * The numbers are cached in SharedPreferences by the app (see [updateStats]) so the
 * widget never has to touch the database on the main thread.
 */
class KettlebellWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { id -> renderWidget(context, appWidgetManager, id) }
    }

    companion object {
        private const val PREFS = "widget_stats"
        private const val KEY_TOTAL = "total"
        private const val KEY_WEEK = "week"

        /** Persist the latest stats and refresh every placed widget. */
        fun updateStats(context: Context, totalWorkouts: Int, workoutsThisWeek: Int) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt(KEY_TOTAL, totalWorkouts)
                .putInt(KEY_WEEK, workoutsThisWeek)
                .apply()

            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, KettlebellWidgetProvider::class.java),
            )
            ids.forEach { id -> renderWidget(context, manager, id) }
        }

        private fun renderWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val total = prefs.getInt(KEY_TOTAL, 0)
            val week = prefs.getInt(KEY_WEEK, 0)

            val views = RemoteViews(context.packageName, R.layout.widget_kettlebell).apply {
                setTextViewText(R.id.widget_total, total.toString())
                setTextViewText(R.id.widget_subtitle, if (total == 1) "workout" else "workouts")
                setTextViewText(R.id.widget_week, "$week this week")
                setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
            }
            manager.updateAppWidget(id, views)
        }

        private fun openAppIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
