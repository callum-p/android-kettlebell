package com.kettlebell.app.ui.model

import com.kettlebell.app.data.db.Exercise
import com.kettlebell.app.data.db.SessionExercise
import com.kettlebell.app.data.db.WorkoutSession
import com.kettlebell.app.data.db.WorkoutSet

/** An exercise inside the currently active workout, with its planned/logged sets. */
data class ActiveExercise(
    val sessionExercise: SessionExercise,
    val exercise: Exercise,
    val sets: List<WorkoutSet>,
)

/** The workout the user is performing right now. */
data class ActiveWorkout(
    val session: WorkoutSession,
    val exercises: List<ActiveExercise>,
) {
    val completedSets: Int get() = exercises.sumOf { ex -> ex.sets.count { it.completed } }
    val totalSets: Int get() = exercises.sumOf { it.sets.size }
    val totalVolumeKg: Double
        get() = exercises.sumOf { ex -> ex.sets.filter { it.completed }.sumOf { it.weightKg * it.reps } }
}

/** A performed exercise within a finished session. */
data class CompletedExercise(
    val exercise: Exercise,
    val sets: List<WorkoutSet>,
)

/** A finished workout, ready to render on the History screen. */
data class SessionSummary(
    val session: WorkoutSession,
    val exercises: List<CompletedExercise>,
) {
    val completedSets: Int get() = exercises.sumOf { ex -> ex.sets.count { it.completed } }
    val totalVolumeKg: Double
        get() = exercises.sumOf { ex -> ex.sets.filter { it.completed }.sumOf { it.weightKg * it.reps } }
    val durationMinutes: Long?
        get() = session.finishedAt?.let { ((it - session.startedAt) / 60_000L).coerceAtLeast(0) }
}

/** State of the between-sets rest countdown. */
data class RestTimerState(
    val totalSeconds: Int,
    val remainingSeconds: Int,
    val exerciseName: String,
) {
    val finished: Boolean get() = remainingSeconds <= 0
    val progress: Float
        get() = if (totalSeconds <= 0) 0f else remainingSeconds.toFloat() / totalSeconds.toFloat()
}

/** Headline numbers for the Home screen. */
data class HomeStats(
    val totalWorkouts: Int = 0,
    val workoutsThisWeek: Int = 0,
    val totalVolumeKg: Double = 0.0,
    val totalSets: Int = 0,
)

/** One past appearance of an exercise, used on the exercise detail screen. */
data class ExerciseHistoryEntry(
    val startedAt: Long,
    val sets: List<WorkoutSet>,
) {
    val topWeightKg: Double get() = sets.maxOfOrNull { it.weightKg } ?: 0.0
    val totalReps: Int get() = sets.sumOf { it.reps }
}

/** Complete state driving the app's screens. */
data class WorkoutUiState(
    val loading: Boolean = true,
    val exercises: List<Exercise> = emptyList(),
    val activeWorkout: ActiveWorkout? = null,
    val history: List<SessionSummary> = emptyList(),
    val stats: HomeStats = HomeStats(),
)
