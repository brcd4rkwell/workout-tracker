package com.d4rkwell.workouttracker.ui.screens.dayeditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4rkwell.workouttracker.data.local.entity.DayEntity
import com.d4rkwell.workouttracker.data.local.entity.DayWithExercises
import com.d4rkwell.workouttracker.data.local.entity.ExerciseEntity
import com.d4rkwell.workouttracker.data.local.entity.LoggedSetEntity
import com.d4rkwell.workouttracker.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DayEditorViewModel(
    private val repository: WorkoutRepository,
    private val dayId: Long
) : ViewModel() {

    val dayWithExercises: StateFlow<DayWithExercises?> = repository.observeDayWithExercises(dayId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Each exercise's heaviest working set from its last logged session, keyed by
    // exerciseId — so the plan list can show "Last used: 80kg x 8" per exercise.
    private val _lastTopSets = MutableStateFlow<Map<Long, LoggedSetEntity?>>(emptyMap())
    val lastTopSets: StateFlow<Map<Long, LoggedSetEntity?>> = _lastTopSets.asStateFlow()

    init {
        viewModelScope.launch {
            dayWithExercises.collect { day ->
                val exercises = day?.exercises.orEmpty()
                val results = exercises.associate { ex ->
                    ex.exerciseId to repository.getLastTopWorkingSet(ex.exerciseId)
                }
                _lastTopSets.value = results
            }
        }
    }

    fun deleteExercise(exercise: ExerciseEntity) {
        viewModelScope.launch { repository.deleteExercise(exercise) }
    }

    fun renameDay(day: DayEntity, newTitle: String) {
        viewModelScope.launch { repository.updateDay(day.copy(title = newTitle)) }
    }

    fun updateWarmupMinutes(day: DayEntity, minutes: Int) {
        viewModelScope.launch { repository.updateDay(day.copy(warmupMinutes = minutes)) }
    }

    fun updateDayNote(day: DayEntity, note: String) {
        viewModelScope.launch { repository.updateDay(day.copy(note = note)) }
    }

    fun reorderExercises(orderedExerciseIds: List<Long>) {
        viewModelScope.launch { repository.reorderExercises(orderedExerciseIds) }
    }
}
