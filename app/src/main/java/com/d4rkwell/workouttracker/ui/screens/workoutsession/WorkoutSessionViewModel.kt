package com.d4rkwell.workouttracker.ui.screens.workoutsession

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4rkwell.workouttracker.data.local.entity.DayWithExercises
import com.d4rkwell.workouttracker.data.local.entity.ExerciseEntity
import com.d4rkwell.workouttracker.data.local.entity.LoggedSetEntity
import com.d4rkwell.workouttracker.data.local.entity.SetPart
import com.d4rkwell.workouttracker.data.local.entity.toExtraPartsString
import com.d4rkwell.workouttracker.data.repository.WorkoutRepository
import com.d4rkwell.workouttracker.timer.TimerController
import com.d4rkwell.workouttracker.timer.TimerType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Which stage of the session the user is currently in. */
enum class SessionStage { WARMUP, EXERCISES, FINISHED }

/** One extra weight×reps part being entered for a multi-part (drop) set, in draft form. */
data class DraftSetPart(
    val weight: String = "",
    val reps: String = ""
)

data class SetEntryDraft(
    val weight: String = "",
    val reps: String = "",
    val note: String = "",
    // Extra parts beyond the first weight/reps above — present only when
    // the person has turned on multi-part entry for this set (a drop set:
    // e.g. 25×4 then 20×6 logged as one set).
    val extraParts: List<DraftSetPart> = emptyList()
) {
    val isMultiPart: Boolean get() = extraParts.isNotEmpty()
}

data class WorkoutSessionUiState(
    val dayTitle: String = "",
    val stage: SessionStage = SessionStage.WARMUP,
    val warmupMinutes: Int = 5,
    val exercises: List<ExerciseEntity> = emptyList(),
    val currentExerciseIndex: Int = 0,
    val loggedSetsForCurrentExercise: List<LoggedSetEntity> = emptyList(),
    val previousBestForCurrentExercise: LoggedSetEntity? = null,
    val sessionId: Long = -1,
    val sessionStartedAtMillis: Long = 0,
    val skippedWarmupExerciseIds: Set<Long> = emptySet(),
    val isLoaded: Boolean = false
) {
    val currentExercise: ExerciseEntity?
        get() = exercises.getOrNull(currentExerciseIndex)

    val loggedWarmupSets: List<LoggedSetEntity>
        get() = loggedSetsForCurrentExercise.filter { it.isWarmup }

    val loggedWorkingSets: List<LoggedSetEntity>
        get() = loggedSetsForCurrentExercise.filter { !it.isWarmup }

    /** Whether the user is still expected to log warmup sets for the current exercise. */
    val isInWarmupSubstage: Boolean
        get() {
            val exercise = currentExercise ?: return false
            if (exercise.exerciseId in skippedWarmupExerciseIds) return false
            return exercise.warmupSets > 0 && loggedWarmupSets.size < exercise.warmupSets
        }

    val isCurrentExerciseComplete: Boolean
        get() {
            val exercise = currentExercise ?: return false
            val warmupSatisfied = exercise.exerciseId in skippedWarmupExerciseIds ||
                loggedWarmupSets.size >= exercise.warmupSets
            return warmupSatisfied && loggedWorkingSets.size >= exercise.workingSets
        }

    val isLastExercise: Boolean
        get() = currentExerciseIndex >= exercises.size - 1
}

