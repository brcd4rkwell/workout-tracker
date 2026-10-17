package com.d4rkwell.workouttracker.ui.screens.workoutsession

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4rkwell.workouttracker.data.local.entity.DayWithExercises
import com.d4rkwell.workouttracker.data.local.entity.ExerciseEntity
import com.d4rkwell.workouttracker.data.local.entity.LoggedSetEntity
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

data class SetEntryDraft(
    val weight: String = "",
    val reps: String = "",
    val note: String = ""
)

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
    val isLoaded: Boolean = false
) {
    val currentExercise: ExerciseEntity?
        get() = exercises.getOrNull(currentExerciseIndex)

    val completedSetCountForCurrentExercise: Int
        get() = loggedSetsForCurrentExercise.size

    val isCurrentExerciseComplete: Boolean
        get() = currentExercise?.let { completedSetCountForCurrentExercise >= it.targetSets } ?: false

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
    private val _draft = MutableStateFlow(SetEntryDraft())
    val draft: StateFlow<SetEntryDraft> = _draft.asStateFlow()

    val timerState = timerController.state

    private val dayFlow: StateFlow<DayWithExercises?> = repository.observeDayWithExercises(dayId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val uiState: StateFlow<WorkoutSessionUiState> = combine(
        dayFlow, _stage, _currentExerciseIndex, _sessionId, _sessionStartedAt, _previousBest
    ) { values ->
        val day = values[0] as DayWithExercises?
        val stage = values[1] as SessionStage
        val exerciseIndex = values[2] as Int
        val sessionId = values[3] as Long
        val startedAt = values[4] as Long
        val previousBest = values[5] as LoggedSetEntity?
        val exercises = day?.exercises.orEmpty()
        WorkoutSessionUiState(
            dayTitle = day?.day?.title ?: "",
            stage = stage,
            warmupMinutes = day?.day?.warmupMinutes ?: 5,
            exercises = exercises,
            currentExerciseIndex = exerciseIndex,
            previousBestForCurrentExercise = previousBest,
            sessionId = sessionId,
            sessionStartedAtMillis = startedAt,
            isLoaded = day != null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WorkoutSessionUiState())

    // Logged sets for whichever exercise is currently active, refreshed live as the
    // session/exercise selection changes.
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
        viewModelScope.launch {
            combine(_sessionId, _currentExerciseIndex, dayFlow) { sessionId, index, day ->
                Pair(sessionId, day?.exercises?.getOrNull(index)?.exerciseId)
            }.collect { (sessionId, exerciseId) ->
                _previousBest.value = if (sessionId > 0 && exerciseId != null) {
                    repository.getMostRecentSetBeforeSession(exerciseId, sessionId)
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

    /** Logs the current draft as the next set for the current exercise, then auto-starts rest. */
    fun logCurrentSet() {
        val state = uiState.value
        val exercise = state.currentExercise ?: return
        val weight = _draft.value.weight.toDoubleOrNull() ?: return
        val reps = _draft.value.reps.toIntOrNull() ?: return
        val note = _draft.value.note

        viewModelScope.launch {
            val setNumber = loggedSetsForCurrentExercise.value.size + 1
            repository.logSet(
                sessionId = state.sessionId,
                exerciseId = exercise.exerciseId,
                setNumber = setNumber,
                weight = weight,
                reps = reps,
                note = note
            )
            // Keep the weight prefilled for the next set (common case: same weight across
            // sets), but clear reps/note so they don't carry over by accident.
            _draft.value = SetEntryDraft(weight = _draft.value.weight)

            // Auto-start rest timer, unless this was the exercise's final set.
            val willBeComplete = setNumber >= exercise.targetSets
            if (!willBeComplete) {
                timerController.start(TimerType.REST, exercise.restSeconds, "Rest · ${exercise.name}")
            }
        }
    }

    fun deleteLoggedSet(loggedSet: LoggedSetEntity) {
        viewModelScope.launch { repository.deleteLoggedSet(loggedSet) }
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
