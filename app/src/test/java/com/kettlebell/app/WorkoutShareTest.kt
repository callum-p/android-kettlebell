package com.kettlebell.app

import com.kettlebell.app.share.WorkoutShare
import com.kettlebell.app.ui.format.WeightUnit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutShareTest {

    private val exercise = TestData.exercise(name = "Kettlebell Swing")

    @Test
    fun buildText_isBriefWithRepoLink() {
        val summary = TestData.summary(
            exercise,
            listOf(TestData.set(16.0, 10), TestData.set(16.0, 8)),
            title = "Morning Swings",
        )
        val text = WorkoutShare.buildText(summary, WeightUnit.KG)

        assertTrue(text.contains("Morning Swings"))
        assertTrue(text.contains("2 sets"))
        assertTrue(text.contains("• Kettlebell Swing: 16 kg×10, 16 kg×8"))
        assertTrue(text.contains("https://github.com/callum-p/android-kettlebell"))
    }

    @Test
    fun buildText_skipsExercisesWithNoCompletedSets() {
        val summary = TestData.summary(
            exercise,
            listOf(TestData.set(16.0, 10, completed = false)),
        )
        val text = WorkoutShare.buildText(summary, WeightUnit.KG)
        assertFalse(text.contains("Kettlebell Swing:"))
    }
}