class WorkoutSessionViewModel(
    private val repository: WorkoutRepository,
    private val timerController: TimerController,
    private val dayId: Long
) : ViewModel() {

    private val _stage = MutableStateFlow(SessionStage.WARMUP)
    private val _currentExerciseIndex = MutableStateFlow(0)
    private val _sessionId = MutableStateFlow(-1L)
    private val _sessionStartedAt = MutableStateFlow(0L)
    private val _previousBest = MutableStateFlow<LoggedSetEntity?>(null)
    private val _skippedWarmupExerciseIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _draft = MutableStateFlow(SetEntryDraft())
    val draft: StateFlow<SetEntryDraft> = _draft.asStateFlow()

    val timerState = timerController.state

    private val dayFlow: StateFlow<DayWithExercises?> = repository.observeDayWithExercises(dayId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Logged sets for whichever exercise is currently active, refreshed live as the
    // session/exercise selection changes. Feeds into uiState below so that
    // state.isInWarmupSubstage (used by logCurrentSet) reflects reality rather
    // than always seeing an empty list.
    val loggedSetsForCurrentExercise: StateFlow<List<LoggedSetEntity>> =
        combine(_sessionId, _currentExerciseIndex, dayFlow) { sessionId, index, day ->
            Pair(sessionId, day?.exercises?.getOrNull(index)?.exerciseId)
        }.flatMapLatest { pair ->
            val sessionId = pair.first
            val exerciseId = pair.second
            if (sessionId > 0 && exerciseId != null) {
                repository.observeSetsForExerciseInSession(sessionId, exerciseId)
            } else {
                emptyFlow()
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<WorkoutSessionUiState> = combine(
        dayFlow, _stage, _currentExerciseIndex, _sessionId, _sessionStartedAt, _previousBest,
        loggedSetsForCurrentExercise, _skippedWarmupExerciseIds
    ) { values ->
        val day = values[0] as DayWithExercises?
        val stage = values[1] as SessionStage
        val exerciseIndex = values[2] as Int
        val sessionId = values[3] as Long
        val startedAt = values[4] as Long
        val previousBest = values[5] as LoggedSetEntity?
        @Suppress("UNCHECKED_CAST")
        val loggedSets = values[6] as List<LoggedSetEntity>
        @Suppress("UNCHECKED_CAST")
        val skippedWarmupIds = values[7] as Set<Long>
        val exercises = day?.exercises.orEmpty()
        WorkoutSessionUiState(
            dayTitle = day?.day?.title ?: "",
            stage = stage,
            warmupMinutes = day?.day?.warmupMinutes ?: 5,
            exercises = exercises,
            currentExerciseIndex = exerciseIndex,
            loggedSetsForCurrentExercise = loggedSets,
            previousBestForCurrentExercise = previousBest,
            sessionId = sessionId,
            sessionStartedAtMillis = startedAt,
            skippedWarmupExerciseIds = skippedWarmupIds,
            isLoaded = day != null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WorkoutSessionUiState())

    /** Total sets logged across the whole session (all exercises), for the finish summary. */
    val totalLoggedSetsForSession: StateFlow<Int> = _sessionId.flatMapLatest { sessionId ->
        if (sessionId > 0) {
            repository.observeSessionWithLoggedSets(sessionId)
        } else {
            emptyFlow()
        }
    }.map { it?.loggedSets?.size ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        viewModelScope.launch {
            val id = repository.startOrResumeSession(dayId)
            val session = repository.getSession(id)
            _sessionId.value = id
            _sessionStartedAt.value = session?.startedAtEpochMillis ?: System.currentTimeMillis()
        }
        // Keep "previous best" in sync with whichever exercise is currently active.
        // This is specifically the heaviest WORKING set from the last time this
        // exercise was logged (not just whatever set was most recent, which could
        // have been a lighter warmup or a lighter early working set) — so it's a
        // meaningful target to beat for progressive overload.
        viewModelScope.launch {
            combine(_currentExerciseIndex, dayFlow) { index, day ->
                day?.exercises?.getOrNull(index)?.exerciseId
            }.collect { exerciseId ->
                _previousBest.value = if (exerciseId != null) {
                    repository.getLastTopWorkingSet(exerciseId)
                } else null
            }
        }
    }

    fun startWarmupTimer() {
        val minutes = uiState.value.warmupMinutes
        timerController.start(TimerType.WARMUP, minutes * 60, "Warm-up")
    }

    fun skipWarmup() {
        timerController.skip()
        _stage.value = SessionStage.EXERCISES
    }

    fun finishWarmupAndBegin() {
        timerController.skip()
        _stage.value = SessionStage.EXERCISES
    }

    fun updateDraftWeight(value: String) {
        _draft.value = _draft.value.copy(weight = value)
    }

    fun updateDraftReps(value: String) {
        _draft.value = _draft.value.copy(reps = value.filter { it.isDigit() })
    }

    fun updateDraftNote(value: String) {
        _draft.value = _draft.value.copy(note = value)
    }

    /** Turns on multi-part (drop set) entry for the set currently being logged, adding one empty extra part. */
    fun addDraftPart() {
        _draft.value = _draft.value.copy(extraParts = _draft.value.extraParts + DraftSetPart())
    }

    fun removeDraftPart(index: Int) {
        _draft.value = _draft.value.copy(
            extraParts = _draft.value.extraParts.toMutableList().apply { if (index in indices) removeAt(index) }
        )
    }

    fun updateDraftPartWeight(index: Int, value: String) {
        val parts = _draft.value.extraParts.toMutableList()
        if (index in parts.indices) {
            parts[index] = parts[index].copy(weight = value)
            _draft.value = _draft.value.copy(extraParts = parts)
        }
    }

    fun updateDraftPartReps(index: Int, value: String) {
        val parts = _draft.value.extraParts.toMutableList()
        if (index in parts.indices) {
            parts[index] = parts[index].copy(reps = value.filter { it.isDigit() })
            _draft.value = _draft.value.copy(extraParts = parts)
        }
    }

    /**
     * Logs the current draft as the next set for the current exercise — into
     * the warmup stage while warmup sets are still outstanding, otherwise
     * into working sets — then auto-starts rest, unless this was the
     * exercise's very last set overall. If the draft has extra parts (a
     * drop set), they're all saved together as one logged set.
     */
    fun logCurrentSet() {
        val state = uiState.value
        val exercise = state.currentExercise ?: return
        val weight = _draft.value.weight.toDoubleOrNull() ?: return
        val reps = _draft.value.reps.toIntOrNull() ?: return
        val note = _draft.value.note
        val isWarmup = state.isInWarmupSubstage

        // Only fully-filled-in extra parts count — an incomplete trailing
        // part (e.g. weight typed but reps left blank) is dropped silently
        // rather than blocking the whole set from being logged.
        val validExtraParts = _draft.value.extraParts.mapNotNull { part ->
            val w = part.weight.toDoubleOrNull()
            val r = part.reps.toIntOrNull()
            if (w != null && r != null) SetPart(w, r) else null
        }
        val extraPartsString = (listOf(SetPart(weight, reps)) + validExtraParts).toExtraPartsString()

        viewModelScope.launch {
            val stageSets = if (isWarmup) state.loggedWarmupSets else state.loggedWorkingSets
            val setNumber = stageSets.size + 1
            repository.logSet(
                sessionId = state.sessionId,
                exerciseId = exercise.exerciseId,
                isWarmup = isWarmup,
                setNumber = setNumber,
                weight = weight,
                reps = reps,
                note = note,
                extraParts = extraPartsString
            )
            // Keep the weight prefilled for the next set (common case: same weight across
            // sets), but clear reps/note/extra-parts so they don't carry over by accident.
            _draft.value = SetEntryDraft(weight = _draft.value.weight)

            // Auto-start rest timer, unless this was the exercise's final set overall.
            val willBeComplete = if (isWarmup) {
                (setNumber >= exercise.warmupSets) && exercise.workingSets == 0
            } else {
                setNumber >= exercise.workingSets
            }
            if (!willBeComplete) {
                timerController.start(TimerType.REST, exercise.restSeconds, "Rest · ${exercise.name}")
            }
        }
    }

    fun deleteLoggedSet(loggedSet: LoggedSetEntity) {
        viewModelScope.launch { repository.deleteLoggedSet(loggedSet) }
    }

    /**
     * Skips any remaining warmup sets for the current exercise and moves
     * straight to working sets — for when the person doesn't need to warm
     * up this particular exercise today. Doesn't log fake sets; just marks
     * the warmup sub-stage as satisfied for this exercise in this session.
     */
    fun skipWarmupForCurrentExercise() {
        val exercise = uiState.value.currentExercise ?: return
        _skippedWarmupExerciseIds.value = _skippedWarmupExerciseIds.value + exercise.exerciseId
    }

    fun goToNextExercise() {
        timerController.skip()
        val state = uiState.value
        if (state.isLastExercise) {
            completeSession()
        } else {
            _currentExerciseIndex.value += 1
            _draft.value = SetEntryDraft()
        }
    }

    fun goToPreviousExercise() {
        if (_currentExerciseIndex.value > 0) {
            timerController.skip()
            _currentExerciseIndex.value -= 1
            _draft.value = SetEntryDraft()
        }
    }

    fun jumpToExercise(index: Int) {
        if (index in uiState.value.exercises.indices) {
            timerController.skip()
            _currentExerciseIndex.value = index
            _draft.value = SetEntryDraft()
        }
    }

    fun completeSession() {
        viewModelScope.launch {
            repository.completeSession(uiState.value.sessionId)
            _stage.value = SessionStage.FINISHED
        }
    }

    fun skipTimer() = timerController.skip()
    fun addTimerSeconds(seconds: Int) = timerController.adjustTime(seconds)

    override fun onCleared() {
        super.onCleared()
        // Don't auto-cancel the timer here — it's app-wide (TimerController singleton)
        // so a rest countdown keeps running correctly if this screen is briefly left.
    }
}
