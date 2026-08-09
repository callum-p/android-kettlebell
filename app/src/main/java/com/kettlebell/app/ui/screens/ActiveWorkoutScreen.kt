package com.kettlebell.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kettlebell.app.data.db.Exercise
import com.kettlebell.app.data.db.WorkoutSet
import com.kettlebell.app.ui.WorkoutViewModel
import com.kettlebell.app.ui.components.EmptyState
import com.kettlebell.app.ui.components.LevelChip
import com.kettlebell.app.ui.components.Stepper
import com.kettlebell.app.ui.format.LocalWeightUnit
import com.kettlebell.app.ui.format.formatVolume
import com.kettlebell.app.ui.format.formatWeight
import com.kettlebell.app.ui.model.ActiveExercise
import com.kettlebell.app.ui.model.ActiveWorkout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    viewModel: WorkoutViewModel,
    activeWorkout: ActiveWorkout?,
    onAddExercise: () -> Unit,
    onOpenExercise: (String) -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
    onBack: () -> Unit,
) {
    var showDiscardDialog by remember { mutableStateOf(false) }
    val ownedBells by viewModel.ownedBells.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(activeWorkout?.session?.title ?: "Workout") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (activeWorkout != null) {
                        TextButton(onClick = { showDiscardDialog = true }) { Text("Discard") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        if (activeWorkout == null) {
            EmptyState(
                title = "No workout in progress",
                subtitle = "Head back and start a workout to begin logging sets.",
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { WorkoutSummary(activeWorkout) }

            items(activeWorkout.exercises, key = { it.sessionExercise.id }) { active ->
                ExerciseCard(
                    active = active,
                    bells = ownedBells,
                    onOpenExercise = { onOpenExercise(active.exercise.id) },
                    onWeightDown = { set -> viewModel.setWeight(set, viewModel.nextBellDown(set.weightKg)) },
                    onWeightUp = { set -> viewModel.setWeight(set, viewModel.nextBellUp(set.weightKg)) },
                    onRepsDown = { set -> viewModel.setReps(set, set.reps - 1) },
                    onRepsUp = { set -> viewModel.setReps(set, set.reps + 1) },
                    onToggle = { set -> viewModel.toggleSetCompleted(set, active.exercise) },
                    onDeleteSet = { set -> viewModel.deleteSet(set) },
                    onAddSet = { viewModel.addSet(active) },
                    onRemoveExercise = { viewModel.removeExercise(active) },
                )
            }

            item {
                OutlinedButton(onClick = onAddExercise, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add exercise")
                }
            }
            item {
                Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Finish workout")
                }
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard workout?") },
            text = { Text("This will delete the current workout and all its sets.") },
            confirmButton = {
                TextButton(onClick = { showDiscardDialog = false; onDiscard() }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun WorkoutSummary(workout: ActiveWorkout) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(20.dp)) {
            SummaryStat("${workout.completedSets}/${workout.totalSets}", "Sets done", Modifier.weight(1f))
            SummaryStat(workout.exercises.size.toString(), "Exercises", Modifier.weight(1f))
            SummaryStat(formatVolume(workout.totalVolumeKg, LocalWeightUnit.current), "Volume", Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ExerciseCard(
    active: ActiveExercise,
    bells: List<Double>,
    onOpenExercise: () -> Unit,
    onWeightDown: (WorkoutSet) -> Unit,
    onWeightUp: (WorkoutSet) -> Unit,
    onRepsDown: (WorkoutSet) -> Unit,
    onRepsUp: (WorkoutSet) -> Unit,
    onToggle: (WorkoutSet) -> Unit,
    onDeleteSet: (WorkoutSet) -> Unit,
    onAddSet: () -> Unit,
    onRemoveExercise: () -> Unit,
) {
    val allComplete = active.sets.isNotEmpty() && active.sets.all { it.completed }
    var expanded by remember { mutableStateOf(true) }
    // Auto-collapse once every set is done; expand again if that changes (e.g. a set is added
    // or un-completed). Manual toggles in between are preserved because this only fires on change.
    LaunchedEffect(allComplete) { expanded = !allComplete }

    Surface(
        color = if (allComplete) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().animateContentSize(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenExercise),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (allComplete) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = active.exercise.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = "View instructions",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    LevelChip(active.exercise.level)
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onRemoveExercise) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Remove exercise",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Rest ${active.exercise.defaultRestSeconds}s between sets",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))

                active.sets.forEach { set ->
                    SetRow(
                        set = set,
                        bells = bells,
                        onWeightDown = { onWeightDown(set) },
                        onWeightUp = { onWeightUp(set) },
                        onRepsDown = { onRepsDown(set) },
                        onRepsUp = { onRepsUp(set) },
                        onToggle = { onToggle(set) },
                        onDelete = { onDeleteSet(set) },
                    )
                }

                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onAddSet) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add set")
                }
            } else {
                Spacer(Modifier.height(6.dp))
                val unit = LocalWeightUnit.current
                val completed = active.sets.filter { it.completed }
                Text(
                    text = if (completed.isEmpty()) {
                        "${active.sets.size} sets · tap to expand"
                    } else {
                        completed.joinToString("   ") { "${formatWeight(it.weightKg, unit)}×${it.reps}" }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SetRow(
    set: WorkoutSet,
    bells: List<Double>,
    onWeightDown: () -> Unit,
    onWeightUp: () -> Unit,
    onRepsDown: () -> Unit,
    onRepsUp: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    val bg = if (set.completed) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    }
    val canWeightDown = set.weightKg > (bells.minOrNull() ?: set.weightKg)
    val canWeightUp = set.weightKg < (bells.maxOrNull() ?: set.weightKg)

    Surface(
        color = bg,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Set ${set.setNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Remove set",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Stepper(
                    value = formatWeight(set.weightKg, LocalWeightUnit.current),
                    label = "Weight",
                    onDecrement = onWeightDown,
                    onIncrement = onWeightUp,
                    canDecrement = canWeightDown,
                    canIncrement = canWeightUp,
                    modifier = Modifier.weight(1f),
                )
                Stepper(
                    value = "${set.reps}",
                    label = "Reps",
                    onDecrement = onRepsDown,
                    onIncrement = onRepsUp,
                    canDecrement = set.reps > 0,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(12.dp))
            if (set.completed) {
                Button(
                    onClick = onToggle,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Completed — tap to undo")
                }
            } else {
                OutlinedButton(
                    onClick = onToggle,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Circle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Mark set complete")
                }
            }
        }
    }
}
