package com.kettlebell.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kettlebell.app.badges.BadgeState
import com.kettlebell.app.badges.Badges
import com.kettlebell.app.data.db.Exercise
import com.kettlebell.app.ui.components.ExerciseListItem
import com.kettlebell.app.ui.components.SectionHeader
import com.kettlebell.app.ui.components.StatCard
import com.kettlebell.app.ui.format.LocalWeightUnit
import com.kettlebell.app.ui.format.formatVolume
import com.kettlebell.app.ui.model.WorkoutUiState

@Composable
fun HomeScreen(
    state: WorkoutUiState,
    earnedBadgeIds: Set<String>,
    onStartWorkout: () -> Unit,
    onResumeWorkout: () -> Unit,
    onOpenExercise: (String) -> Unit,
    onSeeAllExercises: () -> Unit,
) {
    val featured = remember(state.exercises) { state.exercises.take(4) }
    val unit = LocalWeightUnit.current

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column {
                Text(
                    text = "Kettlebell",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Train smart. Progress every session.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            if (state.activeWorkout != null) {
                ResumeCard(
                    title = state.activeWorkout.session.title,
                    completed = state.activeWorkout.completedSets,
                    total = state.activeWorkout.totalSets,
                    onResume = onResumeWorkout,
                )
            } else {
                StartWorkoutHero(onStartWorkout)
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    value = state.stats.totalWorkouts.toString(),
                    label = "Workouts",
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    value = state.stats.workoutsThisWeek.toString(),
                    label = "This week",
                    modifier = Modifier.weight(1f),
                    container = MaterialTheme.colorScheme.primaryContainer,
                    content = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    value = formatVolume(state.stats.totalVolumeKg, unit),
                    label = "Total volume",
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    value = state.stats.totalSets.toString(),
                    label = "Sets logged",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionHeader("Exercises")
                TextButton(onClick = onSeeAllExercises) { Text("See all") }
            }
        }

        items(featured, key = { it.id }) { exercise ->
            ExerciseListItem(
                name = exercise.name,
                level = exercise.level,
                subtitle = exercise.primaryMuscles,
                onClick = { onOpenExercise(exercise.id) },
            )
        }

        item {
            val badges = remember(earnedBadgeIds) {
                Badges.all.map { BadgeState(it, it.id in earnedBadgeIds) }
            }
            val earnedCount = badges.count { it.earned }
            Column {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionHeader("Badges")
                    Text(
                        text = "$earnedCount / ${badges.size}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(12.dp))
                badges.chunked(3).forEach { rowBadges ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        rowBadges.forEach { badgeState ->
                            BadgeTile(badgeState, modifier = Modifier.weight(1f))
                        }
                        repeat(3 - rowBadges.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgeTile(state: BadgeState, modifier: Modifier = Modifier) {
    val container = if (state.earned) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    Surface(
        color = container,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = state.badge.emoji,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.alpha(if (state.earned) 1f else 0.35f),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = state.badge.title,
                style = MaterialTheme.typography.labelLarge,
                color = if (state.earned) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (state.earned) "Earned" else state.badge.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 3,
            )
        }
    }
}

@Composable
private fun StartWorkoutHero(onStartWorkout: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(24.dp)) {
            Icon(
                Icons.Filled.FitnessCenter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(36.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Ready to train?",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Text(
                text = "Start a workout and we'll recommend your weights.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onStartWorkout,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(6.dp))
                Text("Start Workout", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ResumeCard(
    title: String,
    completed: Int,
    total: Int,
    onResume: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = "Workout in progress",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = "$completed of $total sets done",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.height(14.dp))
            Button(onClick = onResume) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(6.dp))
                Text("Resume")
            }
        }
    }
}
