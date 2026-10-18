package com.d4rkwell.workouttracker

import android.app.Application
import com.d4rkwell.workouttracker.data.local.AppDatabase
import com.d4rkwell.workouttracker.data.repository.WorkoutRepository

class WorkoutApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    val repository: WorkoutRepository by lazy {
        WorkoutRepository(
            weekDao = database.weekDao(),
            dayDao = database.dayDao(),
            exerciseDao = database.exerciseDao(),
            sessionDao = database.workoutSessionDao(),
            loggedSetDao = database.loggedSetDao(),
            weightEntryDao = database.weightEntryDao()
        )
    }
}
