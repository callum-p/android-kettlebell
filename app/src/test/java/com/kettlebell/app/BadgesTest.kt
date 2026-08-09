package com.kettlebell.app

import com.kettlebell.app.badges.Badges
import com.kettlebell.app.data.db.Level
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BadgesTest {

    @Test
    fun catalogue_hasUniqueIds() {
        val ids = Badges.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun catalogue_size() {
        assertEquals(22, Badges.all.size)
    }

    @Test
    fun emptyHistory_earnsNothing() {
        assertTrue(Badges.earnedIds(emptyList()).isEmpty())
    }

    @Test
    fun firstWorkout_earnsFirstStepsAndHeavyMetal() {
        val ex = TestData.exercise()
        val history = listOf(TestData.summary(ex, listOf(TestData.set(24.0, 10))))
        val earned = Badges.earnedIds(history)
        assertTrue("first_workout" in earned)
        assertTrue("heavy_metal" in earned) // 24 kg threshold
        assertFalse("beast_mode" in earned) // needs 32 kg
        assertFalse("high_five" in earned) // needs 5 workouts
    }

    @Test
    fun advancedExercise_earnsElite() {
        val advanced = TestData.exercise(id = "snatch", level = Level.ADVANCED)
        val history = listOf(TestData.summary(advanced, listOf(TestData.set(16.0, 5))))
        assertTrue("elite" in Badges.earnedIds(history))
    }

    @Test
    fun consecutiveDays_earnStreak() {
        val ex = TestData.exercise()
        val history = listOf(
            TestData.summary(ex, listOf(TestData.set(12.0, 10)), dateEpochDay = 10),
            TestData.summary(ex, listOf(TestData.set(12.0, 10)), dateEpochDay = 11),
        )
        val earned = Badges.earnedIds(history)
        assertTrue("streak_2" in earned)
        assertFalse("streak_3" in earned)
    }

    @Test
    fun evaluate_marksEarnedFlagConsistently() {
        val ex = TestData.exercise()
        val history = listOf(TestData.summary(ex, listOf(TestData.set(12.0, 10))))
        val states = Badges.evaluate(history)
        assertEquals(Badges.all.size, states.size)
        assertTrue(states.first { it.badge.id == "first_workout" }.earned)
    }
}
