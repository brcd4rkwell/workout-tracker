package com.d4rkwell.workouttracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A prescribed exercise within a [DayEntity] — this is the "plan" row:
 * target sets/reps/rest, plus the user's own free-form note (their own
 * cues/ideas, shown right next to the exercise name).
 *
 * Actual performed sets (weight/reps logged on a given date) live in
 * [LoggedSetEntity] and reference this exercise, so history/analysis can
 * be grouped per exercise across weeks.
 */
@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(
            entity = DayEntity::class,
            parentColumns = ["dayId"],
            childColumns = ["dayId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("dayId")]
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val exerciseId: Long = 0,
    val dayId: Long,
    val orderIndex: Int,
    val name: String,
    val targetSets: Int,
    val targetRepsLow: Int,          // rep range low end, e.g. 8
    val targetRepsHigh: Int,         // rep range high end, e.g. 12 (equal to low if single number)
    val restSeconds: Int,            // rest between sets, in seconds
    val note: String = "",           // user's own little note, shown near the exercise name
    val muscleGroup: String = ""     // optional free-text tag, e.g. "Chest"
)
