package com.d4rkwell.workouttracker.ui.screens.weightprogress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4rkwell.workouttracker.data.local.entity.WeightEntryEntity
import com.d4rkwell.workouttracker.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WeightProgressViewModel(private val repository: WorkoutRepository) : ViewModel() {

    val entries: StateFlow<List<WeightEntryEntity>> = repository.observeAllWeightEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addEntry(weight: Double, note: String = "") {
        viewModelScope.launch { repository.logWeightEntry(weight, note) }
    }

    fun updateEntry(entry: WeightEntryEntity, weight: Double, note: String) {
        viewModelScope.launch { repository.updateWeightEntry(entry.copy(weight = weight, note = note)) }
    }

    fun deleteEntry(entry: WeightEntryEntity) {
        viewModelScope.launch { repository.deleteWeightEntry(entry) }
    }
}
