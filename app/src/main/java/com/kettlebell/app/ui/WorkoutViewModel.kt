package com.kettlebell.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.kettlebell.app.KettlebellApp
import com.kettlebell.app.data.ExerciseCatalog
import com.kettlebell.app.data.ProgressionEngine
import com.kettlebell.app.data.Recommendation
import com.kettlebell.app.data.WorkoutRepository
import com.kettlebell.app.data.WorkoutTemplate
import com.kettlebell.app.data.db.Exercise
import com.kettlebell.app.data.db.SessionExercise
import com.kettlebell.app.data.db.WorkoutSession
import com.kettlebell.app.data.db.WorkoutSet
import com.kettlebell.app.debug.AppLogger
import com.kettlebell.app.ui.model.ActiveExercise
import com.kettlebell.app.ui.model.ActiveWorkout
import com.kettlebell.app.ui.model.CompletedExercise
import com.kettlebell.app.ui.model.ExerciseHistoryEntry
import com.kettlebell.app.ui.model.HomeStats
import com.kettlebell.app.ui.model.RestTimerState
import com.kettlebell.app.ui.model.SessionSummary
import com.kettlebell.app.ui.model.WorkoutUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

private data class RawData(
    val exercises: List<Exercise> = emptyList(),
    val sessions: List<WorkoutSession> = emptyList(),
    val sessionExercises: List<SessionExercise> = emptyList(),
    val sets: List<WorkoutSet> = emptyList(),
    val loaded: Boolean = false,
)

class WorkoutViewModel(private val repository: WorkoutRepository) : ViewModel() {

    private val rawData: StateFlow<RawData> = combine(
        repository.exercises,
        repository.sessions,
        repository.sessionExercises,
        repository.sets,
    ) { exercises, sessions, sessionExercises, sets ->
        RawData(exercises, sessions, sessionExercises, sets, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RawData())

    val uiState: StateFlow<WorkoutUiState> = rawData
        .map(::buildUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutUiState())

    private val _restTimer = MutableStateFlow<RestTimerState?>(null)
    val restTimer: StateFlow<RestTimerState?> = _restTimer.asStateFlow()
    private var restJob: Job? = null

    // ------------------------------------------------------------------ Derived state

    private fun buildUiState(data: RawData): WorkoutUiState {
        val exerciseById = data.exercises.associateBy { it.id }
        val setsByExercise = data.sets.groupBy { it.sessionExerciseId }

        fun activeWorkout(): ActiveWorkout? {
            val session = data.sessions.firstOrNull { it.finishedAt == null } ?: return null
            val active = data.sessionExercises
                .filter { it.sessionId == session.id }
                .sortedBy { it.position }
                .mapNotNull { se ->
                    val exercise = exerciseById[se.exerciseId] ?: return@mapNotNull null
                    ActiveExercise(
                        sessionExercise = se,
                        exercise = exercise,
                        sets = (setsByExercise[se.id] ?: emptyList()).sortedBy { it.setNumber },
                    )
                }
            return ActiveWorkout(session, active)
        }

        val finished = data.sessions.filter { it.finishedAt != null }
        val history = finished.map { session ->
            val completed = data.sessionExercises
                .filter { it.sessionId == session.id }
                .sortedBy { it.position }
                .mapNotNull { se ->
                    val exercise = exerciseById[se.exerciseId] ?: return@mapNotNull null
                    CompletedExercise(exercise, (setsByExercise[se.id] ?: emptyList()).sortedBy { it.setNumber })
                }
            SessionSummary(session, completed)
        }

        val today = LocalDate.now().toEpochDay()
        val stats = HomeStats(
            totalWorkouts = finished.size,
            workoutsThisWeek = finished.count { it.dateEpochDay >= today - 6 },
            totalVolumeKg = history.sumOf { it.totalVolumeKg },
            totalSets = history.sumOf { it.completedSets },
        )

        return WorkoutUiState(
            loading = !data.loaded,
            exercises = data.exercises,
            activeWorkout = activeWorkout(),
            history = history,
            stats = stats,
        )
    }

    /** The most recent finished session's completed sets for an exercise (drives progression). */
    private fun lastSessionSets(exerciseId: String): List<WorkoutSet> {
        val data = rawData.value
        val setsBySessionExercise = data.sets.groupBy { it.sessionExerciseId }
        val finishedSessions = data.sessions
            .filter { it.finishedAt != null }
            .sortedByDescending { it.startedAt }
        for (session in finishedSessions) {
            val matching = data.sessionExercises
                .filter { it.sessionId == session.id && it.exerciseId == exerciseId }
                .flatMap { setsBySessionExercise[it.id] ?: emptyList() }
                .filter { it.completed }
            if (matching.isNotEmpty()) return matching
        }
        return emptyList()
    }

    fun recommendationFor(exercise: Exercise): Recommendation =
        ProgressionEngine.recommend(exercise, lastSessionSets(exercise.id))

    fun exerciseHistory(exerciseId: String): List<ExerciseHistoryEntry> {
        val data = rawData.value
        val setsBySessionExercise = data.sets.groupBy { it.sessionExerciseId }
        return data.sessions
            .filter { it.finishedAt != null }
            .sortedByDescending { it.startedAt }
            .mapNotNull { session ->
                val sets = data.sessionExercises
                    .filter { it.sessionId == session.id && it.exerciseId == exerciseId }
                    .flatMap { setsBySessionExercise[it.id] ?: emptyList() }
                    .filter { it.completed }
                    .sortedBy { it.setNumber }
                if (sets.isEmpty()) null else ExerciseHistoryEntry(session.startedAt, sets)
            }
    }

    fun exerciseById(id: String): Exercise? =
        rawData.value.exercises.firstOrNull { it.id == id } ?: ExerciseCatalog.byId(id)

    // ------------------------------------------------------------------ Mutations

    private fun launchSafely(action: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onFailure { AppLogger.e("WorkoutViewModel", "Failed: $action", it) }
        }
    }

