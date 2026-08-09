package com.kettlebell.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kettlebell.app.progress.ExerciseBest
import com.kettlebell.app.progress.Progress
import com.kettlebell.app.progress.WeekVolume
import com.kettlebell.app.ui.components.BarChart
import com.kettlebell.app.ui.components.EmptyState
import com.kettlebell.app.ui.components.FrequencyHeatmap
import com.kettlebell.app.ui.format.LocalWeightUnit
import com.kettlebell.app.ui.format.WeightUnit
import com.kettlebell.app.ui.format.formatDate
import com.kettlebell.app.ui.format.formatTime
import com.kettlebell.app.ui.format.formatVolume
import com.kettlebell.app.ui.format.formatWeight
import com.kettlebell.app.ui.model.SessionSummary
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    history: List<SessionSummary>,
    onDelete: (SessionSummary) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<SessionSummary?>(null) }
    var showOverview by remember { mutableStateOf(true) }
    val unit = LocalWeightUnit.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Progress") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        if (history.isEmpty()) {
            EmptyState(
                title = "No workouts yet",
                subtitle = "Finished workouts, charts and personal records will show up here.",
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            return@Scaffold
        }

        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TabChip("Overview", showOverview) { showOverview = true }
                TabChip("Sessions", !showOverview) { showOverview = false }
            }
            if (showOverview) {
                OverviewList(history, unit)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(history, key = { it.session.id }) { summary ->
                        SessionCard(summary, unit, onDelete = { pendingDelete = summary })
                    }
                }
            }
        }
    }

    pendingDelete?.let { summary ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete workout?") },
            text = { Text("This will permanently remove this workout from your history.") },
            confirmButton = {
                TextButton(onClick = { onDelete(summary); pendingDelete = null }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}

@Composable
private fun OverviewList(history: List<SessionSummary>, unit: WeightUnit) {
    val today = remember { LocalDate.now().toEpochDay() }
    val weeks = remember(history) { Progress.weeklyVolume(history, today, 8) }
    val activeDays = remember(history) { Progress.activeDays(history) }
    val bests = remember(history) { Progress.bests(history) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { WeeklyVolumeCard(weeks, unit) }
        item { FrequencyCard(activeDays, today) }
        item {
            Text(
                text = "Personal records",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        if (bests.isEmpty()) {
            item {
                Text(
                    text = "Complete some sets to start setting records.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(bests, key = { it.exerciseId }) { best -> RecordRow(best, unit) }
        }
    }
}

@Composable
private fun WeeklyVolumeCard(weeks: List<WeekVolume>, unit: WeightUnit) {
    val thisWeek = weeks.lastOrNull()?.volumeKg ?: 0.0
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = "Weekly volume",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "This week: ${formatVolume(thisWeek, unit)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            BarChart(
                values = weeks.map { it.volumeKg.toFloat() },
                labels = weeks.map {
                    val d = LocalDate.ofEpochDay(it.weekStartEpochDay)
                    "${d.dayOfMonth}/${d.monthValue}"
                },
            )
        }
    }
}

@Composable
private fun FrequencyCard(activeDays: Set<Long>, today: Long) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = "Activity",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Last 12 weeks",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            FrequencyHeatmap(activeDays = activeDays, todayEpochDay = today, weeks = 12)
        }
    }
}

@Composable
private fun RecordRow(best: ExerciseBest, unit: WeightUnit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = best.exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Best: ${formatWeight(best.weightKg, unit)} × ${best.reps}  ·  ${formatDate(best.dateMillis)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "~${formatWeight(best.oneRepMaxKg, unit)}\n1RM",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SessionCard(summary: SessionSummary, unit: WeightUnit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().animateContentSize(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = summary.session.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${formatDate(summary.session.startedAt)} · ${formatTime(summary.session.startedAt)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.DeleteOutline,
                        contentDescription = "Delete workout",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Metric("${summary.completedSets}", "sets")
                Metric(formatVolume(summary.totalVolumeKg, unit), "volume")
                summary.durationMinutes?.let { Metric("${it}m", "duration") }
            }

            if (expanded) {
                Spacer(Modifier.height(16.dp))
                summary.exercises.forEach { completed ->
                    Column(Modifier.padding(vertical = 6.dp)) {
                        Text(
                            text = completed.exercise.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        val done = completed.sets.filter { it.completed }
                        Text(
                            text = if (done.isEmpty()) {
                                "No sets completed"
                            } else {
                                done.joinToString("   ") { "${formatWeight(it.weightKg, unit)}×${it.reps}" }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Metric(value: String, label: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 3.dp),
        )
    }
}
