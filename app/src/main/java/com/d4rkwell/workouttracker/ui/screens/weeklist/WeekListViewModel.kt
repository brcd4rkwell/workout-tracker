package com.d4rkwell.workouttracker.ui.screens.weeklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4rkwell.workouttracker.data.local.entity.WeekEntity
import com.d4rkwell.workouttracker.data.local.entity.WeekWithDays
import com.d4rkwell.workouttracker.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WeekListViewModel(private val repository: WorkoutRepository) : ViewModel() {

    val weeks: StateFlow<List<WeekWithDays>> = repository.observeAllWeeksWithDays()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addWeek(title: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.addWeek(title)
            onCreated(id)
        }
    }

    fun deleteWeek(week: WeekEntity) {
        viewModelScope.launch { repository.deleteWeek(week) }
    }

    fun renameWeek(week: WeekEntity, newTitle: String) {
        viewModelScope.launch { repository.updateWeek(week.copy(title = newTitle)) }
    }

    fun duplicateWeek(weekId: Long) {
        viewModelScope.launch { repository.duplicateWeek(weekId) }
    }
}