    /** Starts a workout (optionally from a template) and returns nothing — observe [uiState]. */
    fun startWorkout(title: String, template: WorkoutTemplate? = null) = launchSafely("startWorkout") {
        val sessionId = repository.startWorkout(title, System.currentTimeMillis())
        template?.exerciseIds?.forEach { exerciseId ->
            val exercise = exerciseById(exerciseId)
            if (exercise != null) {
                repository.addExerciseToSession(sessionId, exercise, recommendationFor(exercise))
            }
        }
    }

    fun addExerciseToActive(exercise: Exercise) = launchSafely("addExercise") {
        val session = rawData.value.sessions.firstOrNull { it.finishedAt == null } ?: return@launchSafely
        repository.addExerciseToSession(session.id, exercise, recommendationFor(exercise))
    }

    fun addSet(active: ActiveExercise) = launchSafely("addSet") {
        val template = active.sets.lastOrNull()
        val weight = template?.weightKg ?: ProgressionEngine.snap(active.exercise.startingWeightKg)
        val reps = template?.reps ?: active.exercise.repRangeHigh
        repository.addSet(active.sessionExercise.id, weight, reps)
    }

    fun updateSet(set: WorkoutSet) = launchSafely("updateSet") { repository.updateSet(set) }

    fun setWeight(set: WorkoutSet, weightKg: Double) =
        updateSet(set.copy(weightKg = weightKg.coerceAtLeast(0.0)))

    fun setReps(set: WorkoutSet, reps: Int) =
        updateSet(set.copy(reps = reps.coerceIn(0, 100)))

    fun toggleSetCompleted(set: WorkoutSet, exercise: Exercise) = launchSafely("toggleSet") {
        val nowComplete = !set.completed
        repository.updateSet(
            set.copy(
                completed = nowComplete,
                completedAt = if (nowComplete) System.currentTimeMillis() else null,
            ),
        )
        if (nowComplete) startRest(exercise.defaultRestSeconds, exercise.name)
    }

    fun deleteSet(set: WorkoutSet) = launchSafely("deleteSet") { repository.deleteSet(set) }

    fun removeExercise(active: ActiveExercise) = launchSafely("removeExercise") {
        repository.removeSessionExercise(active.sessionExercise)
    }

    fun finishWorkout() = launchSafely("finishWorkout") {
        val session = rawData.value.sessions.firstOrNull { it.finishedAt == null } ?: return@launchSafely
        repository.finishWorkout(session, System.currentTimeMillis())
        cancelRest()
    }

    fun discardWorkout() = launchSafely("discardWorkout") {
        val session = rawData.value.sessions.firstOrNull { it.finishedAt == null } ?: return@launchSafely
        repository.deleteSession(session)
        cancelRest()
    }

    fun deleteSession(summary: SessionSummary) = launchSafely("deleteSession") {
        repository.deleteSession(summary.session)
    }

    // ------------------------------------------------------------------ Rest timer

    fun startRest(seconds: Int, exerciseName: String) {
        if (seconds <= 0) return
        restJob?.cancel()
        _restTimer.value = RestTimerState(seconds, seconds, exerciseName)
        restJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1_000)
                remaining -= 1
                val current = _restTimer.value ?: break
                _restTimer.value = current.copy(remainingSeconds = remaining)
            }
        }
    }

    fun addRestTime(delta: Int) {
        val current = _restTimer.value ?: return
        val newRemaining = (current.remainingSeconds + delta).coerceAtLeast(0)
        _restTimer.value = current.copy(
            remainingSeconds = newRemaining,
            totalSeconds = maxOf(current.totalSeconds, newRemaining),
        )
    }

    fun cancelRest() {
        restJob?.cancel()
        restJob = null
        _restTimer.value = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as KettlebellApp
                return WorkoutViewModel(app.repository) as T
            }
        }
    }
}
