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
    val warmupSets: String = "0",
    val workingSets: String = "3",
    val targetRepsLow: String = "8",
    val targetRepsHigh: String = "12",
    val restSeconds: String = "90",
    val note: String = "",
    val videoUrl: String = "",
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
                        warmupSets = exercise.warmupSets.toString(),
                        workingSets = exercise.workingSets.toString(),
                        targetRepsLow = exercise.targetRepsLow.toString(),
                        targetRepsHigh = exercise.targetRepsHigh.toString(),
                        restSeconds = exercise.restSeconds.toString(),
                        note = exercise.note,
                        videoUrl = exercise.videoUrl,
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
    fun updateWarmupSets(value: String) { _form.value = _form.value.copy(warmupSets = value.filter { it.isDigit() }) }
    fun updateWorkingSets(value: String) { _form.value = _form.value.copy(workingSets = value.filter { it.isDigit() }) }
    fun updateRepsLow(value: String) { _form.value = _form.value.copy(targetRepsLow = value.filter { it.isDigit() }) }
    fun updateRepsHigh(value: String) { _form.value = _form.value.copy(targetRepsHigh = value.filter { it.isDigit() }) }
    fun updateRestSeconds(value: String) { _form.value = _form.value.copy(restSeconds = value.filter { it.isDigit() }) }
    fun updateNote(value: String) { _form.value = _form.value.copy(note = value) }
    fun updateVideoUrl(value: String) { _form.value = _form.value.copy(videoUrl = value) }

    val isValid: Boolean
        get() {
            val f = _form.value
            return f.name.isNotBlank() &&
                (f.workingSets.toIntOrNull() ?: 0) > 0 &&
                (f.warmupSets.toIntOrNull() ?: -1) >= 0 &&
                (f.targetRepsLow.toIntOrNull() ?: 0) > 0 &&
                (f.targetRepsHigh.toIntOrNull() ?: 0) > 0 &&
                (f.restSeconds.toIntOrNull() ?: -1) >= 0
        }

    fun save(onDone: () -> Unit) {
        val f = _form.value
        val warmup = f.warmupSets.toIntOrNull() ?: 0
        val working = f.workingSets.toIntOrNull() ?: return
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
                        warmupSets = warmup,
                        workingSets = working,
                        targetRepsLow = repsLow,
                        targetRepsHigh = normalizedHigh,
                        restSeconds = rest,
                        note = f.note.trim(),
                        videoUrl = f.videoUrl.trim()
                    )
                )
            } else {
                repository.addExercise(
                    dayId = dayId,
                    name = f.name.trim(),
                    warmupSets = warmup,
                    workingSets = working,
                    targetRepsLow = repsLow,
                    targetRepsHigh = normalizedHigh,
                    restSeconds = rest,
                    note = f.note.trim(),
                    videoUrl = f.videoUrl.trim()
                )
            }
            onDone()
        }
    }
}
