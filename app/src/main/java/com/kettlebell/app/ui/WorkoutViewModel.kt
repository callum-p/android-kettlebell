package com.kettlebell.app.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.kettlebell.app.KettlebellApp
import com.kettlebell.app.badges.Badge
import com.kettlebell.app.badges.Badges
import com.kettlebell.app.data.ExerciseCatalog
import com.kettlebell.app.data.ProgressionEngine
import com.kettlebell.app.data.Recommendation
import com.kettlebell.app.data.SettingsStore
import com.kettlebell.app.data.WorkoutRepository
import com.kettlebell.app.data.WorkoutTemplate
import com.kettlebell.app.data.db.BodyPart
import com.kettlebell.app.data.db.Exercise
import com.kettlebell.app.data.db.SessionExercise
import com.kettlebell.app.data.db.WorkoutSession
import com.kettlebell.app.data.db.WorkoutSet
import com.kettlebell.app.debug.AppLogger
import com.kettlebell.app.notify.RestNotifier
import com.kettlebell.app.progress.ExerciseBest
import com.kettlebell.app.progress.Progress
import com.kettlebell.app.sync.DatabaseBackup
import com.kettlebell.app.sync.DriveStatus
import com.kettlebell.app.sync.DriveSync
import com.kettlebell.app.ui.format.WeightUnit
import com.kettlebell.app.ui.format.formatWeight
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

