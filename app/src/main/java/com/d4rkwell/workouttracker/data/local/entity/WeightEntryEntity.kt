package com.d4rkwell.workouttracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One logged bodyweight entry, independent of any workout session — this
 * is for tracking overall weight progress over the course of the program
 * (e.g. a starting weight, then an entry every month or every 4 weeks).
 */
@Entity(tableName = "weight_entries")
data class WeightEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val weightEntryId: Long = 0,
    val weight: Double,
    val note: String = "",
    val loggedAtEpochMillis: Long
)
