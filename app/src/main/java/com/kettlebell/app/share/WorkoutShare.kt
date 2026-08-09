package com.kettlebell.app.share

import android.content.Context
import android.content.Intent
import com.kettlebell.app.debug.AppLogger
import com.kettlebell.app.ui.format.WeightUnit
import com.kettlebell.app.ui.format.formatDate
import com.kettlebell.app.ui.format.formatVolume
import com.kettlebell.app.ui.format.formatWeight
import com.kettlebell.app.ui.model.SessionSummary

/** Builds a short plain-text workout recap and hands it to the Android share sheet. */
object WorkoutShare {

    private const val REPO_URL = "https://github.com/callum-p/android-kettlebell"

    fun shareSummary(context: Context, summary: SessionSummary, unit: WeightUnit) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, summary.session.title)
            putExtra(Intent.EXTRA_TEXT, buildText(summary, unit))
        }
        runCatching {
            context.startActivity(Intent.createChooser(intent, "Share workout"))
        }.onFailure { AppLogger.e("WorkoutShare", "Failed to share summary", it) }
    }

    fun buildText(summary: SessionSummary, unit: WeightUnit): String = buildString {
        appendLine("🏋️ ${summary.session.title} · ${formatDate(summary.session.startedAt)}")

        val headline = buildList {
            add("${summary.completedSets} sets")
            add(formatVolume(summary.totalVolumeKg, unit))
            summary.durationMinutes?.let { add("$it min") }
        }
        appendLine(headline.joinToString(" · "))

        summary.exercises.forEach { completed ->
            val done = completed.sets.filter { it.completed }
            if (done.isEmpty()) return@forEach
            val sets = done.joinToString(", ") { "${formatWeight(it.weightKg, unit)}×${it.reps}" }
            appendLine("• ${completed.exercise.name}: $sets")
        }

        appendLine()
        append("Tracked with Kettlebell 💪 $REPO_URL")
    }
}
