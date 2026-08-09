package com.kettlebell.app.ui.screens

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private enum class TimerMode(val label: String, val blurb: String) {
    EMOM("EMOM", "Every minute, on the minute"),
    INTERVAL("Interval", "Work / rest rounds"),
    AMRAP("AMRAP", "As many rounds as possible"),
}

private enum class SegmentKind { WORK, REST }

private data class TimerSegment(val label: String, val seconds: Int, val kind: SegmentKind)

private enum class Phase { SETUP, RUNNING, PAUSED, DONE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimedWorkoutScreen(onBack: () -> Unit) {
    var mode by remember { mutableStateOf(TimerMode.EMOM) }

    // Config
    var emomRounds by remember { mutableIntStateOf(10) }
    var workSeconds by remember { mutableIntStateOf(40) }
    var restSeconds by remember { mutableIntStateOf(20) }
    var intervalRounds by remember { mutableIntStateOf(8) }
    var amrapMinutes by remember { mutableIntStateOf(12) }

    // Runtime
    var phase by remember { mutableStateOf(Phase.SETUP) }
    var segments by remember { mutableStateOf<List<TimerSegment>>(emptyList()) }
    var index by remember { mutableIntStateOf(0) }
    var remaining by remember { mutableIntStateOf(0) }
    var amrapRounds by remember { mutableIntStateOf(0) }

    val context = LocalContext.current
    fun buzz(long: Boolean) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(android.os.VibratorManager::class.java))?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        } ?: return
        runCatching {
            vibrator.vibrate(VibrationEffect.createOneShot(if (long) 500L else 200L, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    fun buildSegments(): List<TimerSegment> = when (mode) {
        TimerMode.EMOM -> (1..emomRounds).map {
            TimerSegment("Round $it / $emomRounds", 60, SegmentKind.WORK)
        }
        TimerMode.INTERVAL -> (1..intervalRounds).flatMap { round ->
            buildList {
                add(TimerSegment("Work · $round / $intervalRounds", workSeconds, SegmentKind.WORK))
                if (round < intervalRounds) add(TimerSegment("Rest", restSeconds, SegmentKind.REST))
            }
        }
        TimerMode.AMRAP -> listOf(TimerSegment("AMRAP", amrapMinutes * 60, SegmentKind.WORK))
    }

    fun start() {
        val built = buildSegments()
        if (built.isEmpty()) return
        segments = built
        index = 0
        remaining = built.first().seconds
        amrapRounds = 0
        phase = Phase.RUNNING
    }

    fun reset() {
        phase = Phase.SETUP
        segments = emptyList()
        index = 0
        remaining = 0
    }

    val isRunning = phase == Phase.RUNNING
    LaunchedEffect(isRunning, index) {
        if (!isRunning) return@LaunchedEffect
        while (remaining > 0) {
            delay(1000)
            remaining--
        }
        // Segment finished.
        if (index < segments.lastIndex) {
            buzz(long = false)
            index++
            remaining = segments[index].seconds
        } else {
            buzz(long = true)
            phase = Phase.DONE
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Timed workout") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        if (phase == Phase.SETUP) {
            SetupContent(
                modifier = Modifier.fillMaxSize().padding(padding),
                mode = mode,
                onModeChange = { mode = it },
                emomRounds = emomRounds,
                onEmomRounds = { emomRounds = it.coerceIn(1, 60) },
                workSeconds = workSeconds,
                onWorkSeconds = { workSeconds = it.coerceIn(5, 600) },
                restSeconds = restSeconds,
                onRestSeconds = { restSeconds = it.coerceIn(0, 600) },
                intervalRounds = intervalRounds,
                onIntervalRounds = { intervalRounds = it.coerceIn(1, 60) },
                amrapMinutes = amrapMinutes,
                onAmrapMinutes = { amrapMinutes = it.coerceIn(1, 60) },
                onStart = { start() },
            )
        } else {
            RunningContent(
                modifier = Modifier.fillMaxSize().padding(padding),
                mode = mode,
                segment = segments.getOrNull(index),
                remaining = remaining,
                done = phase == Phase.DONE,
                roundLabel = "${index + 1} / ${segments.size}",
                amrapRounds = amrapRounds,
                paused = phase == Phase.PAUSED,
                onPauseResume = {
                    phase = if (phase == Phase.RUNNING) Phase.PAUSED else Phase.RUNNING
                },
                onCountRound = { amrapRounds++ },
                onReset = { reset() },
            )
        }
    }
}

@Composable
private fun SetupContent(
    modifier: Modifier,
    mode: TimerMode,
    onModeChange: (TimerMode) -> Unit,
    emomRounds: Int,
    onEmomRounds: (Int) -> Unit,
    workSeconds: Int,
    onWorkSeconds: (Int) -> Unit,
    restSeconds: Int,
    onRestSeconds: (Int) -> Unit,
    intervalRounds: Int,
    onIntervalRounds: (Int) -> Unit,
    amrapMinutes: Int,
    onAmrapMinutes: (Int) -> Unit,
    onStart: () -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimerMode.entries.forEach { m ->
                    FilterChip(
                        selected = m == mode,
                        onClick = { onModeChange(m) },
                        label = { Text(m.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                }
            }
        }
        item {
            Text(
                text = mode.blurb,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    when (mode) {
                        TimerMode.EMOM -> {
                            Stepper("Rounds (minutes)", emomRounds.toString(), { onEmomRounds(emomRounds - 1) }, { onEmomRounds(emomRounds + 1) })
                        }
                        TimerMode.INTERVAL -> {
                            Stepper("Work (sec)", workSeconds.toString(), { onWorkSeconds(workSeconds - 5) }, { onWorkSeconds(workSeconds + 5) })
                            Stepper("Rest (sec)", restSeconds.toString(), { onRestSeconds(restSeconds - 5) }, { onRestSeconds(restSeconds + 5) })
                            Stepper("Rounds", intervalRounds.toString(), { onIntervalRounds(intervalRounds - 1) }, { onIntervalRounds(intervalRounds + 1) })
                        }
                        TimerMode.AMRAP -> {
                            Stepper("Duration (min)", amrapMinutes.toString(), { onAmrapMinutes(amrapMinutes - 1) }, { onAmrapMinutes(amrapMinutes + 1) })
                        }
                    }
                }
            }
        }
        item {
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Start")
            }
        }
    }
}

