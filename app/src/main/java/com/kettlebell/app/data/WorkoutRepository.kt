package com.kettlebell.app.data

import com.kettlebell.app.data.db.Exercise
import com.kettlebell.app.data.db.KettlebellDatabase
import com.kettlebell.app.data.db.SessionExercise
import com.kettlebell.app.data.db.WorkoutSession
import com.kettlebell.app.data.db.WorkoutSet
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Single entry point the UI uses to read and mutate workout data. */
class WorkoutRepository(private val db: KettlebellDatabase) {

    val exercises: Flow<List<Exercise>> = db.exerciseDao().observeAll()
    val sessions: Flow<List<WorkoutSession>> = db.sessionDao().observeAll()
    val activeSession: Flow<WorkoutSession?> = db.sessionDao().observeActive()
    val sessionExercises: Flow<List<SessionExercise>> = db.sessionExerciseDao().observeAll()
    val sets: Flow<List<WorkoutSet>> = db.workoutSetDao().observeAll()

    /** Seed the built-in exercise library the first time the app runs. */
    suspend fun seedIfNeeded() {
        if (db.exerciseDao().count() == 0) {
            db.exerciseDao().insertAll(ExerciseCatalog.exercises)
        }
    }

    suspend fun startWorkout(title: String, now: Long): Long {
        val session = WorkoutSession(
            title = title,
            startedAt = now,
            finishedAt = null,
            dateEpochDay = LocalDate.now().toEpochDay(),
        )
        return db.sessionDao().insert(session)
    }

    /** Add an exercise to a session and pre-fill its planned sets from a recommendation. */
    suspend fun addExerciseToSession(
        sessionId: Long,
        exercise: Exercise,
        recommendation: Recommendation,
    ) {
        val position = db.sessionExerciseDao().maxPosition(sessionId) + 1
        val sessionExerciseId = db.sessionExerciseDao().insert(
            SessionExercise(sessionId = sessionId, exerciseId = exercise.id, position = position),
        )
        for (setNumber in 1..recommendation.sets) {
            db.workoutSetDao().insert(
                WorkoutSet(
                    sessionExerciseId = sessionExerciseId,
                    setNumber = setNumber,
                    weightKg = recommendation.weightKg,
                    targetReps = recommendation.repHigh,
                    reps = recommendation.repHigh,
                    completed = false,
                ),
            )
        }
    }

    suspend fun addSet(sessionExerciseId: Long, weightKg: Double, targetReps: Int) {
        val setNumber = db.workoutSetDao().maxSetNumber(sessionExerciseId) + 1
        db.workoutSetDao().insert(
            WorkoutSet(
                sessionExerciseId = sessionExerciseId,
                setNumber = setNumber,
                weightKg = weightKg,
                targetReps = targetReps,
                reps = targetReps,
                completed = false,
            ),
        )
    }

    suspend fun updateSet(set: WorkoutSet) = db.workoutSetDao().update(set)

    suspend fun deleteSet(set: WorkoutSet) = db.workoutSetDao().delete(set)

    suspend fun removeSessionExercise(sessionExercise: SessionExercise) =
        db.sessionExerciseDao().delete(sessionExercise)

    suspend fun finishWorkout(session: WorkoutSession, now: Long) =
        db.sessionDao().update(session.copy(finishedAt = now))

    suspend fun deleteSession(session: WorkoutSession) = db.sessionDao().delete(session)
}
