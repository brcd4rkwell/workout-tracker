package com.d4rkwell.workouttracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single training week in the program. Weeks are open-ended: the user
 * adds them one at a time via the program editor, and [orderIndex]
 * controls the order they're displayed in (so the user always knows
 * "which week of the workout" they're on).
 */
@Entity(tableName = "weeks")
data class WeekEntity(
    @PrimaryKey(autoGenerate = true)
    val weekId: Long = 0,
    val orderIndex: Int,
    val title: String,          // e.g. "Week 1" or a custom label like "Week 1 - Deload"
    val note: String = ""
)
