package com.kettlebell.app.data

import android.content.Context
import com.kettlebell.app.ui.format.WeightUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Persists lightweight user preferences (currently the weight display unit). */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _weightUnit = MutableStateFlow(loadWeightUnit())
    val weightUnit: StateFlow<WeightUnit> = _weightUnit.asStateFlow()

    private val _ownedBells = MutableStateFlow(loadOwnedBells())
    /** The kettlebell sizes (kg) the user owns; recommendations only ever suggest these. */
    val ownedBells: StateFlow<List<Double>> = _ownedBells.asStateFlow()

    private val _reminderEnabled = MutableStateFlow(prefs.getBoolean(KEY_REMINDER_ENABLED, false))
    /** Whether a daily workout reminder notification is scheduled. */
    val reminderEnabled: StateFlow<Boolean> = _reminderEnabled.asStateFlow()

    private val _reminderHour = MutableStateFlow(prefs.getInt(KEY_REMINDER_HOUR, 18))
    /** Hour of day (0–23) the reminder fires. */
    val reminderHour: StateFlow<Int> = _reminderHour.asStateFlow()

    private val _reminderMinute = MutableStateFlow(prefs.getInt(KEY_REMINDER_MINUTE, 0))
    /** Minute (0–59) the reminder fires. */
    val reminderMinute: StateFlow<Int> = _reminderMinute.asStateFlow()

    fun setWeightUnit(unit: WeightUnit) {
        prefs.edit().putString(KEY_WEIGHT_UNIT, unit.name).apply()
        _weightUnit.value = unit
    }

    fun setOwnedBells(bells: Set<Double>) {
        // Never allow an empty set — fall back to all standard sizes.
        val effective = bells.sorted().ifEmpty { ExerciseCatalog.BELLS }
        prefs.edit().putString(KEY_OWNED_BELLS, effective.joinToString(",")).apply()
        _ownedBells.value = effective
    }

    fun setReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REMINDER_ENABLED, enabled).apply()
        _reminderEnabled.value = enabled
    }

    fun setReminderTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_REMINDER_HOUR, hour)
            .putInt(KEY_REMINDER_MINUTE, minute)
            .apply()
        _reminderHour.value = hour
        _reminderMinute.value = minute
    }

    private fun loadWeightUnit(): WeightUnit = runCatching {
        WeightUnit.valueOf(prefs.getString(KEY_WEIGHT_UNIT, WeightUnit.KG.name)!!)
    }.getOrDefault(WeightUnit.KG)

    private fun loadOwnedBells(): List<Double> {
        val csv = prefs.getString(KEY_OWNED_BELLS, null) ?: return ExerciseCatalog.BELLS
        val list = csv.split(",").mapNotNull { it.toDoubleOrNull() }.sorted()
        return list.ifEmpty { ExerciseCatalog.BELLS }
    }

    private companion object {
        const val KEY_WEIGHT_UNIT = "weight_unit"
        const val KEY_OWNED_BELLS = "owned_bells"
        const val KEY_REMINDER_ENABLED = "reminder_enabled"
        const val KEY_REMINDER_HOUR = "reminder_hour"
        const val KEY_REMINDER_MINUTE = "reminder_minute"
    }
}
