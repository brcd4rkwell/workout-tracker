package com.d4rkwell.workouttracker.data.local.dao

import androidx.room.*
import com.d4rkwell.workouttracker.data.local.entity.LoggedSetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LoggedSetDao {

    @Query("SELECT * FROM logged_sets WHERE sessionId = :sessionId ORDER BY loggedSetId ASC")
    fun observeSetsForSession(sessionId: Long): Flow<List<LoggedSetEntity>>

    @Query("SELECT * FROM logged_sets WHERE sessionId = :sessionId AND exerciseId = :exerciseId ORDER BY setNumber ASC")
    fun observeSetsForExerciseInSession(sessionId: Long, exerciseId: Long): Flow<List<LoggedSetEntity>>

    /** Full history for one exercise across all sessions — powers "overall analysis later". */
    @Query("SELECT * FROM logged_sets WHERE exerciseId = :exerciseId ORDER BY loggedAtEpochMillis ASC")
    fun observeHistoryForExercise(exerciseId: Long): Flow<List<LoggedSetEntity>>

    @Query("SELECT * FROM logged_sets WHERE exerciseId = :exerciseId AND sessionId != :excludingSessionId ORDER BY loggedAtEpochMillis DESC LIMIT 1")
    suspend fun getMostRecentSetBeforeSession(exerciseId: Long, excludingSessionId: Long): LoggedSetEntity?

    @Insert
    suspend fun insert(loggedSet: LoggedSetEntity): Long

    @Update
    suspend fun update(loggedSet: LoggedSetEntity)

    @Delete
    suspend fun delete(loggedSet: LoggedSetEntity)

    @Query("DELETE FROM logged_sets WHERE sessionId = :sessionId AND exerciseId = :exerciseId AND setNumber = :setNumber")
    suspend fun deleteBySessionExerciseAndNumber(sessionId: Long, exerciseId: Long, setNumber: Int)
}
