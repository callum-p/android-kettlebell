package com.kettlebell.app.ui.format

import androidx.compose.runtime.staticCompositionLocalOf
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/** The unit weights are displayed in. Data is always stored in kilograms. */
enum class WeightUnit(val label: String, val suffix: String) {
    KG("Kilograms (kg)", "kg"),
    LB("Pounds (lb)", "lb"),
}

private const val KG_TO_LB = 2.2046226218

/** Current display unit, provided from settings at the top of the UI tree. */
val LocalWeightUnit = staticCompositionLocalOf { WeightUnit.KG }

private val DATE_FORMAT = DateTimeFormatter.ofPattern("EEE d MMM")
private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

/** Formats a weight (stored in kg) in the chosen unit — "16 kg", "6 kg", or "35 lb". */
fun formatWeight(kg: Double, unit: WeightUnit = WeightUnit.KG): String = when (unit) {
    WeightUnit.KG -> "${trimZeros(kg)} kg"
    WeightUnit.LB -> "${(kg * KG_TO_LB).roundToInt()} lb"
}

/** Formats a training volume (stored in kg) in the chosen unit, with a "k" suffix once large. */
fun formatVolume(kg: Double, unit: WeightUnit = WeightUnit.KG): String {
    val value = if (unit == WeightUnit.KG) kg else kg * KG_TO_LB
    val rounded = value.roundToInt()
    return if (rounded >= 1000) {
        "${(rounded / 100) / 10.0}k ${unit.suffix}"
    } else {
        "$rounded ${unit.suffix}"
    }
}

private fun trimZeros(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

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
