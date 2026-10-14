package com.d4rkwell.workouttracker.ui.screens.exerciseeditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4rkwell.workouttracker.data.local.entity.ExerciseEntity
import com.d4rkwell.workouttracker.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExerciseFormState(
    val name: String = "",
    val targetSets: String = "3",
    val targetRepsLow: String = "8",
    val targetRepsHigh: String = "12",
    val restSeconds: String = "90",
    val note: String = "",
    val muscleGroup: String = "",
    val isLoaded: Boolean = false,
    val isEditing: Boolean = false
)

class ExerciseEditorViewModel(
    private val repository: WorkoutRepository,
    private val dayId: Long,
    private val exerciseId: Long // -1 for a new exercise
) : ViewModel() {

    private val _form = MutableStateFlow(ExerciseFormState(isEditing = exerciseId != -1L))
    val form: StateFlow<ExerciseFormState> = _form.asStateFlow()

    private var existingExercise: ExerciseEntity? = null

    init {
        if (exerciseId != -1L) {
            viewModelScope.launch {
                val exercise = repository.getExercise(exerciseId)
                existingExercise = exercise
                if (exercise != null) {
                    _form.value = ExerciseFormState(
                        name = exercise.name,
                        targetSets = exercise.targetSets.toString(),
                        targetRepsLow = exercise.targetRepsLow.toString(),
                        targetRepsHigh = exercise.targetRepsHigh.toString(),
                        restSeconds = exercise.restSeconds.toString(),
                        note = exercise.note,
                        muscleGroup = exercise.muscleGroup,
                        isLoaded = true,
                        isEditing = true
                    )
                }
            }
        } else {
            _form.value = _form.value.copy(isLoaded = true)
        }
    }

    fun updateName(value: String) { _form.value = _form.value.copy(name = value) }
    fun updateSets(value: String) { _form.value = _form.value.copy(targetSets = value.filter { it.isDigit() }) }
    fun updateRepsLow(value: String) { _form.value = _form.value.copy(targetRepsLow = value.filter { it.isDigit() }) }
    fun updateRepsHigh(value: String) { _form.value = _form.value.copy(targetRepsHigh = value.filter { it.isDigit() }) }
    fun updateRestSeconds(value: String) { _form.value = _form.value.copy(restSeconds = value.filter { it.isDigit() }) }
    fun updateNote(value: String) { _form.value = _form.value.copy(note = value) }
    fun updateMuscleGroup(value: String) { _form.value = _form.value.copy(muscleGroup = value) }

    val isValid: Boolean
        get() {
            val f = _form.value
            return f.name.isNotBlank() &&
                (f.targetSets.toIntOrNull() ?: 0) > 0 &&
                (f.targetRepsLow.toIntOrNull() ?: 0) > 0 &&
                (f.targetRepsHigh.toIntOrNull() ?: 0) > 0 &&
                (f.restSeconds.toIntOrNull() ?: -1) >= 0
        }

    fun save(onDone: () -> Unit) {
        val f = _form.value
        val sets = f.targetSets.toIntOrNull() ?: return
        val repsLow = f.targetRepsLow.toIntOrNull() ?: return
        val repsHigh = f.targetRepsHigh.toIntOrNull() ?: return
        val rest = f.restSeconds.toIntOrNull() ?: return
        val normalizedHigh = maxOf(repsLow, repsHigh)

        viewModelScope.launch {
            val existing = existingExercise
            if (existing != null) {
                repository.updateExercise(
                    existing.copy(
                        name = f.name.trim(),
                        targetSets = sets,
                        targetRepsLow = repsLow,
                        targetRepsHigh = normalizedHigh,
                        restSeconds = rest,
                        note = f.note.trim(),
                        muscleGroup = f.muscleGroup.trim()
                    )
                )
            } else {
                repository.addExercise(
                    dayId = dayId,
                    name = f.name.trim(),
                    targetSets = sets,
                    targetRepsLow = repsLow,
                    targetRepsHigh = normalizedHigh,
                    restSeconds = rest,
                    note = f.note.trim(),
                    muscleGroup = f.muscleGroup.trim()
                )
            }
            onDone()
        }
    }
}
