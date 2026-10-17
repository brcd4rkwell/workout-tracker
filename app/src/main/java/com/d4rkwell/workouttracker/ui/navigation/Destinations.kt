package com.d4rkwell.workouttracker.ui.navigation

object Destinations {
    const val WEEK_LIST = "week_list"
    const val WEEK_DETAIL = "week_detail/{weekId}"
    const val DAY_EDITOR = "day_editor/{dayId}"
    const val EXERCISE_EDITOR = "exercise_editor/{dayId}/{exerciseId}"
    const val WORKOUT_SESSION = "workout_session/{dayId}"
    const val HISTORY = "history"
    const val HISTORY_DETAIL = "history_detail/{sessionId}"

    fun weekDetail(weekId: Long) = "week_detail/$weekId"
    fun dayEditor(dayId: Long) = "day_editor/$dayId"

    /** exerciseId = -1 means "new exercise" for this day. */
    fun exerciseEditor(dayId: Long, exerciseId: Long = -1L) = "exercise_editor/$dayId/$exerciseId"
    fun workoutSession(dayId: Long) = "workout_session/$dayId"
    fun historyDetail(sessionId: Long) = "history_detail/$sessionId"
}
