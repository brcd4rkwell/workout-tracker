package com.d4rkwell.workouttracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A prescribed exercise within a [DayEntity] — this is the "plan" row:
 * target warmup/working sets, rep range, rest, plus the user's own
 * free-form note (their own cues/ideas, shown right next to the
 * exercise name).
 *
 * Warmup sets and working sets are tracked as separate counts: warmup
 * sets are light ramp-up sets done before the real working weight, and
 * are logged (and shown during a live session) as their own distinct
 * stage before the working sets.
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
    val warmupSets: Int = 0,         // light ramp-up sets, logged before working sets
    val workingSets: Int,            // the real/counted working sets
    val targetRepsLow: Int,          // rep range low end, e.g. 8
    val targetRepsHigh: Int,         // rep range high end, e.g. 12 (equal to low if single number)
    val restSeconds: Int,            // rest between sets, in seconds
    val note: String = "",           // user's own little note, shown near the exercise name
    val videoUrl: String = ""        // optional YouTube link showing how to perform the exercise
)
