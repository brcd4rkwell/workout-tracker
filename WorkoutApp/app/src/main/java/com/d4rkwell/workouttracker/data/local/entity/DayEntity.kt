package com.d4rkwell.workouttracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A training day that belongs to a [WeekEntity], e.g. "Day 1 - Push".
 * Deleting a week cascades and removes its days.
 */
@Entity(
    tableName = "days",
    foreignKeys = [
        ForeignKey(
            entity = WeekEntity::class,
            parentColumns = ["weekId"],
            childColumns = ["weekId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("weekId")]
)
data class DayEntity(
    @PrimaryKey(autoGenerate = true)
    val dayId: Long = 0,
    val weekId: Long,
    val orderIndex: Int,
    val title: String,              // e.g. "Day 1 - Push"
    val warmupMinutes: Int = 5,     // default warmup timer duration for this day
    val note: String = ""
)
