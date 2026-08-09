package com.kettlebell.app

import com.kettlebell.app.data.db.BodyPart
import com.kettlebell.app.data.db.Exercise
import com.kettlebell.app.data.db.Level
import com.kettlebell.app.data.db.WorkoutSession
import com.kettlebell.app.data.db.WorkoutSet
import com.kettlebell.app.ui.model.CompletedExercise
import com.kettlebell.app.ui.model.SessionSummary

/** Factory helpers for building domain objects in tests. */
object TestData {

    fun exercise(
        id: String = "swing",
        name: String = "Kettlebell Swing",
        level: Level = Level.BEGINNER,
        bodyParts: List<BodyPart> = listOf(BodyPart.LEGS),
        repRangeLow: Int = 8,
        repRangeHigh: Int = 12,
        defaultSets: Int = 3,
        startingWeightKg: Double = 12.0,
    ): Exercise = Exercise(
        id = id,
        name = name,
        level = level,
        category = "Test",
        primaryMuscles = "Everything",
        description = "A test exercise.",
        instructions = listOf("Do the thing."),
        bodyParts = bodyParts,
        youtubeUrl = "",
        repRangeLow = repRangeLow,
        repRangeHigh = repRangeHigh,
        defaultSets = defaultSets,
        defaultRestSeconds = 60,
        startingWeightKg = startingWeightKg,
    )

    fun set(
        weightKg: Double,
        reps: Int,
        completed: Boolean = true,
        rpe: Int? = null,
        notes: String? = null,
        completedAt: Long? = null,
    ): WorkoutSet = WorkoutSet(
        id = 0,
        sessionExerciseId = 1,
        setNumber = 1,
        weightKg = weightKg,
        targetReps = reps,
        reps = reps,
        completed = completed,
        completedAt = completedAt,
        rpe = rpe,
        notes = notes,
    )

    fun session(
        title: String = "Workout",
        startedAt: Long = 0L,
        dateEpochDay: Long = 0L,
        finishedAt: Long? = 1L,
    ): WorkoutSession = WorkoutSession(
        id = 0,
        title = title,
        startedAt = startedAt,
        finishedAt = finishedAt,
        dateEpochDay = dateEpochDay,
    )

    fun summary(
        exercise: Exercise,
        sets: List<WorkoutSet>,
        startedAt: Long = 0L,
        dateEpochDay: Long = 0L,
        title: String = "Workout",
    ): SessionSummary = SessionSummary(
        session = session(title = title, startedAt = startedAt, dateEpochDay = dateEpochDay),
        exercises = listOf(CompletedExercise(exercise, sets)),
    )
}
