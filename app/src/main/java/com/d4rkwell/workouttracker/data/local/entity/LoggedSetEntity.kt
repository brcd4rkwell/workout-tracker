package com.d4rkwell.workouttracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One actually-performed set: weight x reps (+ optional quick note),
 * tied to a specific [WorkoutSessionEntity] and (by id only — see below)
 * the exercise it was performed under. This is the row-level data that
 * powers later analysis (progress over time, per-exercise history, etc).
 *
 * Deliberately NOT a foreign key to [ExerciseEntity]: history must survive
 * the user editing or deleting exercises from their plan later, so
 * [exerciseId] and [exerciseName] (a snapshot taken at logging time) are
 * kept as plain columns rather than a cascading reference. The session
 * link IS a real cascading FK, since deleting a whole session (e.g.
 * discarding an accidental start) should remove its sets.
 */
@Entity(
    tableName = "logged_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId"), Index("exerciseId")]
)
data class LoggedSetEntity(
    @PrimaryKey(autoGenerate = true)
    val loggedSetId: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val exerciseName: String = "", // snapshot of the exercise's name at the time this set was logged
    val setNumber: Int,             // 1-based position within this exercise for this session
    val weight: Double,
    val reps: Int,
    val note: String = "",          // optional quick note for this specific set
    val loggedAtEpochMillis: Long
)
