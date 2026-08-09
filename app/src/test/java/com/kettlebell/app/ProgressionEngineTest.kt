package com.kettlebell.app

import com.kettlebell.app.data.ExerciseCatalog
import com.kettlebell.app.data.ProgressionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionEngineTest {

    private val bells = ExerciseCatalog.BELLS

    @Test
    fun nextBellAbove_returnsNextSize() {
        assertEquals(20.0, ProgressionEngine.nextBellAbove(16.0, bells), 0.0)
    }

    @Test
    fun nextBellAbove_atTop_staysAtTop() {
        assertEquals(40.0, ProgressionEngine.nextBellAbove(40.0, bells), 0.0)
    }

    @Test
    fun nextBellBelow_returnsPreviousSize() {
        assertEquals(16.0, ProgressionEngine.nextBellBelow(20.0, bells), 0.0)
    }

    @Test
    fun nextBellBelow_atBottom_staysAtBottom() {
        assertEquals(4.0, ProgressionEngine.nextBellBelow(4.0, bells), 0.0)
    }

    @Test
    fun snap_choosesClosestBell() {
        assertEquals(16.0, ProgressionEngine.snap(17.0, bells), 0.0)
        assertEquals(24.0, ProgressionEngine.snap(23.0, bells), 0.0)
    }

    @Test
    fun recommend_firstTime_snapsStartingWeight() {
        val exercise = TestData.exercise(startingWeightKg = 13.0, defaultSets = 3)
        val rec = ProgressionEngine.recommend(exercise, emptyList(), bells)
        assertTrue(rec.isFirstTime)
        assertEquals(12.0, rec.weightKg, 0.0) // 13 snaps to nearest bell (12)
        assertEquals(3, rec.sets)
    }

    @Test
    fun recommend_hittingTopReps_sizesUp() {
        val exercise = TestData.exercise(repRangeLow = 8, repRangeHigh = 12, defaultSets = 3)
        val sets = List(3) { TestData.set(weightKg = 16.0, reps = 12) }
        val rec = ProgressionEngine.recommend(exercise, sets, bells)
        assertFalse(rec.isFirstTime)
        assertEquals(20.0, rec.weightKg, 0.0)
    }

    @Test
    fun recommend_midRange_holdsWeight() {
        val exercise = TestData.exercise(repRangeLow = 8, repRangeHigh = 12, defaultSets = 3)
        val sets = List(3) { TestData.set(weightKg = 16.0, reps = 9) }
        val rec = ProgressionEngine.recommend(exercise, sets, bells)
        assertEquals(16.0, rec.weightKg, 0.0)
    }

    @Test
    fun recommend_belowLowReps_sizesDown() {
        val exercise = TestData.exercise(repRangeLow = 8, repRangeHigh = 12, defaultSets = 3)
        val sets = List(3) { TestData.set(weightKg = 16.0, reps = 5) }
        val rec = ProgressionEngine.recommend(exercise, sets, bells)
        assertEquals(14.0, rec.weightKg, 0.0)
    }

    @Test
    fun recommend_onlyOwnedBellsAreSuggested() {
        val owned = listOf(8.0, 12.0, 16.0)
        val exercise = TestData.exercise(repRangeHigh = 10, defaultSets = 2)
        val sets = List(2) { TestData.set(weightKg = 16.0, reps = 10) }
        val rec = ProgressionEngine.recommend(exercise, sets, owned)
        // Already at the heaviest owned bell — should not jump beyond it.
        assertEquals(16.0, rec.weightKg, 0.0)
    }
}
