package com.kettlebell.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY name")
    fun observeAll(): Flow<List<Exercise>>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<Exercise>)
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM sessions WHERE finishedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeActive(): Flow<WorkoutSession?>

    @Insert
    suspend fun insert(session: WorkoutSession): Long

    @Update
    suspend fun update(session: WorkoutSession)

    @Delete
    suspend fun delete(session: WorkoutSession)
}

@Dao
interface SessionExerciseDao {
    @Query("SELECT * FROM session_exercises ORDER BY position")
    fun observeAll(): Flow<List<SessionExercise>>

    @Insert
    suspend fun insert(sessionExercise: SessionExercise): Long

    @Update
    suspend fun update(sessionExercise: SessionExercise)

    @Delete
    suspend fun delete(sessionExercise: SessionExercise)

    @Query("SELECT COALESCE(MAX(position), -1) FROM session_exercises WHERE sessionId = :sessionId")
    suspend fun maxPosition(sessionId: Long): Int
}

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines ORDER BY name")
    fun observeAll(): Flow<List<Routine>>

    @Insert
    suspend fun insert(routine: Routine): Long

    @Query("UPDATE routines SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Delete
    suspend fun delete(routine: Routine)
}

@Dao
interface RoutineExerciseDao {
    @Query("SELECT * FROM routine_exercises ORDER BY position")
    fun observeAll(): Flow<List<RoutineExercise>>

    @Insert
    suspend fun insert(routineExercise: RoutineExercise)

    @Query("DELETE FROM routine_exercises WHERE routineId = :routineId")
    suspend fun deleteForRoutine(routineId: Long)
}

@Dao
interface WorkoutSetDao {
    @Query("SELECT * FROM sets ORDER BY setNumber")
    fun observeAll(): Flow<List<WorkoutSet>>

    @Insert
    suspend fun insert(set: WorkoutSet): Long

    @Update
    suspend fun update(set: WorkoutSet)

    @Delete
    suspend fun delete(set: WorkoutSet)

    @Query("SELECT COALESCE(MAX(setNumber), 0) FROM sets WHERE sessionExerciseId = :sessionExerciseId")
    suspend fun maxSetNumber(sessionExerciseId: Long): Int
}
