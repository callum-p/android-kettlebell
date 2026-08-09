package com.kettlebell.app

import com.kettlebell.app.ui.format.WeightUnit
import com.kettlebell.app.ui.format.formatClock
import com.kettlebell.app.ui.format.formatVolume
import com.kettlebell.app.ui.format.formatWeight
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {

    @Test
    fun formatWeight_kilograms_trimsTrailingZeros() {
        assertEquals("16 kg", formatWeight(16.0, WeightUnit.KG))
        assertEquals("6 kg", formatWeight(6.0, WeightUnit.KG))
    }

    @Test
    fun formatWeight_pounds_convertsAndRounds() {
        assertEquals("35 lb", formatWeight(16.0, WeightUnit.LB))
    }

    @Test
    fun formatVolume_smallStaysWhole() {
        assertEquals("500 kg", formatVolume(500.0, WeightUnit.KG))
    }

    @Test
    fun formatVolume_largeUsesKSuffix() {
        assertEquals("5.0k kg", formatVolume(5000.0, WeightUnit.KG))
        assertEquals("1.2k kg", formatVolume(1234.0, WeightUnit.KG))
    }

    @Test
    fun formatClock_padsSeconds() {
        assertEquals("1:30", formatClock(90))
        assertEquals("0:05", formatClock(5))
        assertEquals("0:00", formatClock(-10))
    }
}
