package com.kettlebell.app.data

import androidx.sqlite.db.SimpleSQLiteQuery
import com.kettlebell.app.data.db.Exercise
import com.kettlebell.app.data.db.KettlebellDatabase
import com.kettlebell.app.data.db.Routine
import com.kettlebell.app.data.db.RoutineExercise
import com.kettlebell.app.data.db.SessionExercise
import com.kettlebell.app.data.db.WorkoutSession
import com.kettlebell.app.data.db.WorkoutSet
import com.kettlebell.app.debug.AppLogger
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Single entry point the UI uses to read and mutate workout data.
 *
 * All reads are gated on [bootReady] so that a Google Drive restore (which overwrites the database
 * file on launch) completes before Room ever opens the database.
 */
class WorkoutRepository(
    private val db: KettlebellDatabase,
    private val bootReady: Deferred<Unit>,
) {

    val exercises: Flow<List<Exercise>> = gated { db.exerciseDao().observeAll() }
    val sessions: Flow<List<WorkoutSession>> = gated { db.sessionDao().observeAll() }
    val activeSession: Flow<WorkoutSession?> = gated { db.sessionDao().observeActive() }
    val sessionExercises: Flow<List<SessionExercise>> = gated { db.sessionExerciseDao().observeAll() }
    val sets: Flow<List<WorkoutSet>> = gated { db.workoutSetDao().observeAll() }
    val routines: Flow<List<Routine>> = gated { db.routineDao().observeAll() }
    val routineExercises: Flow<List<RoutineExercise>> = gated { db.routineExerciseDao().observeAll() }

    private fun <T> gated(source: () -> Flow<T>): Flow<T> = flow {
        bootReady.await()
        emitAll(source())
    }

    /** Seed the built-in exercise library the first time the app runs. */
    suspend fun seedIfNeeded() {
        bootReady.await()
        val existing = db.exerciseDao().count()
        AppLogger.i("Seed", "exercises table has $existing rows")
        if (existing == 0) {
            db.exerciseDao().insertAll(ExerciseCatalog.exercises)
            AppLogger.i("Seed", "seeded ${ExerciseCatalog.exercises.size} exercises")
        }
    }

    /** Flush the write-ahead log into the main database file so a file-level backup is consistent. */
    suspend fun checkpoint() = withContext(Dispatchers.IO) {
        runCatching {
            db.query(SimpleSQLiteQuery("PRAGMA wal_checkpoint(TRUNCATE)")).use { it.moveToFirst() }
        }.onFailure { AppLogger.e("WorkoutRepository", "Checkpoint failed", it) }
        Unit
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

    /** Swap the ordering positions of two exercises within a session. */
    suspend fun swapSessionExercisePositions(a: SessionExercise, b: SessionExercise) {
        db.sessionExerciseDao().update(a.copy(position = b.position))
        db.sessionExerciseDao().update(b.copy(position = a.position))
    }

    suspend fun finishWorkout(session: WorkoutSession, now: Long) =
        db.sessionDao().update(session.copy(finishedAt = now))

    suspend fun deleteSession(session: WorkoutSession) = db.sessionDao().delete(session)

    /** Create (id null) or update a routine and replace its ordered exercise list. */
    suspend fun saveRoutine(id: Long?, name: String, exerciseIds: List<String>) {
        val routineId = if (id == null || id <= 0L) {
            db.routineDao().insert(Routine(name = name, createdAt = System.currentTimeMillis()))
        } else {
            db.routineDao().rename(id, name)
            id
        }
        db.routineExerciseDao().deleteForRoutine(routineId)
        exerciseIds.forEachIndexed { index, exerciseId ->
            db.routineExerciseDao().insert(
                RoutineExercise(routineId = routineId, exerciseId = exerciseId, position = index),
            )
        }
    }

    suspend fun deleteRoutine(routine: Routine) = db.routineDao().delete(routine)
}
