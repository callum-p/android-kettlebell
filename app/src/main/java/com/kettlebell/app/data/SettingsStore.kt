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
    }
}
