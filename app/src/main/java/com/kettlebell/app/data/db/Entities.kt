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

/** Body areas an exercise targets, used to build body-part-focused workouts. */
enum class BodyPart(val label: String) {
    CHEST("Chest"),
    CORE("Core"),
    LEGS("Legs"),
    BACK("Back"),
    ARMS("Arms"),
    SHOULDERS("Shoulders"),
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
    val bodyParts: List<BodyPart> = emptyList(),
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
    /** Rate of perceived exertion, 1–10 (null if not logged). */
    val rpe: Int? = null,
    /** Optional free-text note for this set. */
    val notes: String? = null,
)

/** A user-created workout routine. */
@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

/** Membership + ordering of an exercise within a [Routine]. */
@Entity(
    tableName = "routine_exercises",
    foreignKeys = [
        ForeignKey(
            entity = Routine::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("routineId")],
)
data class RoutineExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val exerciseId: String,
    val position: Int,
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

    @TypeConverter
    fun bodyPartsToString(value: List<BodyPart>): String = value.joinToString(",") { it.name }

    @TypeConverter
    fun stringToBodyParts(value: String): List<BodyPart> =
        if (value.isEmpty()) emptyList() else value.split(",").map { BodyPart.valueOf(it) }

    private companion object {
        const val SEPARATOR = "|~|"
    }
}
