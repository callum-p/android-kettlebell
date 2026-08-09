package com.kettlebell.app.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kettlebell.app.ui.WorkoutViewModel
import com.kettlebell.app.ui.components.CelebrationOverlay
import com.kettlebell.app.ui.format.LocalWeightUnit
import com.kettlebell.app.ui.format.formatClock
import com.kettlebell.app.ui.model.RestTimerState
import com.kettlebell.app.ui.screens.ActiveWorkoutScreen
import com.kettlebell.app.ui.screens.BadgesScreen
import com.kettlebell.app.ui.screens.ExerciseDetailScreen
import com.kettlebell.app.ui.screens.RoutineEditorScreen
import com.kettlebell.app.ui.screens.ExercisesScreen
import com.kettlebell.app.ui.screens.HistoryScreen
import com.kettlebell.app.ui.screens.HomeScreen
import com.kettlebell.app.ui.screens.SettingsScreen
import com.kettlebell.app.ui.screens.StartWorkoutScreen
import kotlinx.coroutines.delay

object Routes {
    const val HOME = "home"
    const val EXERCISES = "exercises"
    const val HISTORY = "history"
    const val BADGES = "badges"
    const val SETTINGS = "settings"
    const val START = "start"
    const val ACTIVE = "active"
    const val EXERCISE_DETAIL = "exercise"
    const val EXERCISE_ARG = "exerciseId"
    const val ROUTINE_EDITOR = "routine_editor"
    const val ROUTINE_ARG = "routineId"

    fun exerciseDetail(id: String) = "$EXERCISE_DETAIL/$id"
    fun routineEditor(id: Long) = "$ROUTINE_EDITOR/$id"
}

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem(Routes.HOME, "Home", Icons.Filled.Home),
    TabItem(Routes.EXERCISES, "Exercises", Icons.Filled.FitnessCenter),
    TabItem(Routes.HISTORY, "History", Icons.Filled.History),
    TabItem(Routes.BADGES, "Badges", Icons.Filled.EmojiEvents),
    TabItem(Routes.SETTINGS, "Settings", Icons.Filled.Settings),
)

@Composable
fun KettlebellRoot() {
    val viewModel: WorkoutViewModel = viewModel(factory = WorkoutViewModel.Factory)
    val navController = rememberNavController()
    val restTimer by viewModel.restTimer.collectAsStateWithLifecycle()

    val celebrations by viewModel.celebrations.collectAsStateWithLifecycle()
    var showCelebration by remember { mutableStateOf(false) }
    LaunchedEffect(celebrations) {
        if (celebrations > 0) {
            showCelebration = true
            delay(3000)
            showCelebration = false
        }
    }

    val badgeBanner by viewModel.badgeBanner.collectAsStateWithLifecycle()
    LaunchedEffect(badgeBanner) {
        if (badgeBanner != null) {
            delay(3500)
            viewModel.clearBadgeBanner()
        }
    }

    val weightUnit by viewModel.weightUnit.collectAsStateWithLifecycle()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in tabs.map { it.route }

    CompositionLocalProvider(LocalWeightUnit provides weightUnit) {
    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (showBottomBar) {
                    BottomBar(currentRoute) { route -> navController.navigateTab(route) }
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.padding(padding),
            ) {
                appDestinations(navController, viewModel)
            }
        }

        restTimer?.let { timer ->
            RestTimerBar(
                timer = timer,
                onAdd = { viewModel.addRestTime(15) },
                onSkip = { viewModel.cancelRest() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .padding(bottom = if (showBottomBar) 80.dp else 0.dp),
            )
        }

        if (showCelebration) {
            CelebrationOverlay(modifier = Modifier.align(Alignment.Center))
        }

        badgeBanner?.let { text ->
            BadgeBanner(
                text = text,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp)
                    .padding(top = 48.dp),
            )
        }
    }
    }
}

@Composable
private fun BadgeBanner(text: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
        )
    }
}