@Composable
private fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onMinus) {
            Icon(Icons.Filled.Remove, contentDescription = "Decrease")
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(56.dp),
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = onPlus) {
            Icon(Icons.Filled.Add, contentDescription = "Increase")
        }
    }
}

@Composable
private fun RunningContent(
    modifier: Modifier,
    mode: TimerMode,
    segment: TimerSegment?,
    remaining: Int,
    done: Boolean,
    roundLabel: String,
    amrapRounds: Int,
    paused: Boolean,
    onPauseResume: () -> Unit,
    onCountRound: () -> Unit,
    onReset: () -> Unit,
) {
    val isRest = segment?.kind == SegmentKind.REST
    val targetColor = when {
        done -> MaterialTheme.colorScheme.primary
        isRest -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    val ringColor by animateColorAsState(targetColor, label = "ring")

    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = if (done) "Done 🎉" else segment?.label.orEmpty(),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                color = ringColor.copy(alpha = 0.12f),
                shape = CircleShape,
                modifier = Modifier.fillMaxSize(),
            ) {}
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatMmSs(remaining),
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = ringColor,
                )
                if (!done) {
                    Text(
                        text = if (mode == TimerMode.AMRAP) "remaining" else roundLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))

        if (mode == TimerMode.AMRAP) {
            Text(
                text = "Rounds: $amrapRounds",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(12.dp))
            if (!done) {
                Button(onClick = onCountRound, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Count a round")
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!done) {
                OutlinedButton(onClick = onPauseResume) {
                    Icon(
                        if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (paused) "Resume" else "Pause")
                }
            }
            Button(
                onClick = onReset,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            ) {
                Icon(Icons.Filled.Replay, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (done) "New timer" else "Reset")
            }
        }
    }
}

private fun formatMmSs(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    return "%d:%02d".format(safe / 60, safe % 60)
}
