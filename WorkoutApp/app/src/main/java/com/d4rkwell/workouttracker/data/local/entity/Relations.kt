package com.d4rkwell.workouttracker.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/** A week with all of its days nested (days NOT yet including exercises). */
data class WeekWithDays(
    @Embedded val week: WeekEntity,
    @Relation(
        parentColumn = "weekId",
        entityColumn = "weekId"
    )
    val days: List<DayEntity>
)

/** A day with its prescribed exercises nested. */
data class DayWithExercises(
    @Embedded val day: DayEntity,
    @Relation(
        parentColumn = "dayId",
        entityColumn = "dayId"
    )
    val exercises: List<ExerciseEntity>
)

/** A workout session with all logged sets nested (for history detail / inspection). */
data class SessionWithLoggedSets(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(
        parentColumn = "sessionId",
        entityColumn = "sessionId"
    )
    val loggedSets: List<LoggedSetEntity>
)