private fun NavGraphBuilder.appDestinations(
    navController: NavHostController,
    viewModel: WorkoutViewModel,
) {
    // Each destination collects uiState inside its own composable scope so it stays live —
    // capturing it once at graph-construction time would leave every screen showing stale state.
    composable(Routes.HOME) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        HomeScreen(
            state = uiState,
            onStartWorkout = { navController.navigate(Routes.START) },
            onResumeWorkout = { navController.navigate(Routes.ACTIVE) },
            onOpenExercise = { navController.navigate(Routes.exerciseDetail(it)) },
            onSeeAllExercises = { navController.navigateTab(Routes.EXERCISES) },
        )
    }
    composable(Routes.BADGES) {
        val earnedBadges by viewModel.earnedBadges.collectAsStateWithLifecycle()
        BadgesScreen(earnedBadgeIds = earnedBadges)
    }
    composable(Routes.EXERCISES) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        ExercisesScreen(
            exercises = uiState.exercises,
            onOpenExercise = { navController.navigate(Routes.exerciseDetail(it)) },
        )
    }
    composable(Routes.HISTORY) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        HistoryScreen(
            history = uiState.history,
            onDelete = viewModel::deleteSession,
        )
    }
    composable(Routes.SETTINGS) {
        SettingsScreen(viewModel = viewModel)
    }
    composable(Routes.START) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val routines by viewModel.routines.collectAsStateWithLifecycle()
        StartWorkoutScreen(
            hasActiveWorkout = uiState.activeWorkout != null,
            routines = routines,
            onStart = { title, template ->
                viewModel.startWorkout(title, template)
                navController.popBackStack()
                navController.navigate(Routes.ACTIVE)
            },
            onStartBodyPart = { bodyPart ->
                viewModel.startBodyPartWorkout(bodyPart)
                navController.popBackStack()
                navController.navigate(Routes.ACTIVE)
            },
            onStartRoutine = { routine ->
                viewModel.startRoutine(routine)
                navController.popBackStack()
                navController.navigate(Routes.ACTIVE)
            },
            onCreateRoutine = { navController.navigate(Routes.routineEditor(-1L)) },
            onEditRoutine = { id -> navController.navigate(Routes.routineEditor(id)) },
            onBack = { navController.popBackStack() },
        )
    }
    composable(
        route = "${Routes.ROUTINE_EDITOR}/{${Routes.ROUTINE_ARG}}",
        arguments = listOf(navArgument(Routes.ROUTINE_ARG) { type = NavType.LongType }),
    ) { entry ->
        val id = entry.arguments?.getLong(Routes.ROUTINE_ARG) ?: -1L
        RoutineEditorScreen(
            viewModel = viewModel,
            routineId = id,
            onDone = { navController.popBackStack() },
        )
    }
    composable(Routes.ACTIVE) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        ActiveWorkoutScreen(
            viewModel = viewModel,
            activeWorkout = uiState.activeWorkout,
            onAddExercise = { navController.navigate(Routes.EXERCISES) },
            onOpenExercise = { navController.navigate(Routes.exerciseDetail(it)) },
            onFinish = {
                viewModel.finishWorkout()
                navController.popBackStack(Routes.HOME, inclusive = false)
            },
            onDiscard = {
                viewModel.discardWorkout()
                navController.popBackStack(Routes.HOME, inclusive = false)
            },
            onBack = { navController.popBackStack() },
        )
    }
    composable("${Routes.EXERCISE_DETAIL}/{${Routes.EXERCISE_ARG}}") { entry ->
        val exerciseId = entry.arguments?.getString(Routes.EXERCISE_ARG).orEmpty()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        ExerciseDetailScreen(
            viewModel = viewModel,
            exerciseId = exerciseId,
            hasActiveWorkout = uiState.activeWorkout != null,
            onBack = { navController.popBackStack() },
        )
    }
}

private fun NavHostController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun BottomBar(currentRoute: String?, onSelect: (String) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { onSelect(tab.route) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
            )
        }
    }
}

@Composable
private fun RestTimerBar(
    timer: RestTimerState,
    onAdd: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = if (timer.finished) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.primary
    }
    val content = if (timer.finished) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onPrimary
    }
    Surface(
        color = container,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (timer.finished) "Rest complete" else "Rest",
                    style = MaterialTheme.typography.labelLarge,
                    color = content.copy(alpha = 0.85f),
                )
                Text(
                    text = when {
                        !timer.finished -> formatClock(timer.remainingSeconds)
                        timer.exerciseName.isBlank() -> "Workout complete!"
                        else -> "Ready for ${timer.exerciseName}"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    color = content,
                )
            }
            if (!timer.finished) {
                Surface(
                    color = content.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .clickable(onClick = onAdd)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add 15 seconds", tint = content, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("15s", color = content, style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(Modifier.width(10.dp))
            }
            Button(onClick = onSkip) {
                Text(if (timer.finished) "Done" else "Skip")
            }
        }
    }
}
