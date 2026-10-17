package com.d4rkwell.workouttracker.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4rkwell.workouttracker.data.local.entity.WorkoutSessionEntity
import com.d4rkwell.workouttracker.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HistorySessionRow(
    val session: WorkoutSessionEntity,
    val dayTitle: String
)

class HistoryViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    // dayTitle is a snapshot stored on the session itself at the time it was
    // started, so history keeps showing correctly even if the day/week it
    // came from is later renamed or deleted from the plan.
    val sessions: StateFlow<List<HistorySessionRow>> = repository.observeCompletedSessions()
        .map { sessions ->
            sessions.map { session ->
                HistorySessionRow(
                    session = session,
                    dayTitle = session.dayTitle.ifBlank { "Workout" }
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
