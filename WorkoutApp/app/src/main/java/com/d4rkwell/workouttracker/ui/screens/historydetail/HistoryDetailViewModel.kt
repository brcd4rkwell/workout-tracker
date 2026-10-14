package com.d4rkwell.workouttracker.ui.screens.historydetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4rkwell.workouttracker.data.local.entity.LoggedSetEntity
import com.d4rkwell.workouttracker.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ExerciseSetGroup(
    val exerciseName: String,
    val sets: List<LoggedSetEntity>
)

data class HistoryDetailUiState(
    val dayTitle: String = "",
    val startedAtMillis: Long = 0,
    val endedAtMillis: Long? = null,
    val groups: List<ExerciseSetGroup> = emptyList(),
    val isLoaded: Boolean = false
)

class HistoryDetailViewModel(
    private val repository: WorkoutRepository,
    private val sessionId: Long
) : ViewModel() {

    // Exercise names are read from each logged set's own snapshot (taken at
    // the moment it was logged), not looked up live — so history stays
    // correct even if the exercise was later renamed or removed from the plan.
    val uiState: StateFlow<HistoryDetailUiState> = repository.observeSessionWithLoggedSets(sessionId)
        .map { sessionWithSets ->
            if (sessionWithSets == null) {
                HistoryDetailUiState(isLoaded = false)
            } else {
                val grouped = sessionWithSets.loggedSets
                    .groupBy { it.exerciseId }
                    .map { (_, sets) ->
                        ExerciseSetGroup(
                            exerciseName = sets.first().exerciseName.ifBlank { "Exercise" },
                            sets = sets.sortedBy { it.setNumber }
                        )
                    }
                HistoryDetailUiState(
                    dayTitle = sessionWithSets.session.dayTitle.ifBlank { "Workout" },
                    startedAtMillis = sessionWithSets.session.startedAtEpochMillis,
                    endedAtMillis = sessionWithSets.session.endedAtEpochMillis,
                    groups = grouped,
                    isLoaded = true
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryDetailUiState())
}
