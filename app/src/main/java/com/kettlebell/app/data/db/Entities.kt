package com.kettlebell.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

/** Difficulty tiers for exercises and workout templates. */
enum class Level(val label: String) {
    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced"),
}

/**
 * A kettlebell exercise from the built-in catalogue. Rows are seeded on first launch and are
 * effectively read-only for the user.
 */
@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey val id: String,
    val name: String,
    val level: Level,
    val category: String,
    val primaryMuscles: String,
    val description: String,
    val instructions: List<String>,
    val youtubeUrl: String = "",
    val repRangeLow: Int,
    val repRangeHigh: Int,
    val defaultSets: Int,
    val defaultRestSeconds: Int,
    val startingWeightKg: Double,
)

/** One workout the user performed (or is currently performing when [finishedAt] is null). */
@Entity(tableName = "sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val dateEpochDay: Long,
)

/** Membership + ordering of an exercise within a session (persists even before any set is logged). */
@Entity(
    tableName = "session_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
data class SessionExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: String,
    val position: Int,
)

/** A single working set belonging to a [SessionExercise]. */
@Entity(
    tableName = "sets",
    foreignKeys = [
        ForeignKey(
            entity = SessionExercise::class,
            parentColumns = ["id"],
            childColumns = ["sessionExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionExerciseId")],
)
data class WorkoutSet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionExerciseId: Long,
    val setNumber: Int,
    val weightKg: Double,
    val targetReps: Int,
    val reps: Int,
    val completed: Boolean,
    val completedAt: Long? = null,
)

/** Room type converters for the small non-primitive fields used above. */
class Converters {
    @TypeConverter
    fun levelToString(level: Level): String = level.name

    @TypeConverter
    fun stringToLevel(value: String): Level = Level.valueOf(value)

    @TypeConverter
    fun instructionsToString(value: List<String>): String = value.joinToString(SEPARATOR)

    @TypeConverter
    fun stringToInstructions(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(SEPARATOR)

    private companion object {
        const val SEPARATOR = "|~|"
    }
}
