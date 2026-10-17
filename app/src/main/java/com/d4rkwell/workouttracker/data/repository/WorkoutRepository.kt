package com.d4rkwell.workouttracker.data.repository

import com.d4rkwell.workouttracker.data.local.dao.DayDao
import com.d4rkwell.workouttracker.data.local.dao.ExerciseDao
import com.d4rkwell.workouttracker.data.local.dao.LoggedSetDao
import com.d4rkwell.workouttracker.data.local.dao.WeekDao
import com.d4rkwell.workouttracker.data.local.dao.WorkoutSessionDao
import com.d4rkwell.workouttracker.data.local.entity.DayEntity
import com.d4rkwell.workouttracker.data.local.entity.DayWithExercises
import com.d4rkwell.workouttracker.data.local.entity.ExerciseEntity
import com.d4rkwell.workouttracker.data.local.entity.LoggedSetEntity
import com.d4rkwell.workouttracker.data.local.entity.SessionWithLoggedSets
import com.d4rkwell.workouttracker.data.local.entity.WeekEntity
import com.d4rkwell.workouttracker.data.local.entity.WeekWithDays
import com.d4rkwell.workouttracker.data.local.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Single entry point the UI layer talks to. Keeps ViewModels free of
 * direct DAO wiring and gives one place to coordinate multi-table
 * operations (e.g. "start a session" or "reorder weeks").
 */
class WorkoutRepository(
    private val weekDao: WeekDao,
    private val dayDao: DayDao,
    private val exerciseDao: ExerciseDao,
    private val sessionDao: WorkoutSessionDao,
    private val loggedSetDao: LoggedSetDao
) {
    // ---------- Weeks ----------

    // Room's @Relation does not guarantee child ordering, so we sort days by
    // orderIndex here — this is what keeps "Day 1, Day 2, ..." in the right
    // order and keeps weeks showing in the order they were added.
    fun observeAllWeeksWithDays(): Flow<List<WeekWithDays>> =
        weekDao.observeAllWeeksWithDays().map { list ->
            list.map { it.copy(days = it.days.sortedBy { day -> day.orderIndex }) }
        }

    fun observeWeekWithDays(weekId: Long): Flow<WeekWithDays?> =
        weekDao.observeWeekWithDays(weekId).map { week ->
            week?.copy(days = week.days.sortedBy { day -> day.orderIndex })
        }

    suspend fun addWeek(title: String, note: String = ""): Long {
        val nextIndex = weekDao.getMaxOrderIndex() + 1
        return weekDao.insert(WeekEntity(orderIndex = nextIndex, title = title, note = note))
    }

    suspend fun updateWeek(week: WeekEntity) = weekDao.update(week)

    suspend fun deleteWeek(week: WeekEntity) = weekDao.delete(week)

    // ---------- Days ----------

    fun observeDayWithExercises(dayId: Long): Flow<DayWithExercises?> =
        dayDao.observeDayWithExercises(dayId).map { day ->
            day?.copy(exercises = day.exercises.sortedBy { ex -> ex.orderIndex })
        }

    suspend fun addDay(weekId: Long, title: String, warmupMinutes: Int = 5, note: String = ""): Long {
        val nextIndex = dayDao.getMaxOrderIndex(weekId) + 1
        return dayDao.insert(
            DayEntity(
                weekId = weekId,
                orderIndex = nextIndex,
                title = title,
                warmupMinutes = warmupMinutes,
                note = note
            )
        )
    }

    suspend fun updateDay(day: DayEntity) = dayDao.update(day)

    suspend fun deleteDay(day: DayEntity) = dayDao.delete(day)

    // ---------- Exercises ----------

    suspend fun addExercise(
        dayId: Long,
        name: String,
        targetSets: Int,
        targetRepsLow: Int,
        targetRepsHigh: Int,
        restSeconds: Int,
        note: String = "",
        muscleGroup: String = ""
    ): Long {
        val nextIndex = exerciseDao.getMaxOrderIndex(dayId) + 1
        return exerciseDao.insert(
            ExerciseEntity(
                dayId = dayId,
                orderIndex = nextIndex,
                name = name,
                targetSets = targetSets,
                targetRepsLow = targetRepsLow,
                targetRepsHigh = targetRepsHigh,
                restSeconds = restSeconds,
                note = note,
                muscleGroup = muscleGroup
            )
        )
    }

    suspend fun updateExercise(exercise: ExerciseEntity) = exerciseDao.update(exercise)

    suspend fun deleteExercise(exercise: ExerciseEntity) = exerciseDao.delete(exercise)

    suspend fun getExercise(exerciseId: Long): ExerciseEntity? = exerciseDao.getExercise(exerciseId)

    fun observeExercise(exerciseId: Long): Flow<ExerciseEntity?> = exerciseDao.observeExercise(exerciseId)

    // ---------- Sessions ----------

    /** Resumes an in-progress session for this day if one exists, else starts a new one. */
    suspend fun startOrResumeSession(dayId: Long): Long {
        val existing = sessionDao.getInProgressSessionForDay(dayId)
        if (existing != null) return existing.sessionId
        val dayTitle = dayDao.getDay(dayId)?.title ?: ""
        return sessionDao.insert(
            WorkoutSessionEntity(
                dayId = dayId,
                dayTitle = dayTitle,
                startedAtEpochMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun completeSession(sessionId: Long) {
        val session = sessionDao.getSession(sessionId) ?: return
        sessionDao.update(
            session.copy(
                endedAtEpochMillis = System.currentTimeMillis(),
                isCompleted = true
            )
        )
    }

    suspend fun discardSession(session: WorkoutSessionEntity) = sessionDao.delete(session)

    suspend fun getSession(sessionId: Long): WorkoutSessionEntity? = sessionDao.getSession(sessionId)

    fun observeCompletedSessions(): Flow<List<WorkoutSessionEntity>> = sessionDao.observeCompletedSessions()

    fun observeCompletedSessionsForDay(dayId: Long): Flow<List<WorkoutSessionEntity>> =
        sessionDao.observeCompletedSessionsForDay(dayId)

    fun observeSessionWithLoggedSets(sessionId: Long): Flow<SessionWithLoggedSets?> =
        sessionDao.observeSessionWithLoggedSets(sessionId)

    // ---------- Logged sets ----------

    fun observeSetsForExerciseInSession(sessionId: Long, exerciseId: Long): Flow<List<LoggedSetEntity>> =
        loggedSetDao.observeSetsForExerciseInSession(sessionId, exerciseId)

    fun observeHistoryForExercise(exerciseId: Long): Flow<List<LoggedSetEntity>> =
        loggedSetDao.observeHistoryForExercise(exerciseId)

    suspend fun getMostRecentSetBeforeSession(exerciseId: Long, excludingSessionId: Long): LoggedSetEntity? =
        loggedSetDao.getMostRecentSetBeforeSession(exerciseId, excludingSessionId)

    suspend fun logSet(
        sessionId: Long,
        exerciseId: Long,
        setNumber: Int,
        weight: Double,
        reps: Int,
        note: String = ""
    ): Long {
        val exerciseName = exerciseDao.getExercise(exerciseId)?.name ?: ""
        return loggedSetDao.insert(
            LoggedSetEntity(
                sessionId = sessionId,
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                setNumber = setNumber,
                weight = weight,
                reps = reps,
                note = note,
                loggedAtEpochMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateLoggedSet(loggedSet: LoggedSetEntity) = loggedSetDao.update(loggedSet)

    suspend fun deleteLoggedSet(loggedSet: LoggedSetEntity) = loggedSetDao.delete(loggedSet)
}
