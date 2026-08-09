package com.kettlebell.app

import com.kettlebell.app.data.ExerciseCatalog
import com.kettlebell.app.data.db.BodyPart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseCatalogTest {

    @Test
    fun exercises_areNonEmptyWithUniqueIds() {
        val exercises = ExerciseCatalog.exercises
        assertTrue(exercises.isNotEmpty())
        val ids = exercises.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun exercises_haveSaneRepRanges() {
        ExerciseCatalog.exercises.forEach { ex ->
            assertTrue("${ex.id} rep range", ex.repRangeLow in 1..ex.repRangeHigh)
            assertTrue("${ex.id} sets", ex.defaultSets >= 1)
            assertTrue("${ex.id} starting weight", ex.startingWeightKg > 0.0)
        }
    }

    @Test
    fun everyTemplateReferencesRealExercises() {
        ExerciseCatalog.templates.forEach { template ->
            assertTrue("${template.name} has exercises", template.exerciseIds.isNotEmpty())
            template.exerciseIds.forEach { id ->
                assertNotNull("template ${template.name} references missing $id", ExerciseCatalog.byId(id))
            }
        }
    }

    @Test
    fun byId_unknownReturnsNull() {
        assertNull(ExerciseCatalog.byId("does-not-exist"))
    }

    @Test
    fun forBodyPart_onlyReturnsMatchingExercises() {
        BodyPart.entries.forEach { part ->
            ExerciseCatalog.forBodyPart(part).forEach { ex ->
                assertTrue("${ex.id} should target $part", part in ex.bodyParts)
            }
        }
    }

    @Test
    fun bells_areSortedAndPositive() {
        val bells = ExerciseCatalog.BELLS
        assertTrue(bells.isNotEmpty())
        assertEquals(bells.sorted(), bells)
        assertTrue(bells.all { it > 0.0 })
    }
}
