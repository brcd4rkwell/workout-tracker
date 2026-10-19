package com.d4rkwell.workouttracker.ui.screens.weekdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4rkwell.workouttracker.data.local.entity.DayEntity
import com.d4rkwell.workouttracker.data.local.entity.WeekEntity
import com.d4rkwell.workouttracker.data.local.entity.WeekWithDays
import com.d4rkwell.workouttracker.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WeekDetailViewModel(
    private val repository: WorkoutRepository,
    private val weekId: Long
) : ViewModel() {

    val weekWithDays: StateFlow<WeekWithDays?> = repository.observeWeekWithDays(weekId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun addDay(title: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.addDay(weekId, title)
            onCreated(id)
        }
    }

    fun deleteDay(day: DayEntity) {
        viewModelScope.launch { repository.deleteDay(day) }
    }

    fun renameDay(day: DayEntity, newTitle: String) {
        viewModelScope.launch { repository.updateDay(day.copy(title = newTitle)) }
    }

    fun updateWeekNote(week: WeekEntity, note: String) {
        viewModelScope.launch { repository.updateWeek(week.copy(note = note)) }
    }

    fun renameWeek(week: WeekEntity, newTitle: String) {
        viewModelScope.launch { repository.updateWeek(week.copy(title = newTitle)) }
    }

    fun moveDayUp(dayId: Long) {
        viewModelScope.launch { repository.moveDay(dayId, weekId, direction = -1) }
    }

    fun moveDayDown(dayId: Long) {
        viewModelScope.launch { repository.moveDay(dayId, weekId, direction = 1) }
    }
}