class WorkoutViewModel(
    private val repository: WorkoutRepository,
    private val appContext: Context,
    private val driveSync: DriveSync,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    val driveStatus: StateFlow<DriveStatus> = driveSync.status
    val weightUnit: StateFlow<WeightUnit> = settingsStore.weightUnit

    fun setWeightUnit(unit: WeightUnit) = settingsStore.setWeightUnit(unit)

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
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            WorkoutUiState(exercises = ExerciseCatalog.exercises),
        )

    private val _restTimer = MutableStateFlow<RestTimerState?>(null)
    val restTimer: StateFlow<RestTimerState?> = _restTimer.asStateFlow()
    private var restJob: Job? = null

    /** Increments each time a celebration should play (finishing a workout or unlocking a badge). */
    private val _celebrations = MutableStateFlow(0)
    val celebrations: StateFlow<Int> = _celebrations.asStateFlow()

    private val badgePrefs = appContext.getSharedPreferences("badges", Context.MODE_PRIVATE)
    private val _badgeBanner = MutableStateFlow<String?>(null)
    val badgeBanner: StateFlow<String?> = _badgeBanner.asStateFlow()

    /** Badges the user has earned. Sticky — once earned, a badge stays even if data is later removed. */
    private val _earnedBadges = MutableStateFlow(
        badgePrefs.getStringSet(KEY_ACK_BADGES, null)?.toSet() ?: emptySet(),
    )
    val earnedBadges: StateFlow<Set<String>> = _earnedBadges.asStateFlow()

    private val recordPrefs = appContext.getSharedPreferences("records", Context.MODE_PRIVATE)

    init {
        // Watch for newly-earned badges and personal records and celebrate them.
        viewModelScope.launch {
            uiState.collect { state ->
                if (!state.loading) {
                    checkForNewBadges(state.history)
                    checkForNewRecords(state.history)
                }
            }
        }
    }

    private fun checkForNewRecords(history: List<com.kettlebell.app.ui.model.SessionSummary>) {
        val bests = Progress.bests(history)
        val editor = recordPrefs.edit()
        if (!recordPrefs.contains(KEY_PR_SEEDED)) {
            // Backfill existing bests silently so past lifts don't all fire as PRs.
            bests.forEach { editor.putFloat(prKey(it.exerciseId), it.oneRepMaxKg.toFloat()) }
            editor.putBoolean(KEY_PR_SEEDED, true).apply()
            return
        }
        val newRecords = mutableListOf<ExerciseBest>()
        for (best in bests) {
            val previous = recordPrefs.getFloat(prKey(best.exerciseId), 0f)
            if (best.oneRepMaxKg.toFloat() > previous + 0.01f) {
                newRecords += best
                editor.putFloat(prKey(best.exerciseId), best.oneRepMaxKg.toFloat())
            }
        }
        editor.apply()
        if (newRecords.isNotEmpty()) onRecordsSet(newRecords)
    }

    private fun onRecordsSet(records: List<ExerciseBest>) {
        _celebrations.value += 1
        val unit = weightUnit.value
        _badgeBanner.value = if (records.size == 1) {
            val r = records[0]
            "New PR: ${r.exerciseName} — ${formatWeight(r.weightKg, unit)} × ${r.reps}"
        } else {
            "${records.size} new personal records! 🏆"
        }
        records.forEach {
            RestNotifier.notifyPersonalRecord(
                appContext,
                "${it.exerciseName}: ${formatWeight(it.weightKg, unit)} × ${it.reps}",
            )
        }
    }

    private fun prKey(exerciseId: String) = "pr_$exerciseId"

    private fun checkForNewBadges(history: List<com.kettlebell.app.ui.model.SessionSummary>) {
        val qualifying = Badges.earnedIds(history)
        if (!badgePrefs.contains(KEY_ACK_BADGES)) {
            // First run with this feature — backfill from existing DB content silently so past
            // achievements are recorded without a storm of celebrations.
            badgePrefs.edit().putStringSet(KEY_ACK_BADGES, qualifying).apply()
            _earnedBadges.value = qualifying
            return
        }
        val union = _earnedBadges.value + qualifying
        if (union.size != _earnedBadges.value.size) {
            val newlyEarned = Badges.all.filter { it.id in (union - _earnedBadges.value) }
            badgePrefs.edit().putStringSet(KEY_ACK_BADGES, union).apply()
            _earnedBadges.value = union
            if (newlyEarned.isNotEmpty()) onBadgesUnlocked(newlyEarned)
        }
    }

    private fun onBadgesUnlocked(newlyEarned: List<Badge>) {
        _celebrations.value += 1
        _badgeBanner.value = if (newlyEarned.size == 1) {
            "Badge unlocked: ${newlyEarned[0].emoji} ${newlyEarned[0].title}"
        } else {
            "${newlyEarned.size} badges unlocked! 🎉"
        }
        newlyEarned.forEach { RestNotifier.notifyBadge(appContext, "${it.emoji} ${it.title}") }
    }

    fun clearBadgeBanner() {
        _badgeBanner.value = null
    }

    // ------------------------------------------------------------------ Derived state

    private fun buildUiState(data: RawData): WorkoutUiState {
        // The exercise catalogue is static data, so it's the source of truth here — the UI never
        // depends on an async database seed to display the library.
        val exerciseById = ExerciseCatalog.exercises.associateBy { it.id }
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
            exercises = ExerciseCatalog.exercises,
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

    fun recommendationFor(
        exercise: Exercise,
        unit: WeightUnit = weightUnit.value,
    ): Recommendation =
        ProgressionEngine.recommend(exercise, lastSessionSets(exercise.id)) { formatWeight(it, unit) }

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

    fun exerciseById(id: String): Exercise? = ExerciseCatalog.byId(id)

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

    /** Starts a workout focused on a body part, pre-filled with matching exercises. */
    fun startBodyPartWorkout(bodyPart: BodyPart) = launchSafely("startBodyPartWorkout") {
        val exercises = ExerciseCatalog.forBodyPart(bodyPart).take(5)
        val sessionId = repository.startWorkout("${bodyPart.label} Workout", System.currentTimeMillis())
        exercises.forEach { exercise ->
            repository.addExerciseToSession(sessionId, exercise, recommendationFor(exercise))
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
        if (nowComplete) {
            // Rest banner points at what's actually next: more sets of this exercise, otherwise
            // the next exercise with work left, otherwise the workout is done.
            startRest(exercise.defaultRestSeconds, nextUpExerciseName(set, nowComplete = true) ?: "")
        }
    }

    /**
     * Name of what to do after completing [afterSet]: the same exercise if it still has sets,
     * else the next exercise in order with incomplete sets, else null when the workout is done.
     * [nowComplete] overrides [afterSet]'s stored state since the DB write is still in flight.
     */
    private fun nextUpExerciseName(afterSet: WorkoutSet, nowComplete: Boolean): String? {
        val data = rawData.value
        val session = data.sessions.firstOrNull { it.finishedAt == null } ?: return null
        val ordered = data.sessionExercises
            .filter { it.sessionId == session.id }
            .sortedBy { it.position }
        val setsBySessionExercise = data.sets.groupBy { it.sessionExerciseId }
        fun isDone(s: WorkoutSet): Boolean = if (s.id == afterSet.id) nowComplete else s.completed

        val currentIndex = ordered.indexOfFirst { it.id == afterSet.sessionExerciseId }
        if (currentIndex < 0) return null

        val currentSets = setsBySessionExercise[afterSet.sessionExerciseId].orEmpty()
        if (currentSets.any { !isDone(it) }) {
            return ExerciseCatalog.byId(ordered[currentIndex].exerciseId)?.name
        }

        val searchOrder = (currentIndex + 1 until ordered.size) + (0 until currentIndex)
        for (index in searchOrder) {
            val sessionExercise = ordered[index]
            val sets = setsBySessionExercise[sessionExercise.id].orEmpty()
            if (sets.isEmpty() || sets.any { !isDone(it) }) {
                return ExerciseCatalog.byId(sessionExercise.exerciseId)?.name
            }
        }
        return null
    }

    fun deleteSet(set: WorkoutSet) = launchSafely("deleteSet") { repository.deleteSet(set) }

    fun removeExercise(active: ActiveExercise) = launchSafely("removeExercise") {
        repository.removeSessionExercise(active.sessionExercise)
    }

    fun finishWorkout() = launchSafely("finishWorkout") {
        val session = rawData.value.sessions.firstOrNull { it.finishedAt == null } ?: return@launchSafely
        repository.finishWorkout(session, System.currentTimeMillis())
        cancelRest()
        _celebrations.value += 1
        if (driveSync.isConnected()) {
            repository.checkpoint()
            driveSync.backup()
        }
    }

    // ------------------------------------------------------------------ Google Drive sync

    fun driveSignInIntent(): Intent = driveSync.signInIntent()

    fun onDriveSignInResult(data: Intent?) {
        val granted = driveSync.handleSignInResult(data)
        if (granted) {
            // Seed the remote copy with the current database immediately after connecting.
            launchSafely("driveInitialBackup") {
                repository.checkpoint()
                driveSync.backup()
            }
        }
    }

    fun disconnectDrive() = driveSync.disconnect()

    fun syncToDriveNow() = launchSafely("syncToDrive") {
        repository.checkpoint()
        driveSync.backup()
    }

    // ------------------------------------------------------------------ Local file backup

    fun exportBackup(uri: android.net.Uri) = launchSafely("exportBackup") {
        repository.checkpoint()
        DatabaseBackup.copyDatabaseToUri(appContext, uri)
    }

    /** Overwrites the local database from [uri] and restarts the app so Room reopens the new file. */
    fun importBackup(uri: android.net.Uri) = launchSafely("importBackup") {
        if (DatabaseBackup.importDatabaseFromUri(appContext, uri)) {
            DatabaseBackup.restartApp(appContext)
        }
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
        RestNotifier.cancel(appContext)
        _restTimer.value = RestTimerState(seconds, seconds, exerciseName)
        restJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1_000)
                remaining -= 1
                val current = _restTimer.value ?: break
                _restTimer.value = current.copy(remainingSeconds = remaining)
            }
            // Only reached on natural completion — a skip cancels this coroutine before here.
            val finishedName = _restTimer.value?.exerciseName
            val message = if (finishedName.isNullOrBlank()) {
                "Workout complete!"
            } else {
                "Ready for $finishedName"
            }
            RestNotifier.notifyRestComplete(appContext, message)
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
        RestNotifier.cancel(appContext)
    }

    companion object {
        private const val KEY_ACK_BADGES = "acknowledged"
        private const val KEY_PR_SEEDED = "pr_seeded"

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as KettlebellApp
                return WorkoutViewModel(
                    app.repository,
                    app.applicationContext,
                    app.driveSync,
                    app.settingsStore,
                ) as T
            }
        }
    }
}
