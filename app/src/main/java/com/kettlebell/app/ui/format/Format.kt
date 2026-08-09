package com.kettlebell.app.ui.format

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val DATE_FORMAT = DateTimeFormatter.ofPattern("EEE d MMM")
private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

/** Formats a weight in kg, dropping the decimal when it's a whole number ("16 kg", "6 kg"). */
fun formatWeight(kg: Double): String {
    val text = if (kg % 1.0 == 0.0) kg.toInt().toString() else kg.toString()
    return "$text kg"
}

/** Formats a training volume with a "k" suffix once it gets large. */
fun formatVolume(kg: Double): String {
    val rounded = kg.roundToInt()
    return if (rounded >= 1000) {
        val thousands = rounded / 1000.0
        "${(thousands * 10).roundToInt() / 10.0}k kg"
    } else {
        "$rounded kg"
    }
}

fun formatDate(epochMillis: Long): String =
    DATE_FORMAT.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

fun formatTime(epochMillis: Long): String =
    TIME_FORMAT.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

fun formatClock(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val minutes = safe / 60
    val seconds = safe % 60
    return "%d:%02d".format(minutes, seconds)
}
