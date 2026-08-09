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

    fun setWeightUnit(unit: WeightUnit) {
        prefs.edit().putString(KEY_WEIGHT_UNIT, unit.name).apply()
        _weightUnit.value = unit
    }

    private fun loadWeightUnit(): WeightUnit = runCatching {
        WeightUnit.valueOf(prefs.getString(KEY_WEIGHT_UNIT, WeightUnit.KG.name)!!)
    }.getOrDefault(WeightUnit.KG)

    private companion object {
        const val KEY_WEIGHT_UNIT = "weight_unit"
    }
}
