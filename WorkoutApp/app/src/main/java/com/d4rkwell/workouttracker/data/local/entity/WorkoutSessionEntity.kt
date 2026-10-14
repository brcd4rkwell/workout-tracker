package com.d4rkwell.workouttracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One real-world instance of performing a [DayEntity]'s workout — e.g.
 * "Day 1 - Push, done on 2026-10-09". Tracks start/end time so total
 * duration can be shown and inspected later in history.
 *
 * [dayId] is kept as a plain (non-FK) column, with [dayTitle] snapshotting
 * the day's name at the time the session was started — so completed
 * workout history survives the user later renaming, reorganizing, or
 * deleting that day/week from their plan. History is permanent by design;
 * only the plan is editable.
 */
@Entity(
    tableName = "workout_sessions",
    indices = [Index("dayId")]
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val sessionId: Long = 0,
    val dayId: Long,
    val dayTitle: String = "",
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long? = null,   // null while session is in progress
    val isCompleted: Boolean = false
)
