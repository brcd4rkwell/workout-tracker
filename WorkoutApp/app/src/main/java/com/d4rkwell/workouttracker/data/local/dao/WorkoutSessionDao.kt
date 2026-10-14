package com.d4rkwell.workouttracker.data.local.dao

import androidx.room.*
import com.d4rkwell.workouttracker.data.local.entity.SessionWithLoggedSets
import com.d4rkwell.workouttracker.data.local.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {

    @Query("SELECT * FROM workout_sessions WHERE isCompleted = 1 ORDER BY startedAtEpochMillis DESC")
    fun observeCompletedSessions(): Flow<List<WorkoutSessionEntity>>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE sessionId = :sessionId")
    fun observeSessionWithLoggedSets(sessionId: Long): Flow<SessionWithLoggedSets?>

    @Query("SELECT * FROM workout_sessions WHERE dayId = :dayId AND isCompleted = 0 ORDER BY startedAtEpochMillis DESC LIMIT 1")
    suspend fun getInProgressSessionForDay(dayId: Long): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE sessionId = :sessionId")
    suspend fun getSession(sessionId: Long): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE dayId = :dayId AND isCompleted = 1 ORDER BY startedAtEpochMillis DESC")
    fun observeCompletedSessionsForDay(dayId: Long): Flow<List<WorkoutSessionEntity>>

    @Insert
    suspend fun insert(session: WorkoutSessionEntity): Long

    @Update
    suspend fun update(session: WorkoutSessionEntity)

    @Delete
    suspend fun delete(session: WorkoutSessionEntity)
}
