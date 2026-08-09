package com.kettlebell.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.kettlebell.app.data.ExerciseCatalog
import com.kettlebell.app.data.db.Exercise
import com.kettlebell.app.ui.WorkoutViewModel
import com.kettlebell.app.ui.components.LevelChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineEditorScreen(
    viewModel: WorkoutViewModel,
    routineId: Long,
    onDone: () -> Unit,
) {
    val routines by viewModel.routines.collectAsStateWithLifecycle()
    val existing = remember(routines, routineId) {
        if (routineId > 0L) routines.firstOrNull { it.routine.id == routineId } else null
    }

    var name by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<List<Exercise>>(emptyList()) }
    var loaded by remember { mutableStateOf(routineId <= 0L) }
    var showPicker by remember { mutableStateOf(false) }

    LaunchedEffect(existing) {
        if (!loaded && existing != null) {
            name = existing.routine.name
            selected = existing.exercises
            loaded = true
        }
    }

    val canSave = name.isNotBlank() && selected.isNotEmpty()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(if (routineId > 0L) "Edit routine" else "New routine") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.saveRoutine(
                                id = if (routineId > 0L) routineId else null,
                                name = name,
                                exerciseIds = selected.map { it.id },
                            )
                            onDone()
                        },
                        enabled = canSave,
                    ) { Text("Save") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Routine name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Text(
                    text = "Exercises",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            if (selected.isEmpty()) {
                item {
                    Text(
                        text = "Add exercises to build your routine.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                itemsIndexed(selected, key = { _, ex -> ex.id }) { index, exercise ->
                    SelectedExerciseRow(
                        exercise = exercise,
                        canMoveUp = index > 0,
                        canMoveDown = index < selected.size - 1,
                        onMoveUp = { selected = selected.swap(index, index - 1) },
                        onMoveDown = { selected = selected.swap(index, index + 1) },
                        onRemove = { selected = selected.filterNot { it.id == exercise.id } },
                    )
                }
            }
            item {
                OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add exercise")
                }
            }
            if (routineId > 0L && existing != null) {
                item {
                    TextButton(
                        onClick = { viewModel.deleteRoutine(existing.routine); onDone() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Delete routine", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }

    if (showPicker) {
        ExercisePickerDialog(
            alreadyAdded = selected.map { it.id }.toSet(),
            onPick = { exercise ->
                if (selected.none { it.id == exercise.id }) selected = selected + exercise
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

private fun List<Exercise>.swap(a: Int, b: Int): List<Exercise> {
    if (a !in indices || b !in indices) return this
    val mutable = toMutableList()
    val tmp = mutable[a]
    mutable[a] = mutable[b]
    mutable[b] = tmp
    return mutable
}

@Composable
private fun SelectedExerciseRow(
    exercise: Exercise,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move up")
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move down")
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ExercisePickerDialog(
    alreadyAdded: Set<String>,
    onPick: (Exercise) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add exercise") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                itemsIndexed(ExerciseCatalog.exercises, key = { _, ex -> ex.id }) { _, exercise ->
                    val added = exercise.id in alreadyAdded
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !added) { onPick(exercise) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = exercise.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (added) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                            Text(
                                text = exercise.category,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (added) {
                            Text(
                                text = "Added",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            LevelChip(exercise.level)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
    )
}
