package com.kettlebell.app

import com.kettlebell.app.progress.Progress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class ProgressTest {

    @Test
    fun estimatedOneRepMax_singleRep_isTheWeight() {
        assertEquals(20.0, Progress.estimatedOneRepMax(20.0, 1), 0.0)
    }

    @Test
    fun estimatedOneRepMax_usesEpleyFormula() {
        // 20 * (1 + 5/30) = 23.333…
        assertEquals(23.333, Progress.estimatedOneRepMax(20.0, 5), 0.01)
    }

    @Test
    fun bests_picksHighestEstimatedOneRepMax() {
        val exercise = TestData.exercise(id = "press", name = "Press")
        val history = listOf(
            TestData.summary(exercise, listOf(TestData.set(16.0, 8), TestData.set(20.0, 3))),
        )
        val bests = Progress.bests(history)
        assertEquals(1, bests.size)
        // 16*(1+8/30)=20.27 vs 20*(1+3/30)=22.0 → 20kg×3 wins.
        assertEquals(20.0, bests.first().weightKg, 0.0)
        assertEquals(3, bests.first().reps)
    }

    @Test
    fun bestFor_unknownExercise_isNull() {
        assertNull(Progress.bestFor(emptyList(), "nope"))
    }

    @Test
    fun activeDays_collectsDistinctDays() {
        val ex = TestData.exercise()
        val history = listOf(
            TestData.summary(ex, listOf(TestData.set(12.0, 10)), dateEpochDay = 100),
            TestData.summary(ex, listOf(TestData.set(12.0, 10)), dateEpochDay = 100),
            TestData.summary(ex, listOf(TestData.set(12.0, 10)), dateEpochDay = 102),
        )
        assertEquals(setOf(100L, 102L), Progress.activeDays(history))
    }

    @Test
    fun weekStart_returnsMonday() {
        val start = Progress.weekStart(20_000L)
        assertEquals(DayOfWeek.MONDAY, LocalDate.ofEpochDay(start).dayOfWeek)
        assertTrue(start <= 20_000L && start > 20_000L - 7)
    }

    @Test
    fun weeklyVolume_bucketsIntoRequestedWeeks() {
        val ex = TestData.exercise()
        val today = Progress.weekStart(1_000L) + 3 // some midweek day
        val history = listOf(
            TestData.summary(ex, listOf(TestData.set(10.0, 10)), dateEpochDay = today),
        )
        val weeks = Progress.weeklyVolume(history, today, 4)
        assertEquals(4, weeks.size)
        // The most recent bucket should hold this week's 100 kg of volume.
        assertEquals(100.0, weeks.last().volumeKg, 0.0)
    }
}
