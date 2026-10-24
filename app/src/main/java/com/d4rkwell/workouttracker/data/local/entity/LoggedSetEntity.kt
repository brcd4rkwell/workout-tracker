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
    indices = [Index("sessionId"), Index("exerciseId"), Index("loggedAtEpochMillis")]
)
data class LoggedSetEntity(
    @PrimaryKey(autoGenerate = true)
    val loggedSetId: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val exerciseName: String = "", // snapshot of the exercise's name at the time this set was logged
    val isWarmup: Boolean = false,  // true for a warmup-stage set, false for a working set
    val setNumber: Int,             // 1-based position within its stage (warmup or working) for this session
    val weight: Double,              // the set's first (or only) weight×reps part
    val reps: Int,                   // the set's first (or only) weight×reps part
    val note: String = "",          // optional quick note for this specific set
    val loggedAtEpochMillis: Long,
    // Additional weight×reps parts beyond the first, for a drop set logged
    // as one set (e.g. 25kg×4 then 20kg×6). Stored as "weight,reps;weight,reps"
    // — deliberately plain text rather than a second table or JSON, since
    // it's always small and this keeps every existing query/analysis that
    // reads weight/reps directly (the first part) working unchanged.
    val extraParts: String = ""
)

/** One weight×reps part of a (possibly multi-part / drop) set. */
data class SetPart(val weight: Double, val reps: Int)

/** All parts of this set, first part included — for a plain set, a list of one. */
fun LoggedSetEntity.allParts(): List<SetPart> {
    val parts = mutableListOf(SetPart(weight, reps))
    if (extraParts.isNotBlank()) {
        extraParts.split(";").forEach { chunk ->
            val pieces = chunk.split(",")
            if (pieces.size == 2) {
                val w = pieces[0].toDoubleOrNull()
                val r = pieces[1].toIntOrNull()
                if (w != null && r != null) parts.add(SetPart(w, r))
            }
        }
    }
    return parts
}

fun List<SetPart>.toExtraPartsString(): String =
    if (size <= 1) "" else drop(1).joinToString(";") { "${it.weight},${it.reps}" }

val LoggedSetEntity.isMultiPart: Boolean
    get() = extraParts.isNotBlank()

/** Combined rep total across all parts — e.g. 25×4 + 20×6 → 10 total reps. */
fun LoggedSetEntity.totalReps(): Int = allParts().sumOf { it.reps }
