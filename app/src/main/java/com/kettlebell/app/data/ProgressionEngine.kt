package com.kettlebell.app.data

import com.kettlebell.app.data.db.Exercise
import com.kettlebell.app.data.db.WorkoutSet

/** A weight/rep suggestion for the next time an exercise is performed. */
data class Recommendation(
    val weightKg: Double,
    val sets: Int,
    val repLow: Int,
    val repHigh: Int,
    val rationale: String,
    val isFirstTime: Boolean,
)

/**
 * Implements a simple, proven "double progression" scheme:
 *  - Keep the weight until every working set reaches the top of the rep range.
 *  - Once you do, move up to the next kettlebell size.
 *  - If a session was clearly too heavy (every set below the low end), suggest sizing down.
 *
 * Weights snap to the standard bells in [ExerciseCatalog.BELLS].
 */
object ProgressionEngine {

    fun recommend(
        exercise: Exercise,
        lastSessionSets: List<WorkoutSet>,
        bells: List<Double> = ExerciseCatalog.BELLS,
        format: (Double) -> String = { defaultFormat(it) },
    ): Recommendation {
        val completed = lastSessionSets.filter { it.completed }
        if (completed.isEmpty()) {
            return Recommendation(
                weightKg = snap(exercise.startingWeightKg, bells),
                sets = exercise.defaultSets,
                repLow = exercise.repRangeLow,
                repHigh = exercise.repRangeHigh,
                rationale = "First time here — start light to nail your technique.",
                isFirstTime = true,
            )
        }

        // Progression is judged at the heaviest bell used last session (warm-ups aside).
        val topWeight = completed.maxOf { it.weightKg }
        val topSets = completed.filter { it.weightKg == topWeight }
        val minReps = topSets.minOf { it.reps }
        val maxReps = topSets.maxOf { it.reps }
        val hitEnoughSets = topSets.size >= exercise.defaultSets

        return when {
            hitEnoughSets && minReps >= exercise.repRangeHigh -> {
                val next = nextBellAbove(topWeight, bells)
                if (next > topWeight) {
                    Recommendation(
                        weightKg = next,
                        sets = exercise.defaultSets,
                        repLow = exercise.repRangeLow,
                        repHigh = exercise.repRangeHigh,
                        rationale = "You hit ${exercise.repRangeHigh}+ reps on every set at " +
                            "${format(topWeight)} — time to size up to ${format(next)}.",
                        isFirstTime = false,
                    )
                } else {
                    Recommendation(
                        weightKg = topWeight,
                        sets = exercise.defaultSets,
                        repLow = exercise.repRangeLow,
                        repHigh = exercise.repRangeHigh,
                        rationale = "You're crushing the heaviest bell — add reps or slow the tempo.",
                        isFirstTime = false,
                    )
                }
            }

            maxReps < exercise.repRangeLow -> {
                val down = nextBellBelow(topWeight, bells)
                Recommendation(
                    weightKg = down,
                    sets = exercise.defaultSets,
                    repLow = exercise.repRangeLow,
                    repHigh = exercise.repRangeHigh,
                    rationale = "Last session was a grind — drop to ${format(down)} and build back up.",
                    isFirstTime = false,
                )
            }

            else -> Recommendation(
                weightKg = topWeight,
                sets = exercise.defaultSets,
                repLow = exercise.repRangeLow,
                repHigh = exercise.repRangeHigh,
                rationale = "Stay at ${format(topWeight)} and push toward " +
                    "${exercise.repRangeHigh} reps on all sets before going heavier.",
                isFirstTime = false,
            )
        }
    }

    fun nextBellAbove(weight: Double, bells: List<Double> = ExerciseCatalog.BELLS): Double {
        val sorted = bells.sorted()
        return sorted.firstOrNull { it > weight + 0.001 } ?: sorted.lastOrNull() ?: weight
    }

    fun nextBellBelow(weight: Double, bells: List<Double> = ExerciseCatalog.BELLS): Double {
        val sorted = bells.sorted()
        return sorted.lastOrNull { it < weight - 0.001 } ?: sorted.firstOrNull() ?: weight
    }

    fun snap(weight: Double, bells: List<Double> = ExerciseCatalog.BELLS): Double =
        bells.minByOrNull { kotlin.math.abs(it - weight) } ?: weight

    private fun defaultFormat(weight: Double): String =
        if (weight % 1.0 == 0.0) "${weight.toInt()} kg" else "$weight kg"
}
