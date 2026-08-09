package com.kettlebell.app.progress

import com.kettlebell.app.ui.model.SessionSummary
import java.time.DayOfWeek
import java.time.LocalDate

/** The best-ever set for one exercise. */
data class ExerciseBest(
    val exerciseId: String,
    val exerciseName: String,
    val weightKg: Double,
    val reps: Int,
    val oneRepMaxKg: Double,
    val dateMillis: Long,
)

/** Total training volume for a single calendar week (Monday-anchored). */
data class WeekVolume(
    val weekStartEpochDay: Long,
    val volumeKg: Double,
)

/** Derived progress analytics computed from finished-session history. */
object Progress {

    /** Epley estimated one-rep max. */
    fun estimatedOneRepMax(weightKg: Double, reps: Int): Double =
        if (reps <= 1) weightKg else weightKg * (1.0 + reps / 30.0)

    /** The best set per exercise (by estimated 1RM), strongest first. */
    fun bests(history: List<SessionSummary>): List<ExerciseBest> {
        val byExercise = LinkedHashMap<String, ExerciseBest>()
        for (session in history) {
            for (completed in session.exercises) {
                for (set in completed.sets.filter { it.completed && it.reps > 0 }) {
                    val orm = estimatedOneRepMax(set.weightKg, set.reps)
                    val current = byExercise[completed.exercise.id]
                    if (current == null || orm > current.oneRepMaxKg) {
                        byExercise[completed.exercise.id] = ExerciseBest(
                            exerciseId = completed.exercise.id,
                            exerciseName = completed.exercise.name,
                            weightKg = set.weightKg,
                            reps = set.reps,
                            oneRepMaxKg = orm,
                            dateMillis = set.completedAt ?: session.session.startedAt,
                        )
                    }
                }
            }
        }
        return byExercise.values.sortedByDescending { it.oneRepMaxKg }
    }

    /** The best set for a single exercise, or null if never performed. */
    fun bestFor(history: List<SessionSummary>, exerciseId: String): ExerciseBest? =
        bests(history).firstOrNull { it.exerciseId == exerciseId }

    /** Top working weight for an exercise per session it appeared in, oldest first (for trend lines). */
    fun topWeightTrend(history: List<SessionSummary>, exerciseId: String): List<Double> =
        history
            .sortedBy { it.session.startedAt }
            .mapNotNull { session ->
                session.exercises
                    .filter { it.exercise.id == exerciseId }
                    .flatMap { it.sets }
                    .filter { it.completed }
                    .maxOfOrNull { it.weightKg }
            }

    /** Training volume for the last [weeks] weeks (Monday-anchored), oldest first, including empty weeks. */
    fun weeklyVolume(history: List<SessionSummary>, todayEpochDay: Long, weeks: Int): List<WeekVolume> {
        val thisMonday = mondayOf(todayEpochDay)
        val buckets = LinkedHashMap<Long, Double>()
        for (offset in (weeks - 1) downTo 0) {
            buckets[thisMonday - offset * 7L] = 0.0
        }
        for (session in history) {
            val monday = mondayOf(session.session.dateEpochDay)
            if (buckets.containsKey(monday)) {
                buckets[monday] = (buckets[monday] ?: 0.0) + session.totalVolumeKg
            }
        }
        return buckets.map { WeekVolume(it.key, it.value) }
    }

    /** Epoch-days on which at least one workout was finished. */
    fun activeDays(history: List<SessionSummary>): Set<Long> =
        history.map { it.session.dateEpochDay }.toSet()

    /** Epoch-day of the Monday that starts the week containing [epochDay]. */
    fun weekStart(epochDay: Long): Long {
        val date = LocalDate.ofEpochDay(epochDay)
        return date.minusDays((date.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong()).toEpochDay()
    }

    private fun mondayOf(epochDay: Long): Long = weekStart(epochDay)
}
