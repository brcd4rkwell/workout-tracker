package com.d4rkwell.workouttracker.ui.screens.dayeditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4rkwell.workouttracker.data.local.entity.DayEntity
import com.d4rkwell.workouttracker.data.local.entity.DayWithExercises
import com.d4rkwell.workouttracker.data.local.entity.ExerciseEntity
import com.d4rkwell.workouttracker.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DayEditorViewModel(
    private val repository: WorkoutRepository,
    private val dayId: Long
) : ViewModel() {

    val dayWithExercises: StateFlow<DayWithExercises?> = repository.observeDayWithExercises(dayId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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
}
