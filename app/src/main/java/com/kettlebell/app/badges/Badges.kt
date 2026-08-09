package com.kettlebell.app.badges

import com.kettlebell.app.data.db.BodyPart
import com.kettlebell.app.ui.model.SessionSummary
import kotlin.math.max

/** A single achievement the user can earn. */
data class Badge(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
)

/** A badge paired with whether the user has earned it. */
data class BadgeState(
    val badge: Badge,
    val earned: Boolean,
)

/** The achievement catalogue and the logic that decides which are earned from workout history. */
object Badges {

    val all: List<Badge> = listOf(
        Badge("first_workout", "First Steps", "Finish your very first workout.", "👟"),
        Badge("streak_2", "Back at It", "Work out two days in a row.", "🔥"),
        Badge("workouts_3", "Getting Consistent", "Finish three workouts.", "📅"),
        Badge("high_five", "High Five", "Finish five workouts.", "✋"),
        Badge("perfect_ten", "Perfect Ten", "Finish ten workouts.", "🔟"),
        Badge("streak_7", "Seven-Day Swing", "Work out every day for a week.", "🗓️"),
        Badge("heavy_metal", "Heavy Metal", "Lift a 24 kg bell or heavier.", "🏋️"),
        Badge("century_club", "Century Club", "Log 100 completed sets.", "💯"),
        Badge("well_rounded", "Well Rounded", "Train every body part at least once.", "🌀"),
        Badge("half_tonne", "Half-Tonne Club", "Lift 5,000 kg of total volume.", "💪"),
    )

    fun evaluate(history: List<SessionSummary>): List<BadgeState> {
        val earned = earnedIds(history)
        return all.map { BadgeState(it, it.id in earned) }
    }

    fun earnedIds(history: List<SessionSummary>): Set<String> {
        if (history.isEmpty()) return emptySet()

        val workouts = history.size
        val longestStreak = longestDailyStreak(history)
        val totalSets = history.sumOf { it.completedSets }
        val totalVolume = history.sumOf { it.totalVolumeKg }
        val maxWeight = history
            .flatMap { it.exercises }
            .flatMap { it.sets }
            .filter { it.completed }
            .maxOfOrNull { it.weightKg } ?: 0.0
        val bodyPartsTrained = history
            .flatMap { it.exercises }
            .flatMap { it.exercise.bodyParts }
            .toSet()

        val earned = mutableSetOf<String>()
        if (workouts >= 1) earned += "first_workout"
        if (workouts >= 3) earned += "workouts_3"
        if (workouts >= 5) earned += "high_five"
        if (workouts >= 10) earned += "perfect_ten"
        if (longestStreak >= 2) earned += "streak_2"
        if (longestStreak >= 7) earned += "streak_7"
        if (maxWeight >= 24.0) earned += "heavy_metal"
        if (totalSets >= 100) earned += "century_club"
        if (BodyPart.entries.all { it in bodyPartsTrained }) earned += "well_rounded"
        if (totalVolume >= 5_000.0) earned += "half_tonne"
        return earned
    }

    private fun longestDailyStreak(history: List<SessionSummary>): Int {
        val days = history.map { it.session.dateEpochDay }.toSortedSet().toList()
        if (days.isEmpty()) return 0
        var longest = 1
        var current = 1
        for (i in 1 until days.size) {
            if (days[i] == days[i - 1] + 1) {
                current += 1
                longest = max(longest, current)
            } else {
                current = 1
            }
        }
        return longest
    }
}
