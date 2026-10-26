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

    /**
     * Finds the single most recent session (by sessionId, which only ever
     * increases) in which a working set of this exercise was logged, so we
     * can then show "your heaviest working set from last time" even
     * outside of a live session — e.g. while planning a new week.
     */
    @Query(
        "SELECT sessionId FROM logged_sets WHERE exerciseId = :exerciseId AND isWarmup = 0 " +
            "ORDER BY sessionId DESC LIMIT 1"
    )
    suspend fun getMostRecentSessionIdForExercise(exerciseId: Long): Long?

    /**
     * Within one session, the heaviest working set logged for this
     * exercise (ties broken by higher reps) — i.e. your top set from that
     * session.
     */
    @Query(
        "SELECT * FROM logged_sets WHERE exerciseId = :exerciseId AND sessionId = :sessionId AND isWarmup = 0 " +
            "ORDER BY weight DESC, reps DESC LIMIT 1"
    )
    suspend fun getHeaviestWorkingSetInSession(exerciseId: Long, sessionId: Long): LoggedSetEntity?

    // ---- By-name lookups ----
    // The queries above key everything off exerciseId, the database row id
    // of one specific exercise entry in the plan — but the same named
    // exercise (e.g. "Bench Press") typically gets added separately to
    // several weeks, each as its own distinct exerciseId with no shared
    // history. These by-name variants instead match on the exerciseName
    // snapshot stored on every logged set, so "last time's result" carries
    // across every week that includes an exercise with the same name,
    // which is what most multi-week programs actually need. Matching is
    // case-insensitive and trims whitespace so small naming differences
    // ("Bench press" vs "Bench Press ") don't silently break the link.

    @Query(
        "SELECT sessionId FROM logged_sets WHERE isWarmup = 0 " +
            "AND LOWER(TRIM(exerciseName)) = LOWER(TRIM(:exerciseName)) " +
            "AND sessionId != :excludingSessionId " +
            "ORDER BY sessionId DESC LIMIT 1"
    )
    suspend fun getMostRecentSessionIdForExerciseName(exerciseName: String, excludingSessionId: Long): Long?

    @Query(
        "SELECT * FROM logged_sets WHERE sessionId = :sessionId AND isWarmup = 0 " +
            "AND LOWER(TRIM(exerciseName)) = LOWER(TRIM(:exerciseName)) " +
            "ORDER BY weight DESC, reps DESC LIMIT 1"
    )
    suspend fun getHeaviestWorkingSetInSessionByName(exerciseName: String, sessionId: Long): LoggedSetEntity?

    @Insert
    suspend fun insert(loggedSet: LoggedSetEntity): Long

    @Update
    suspend fun update(loggedSet: LoggedSetEntity)

    @Delete
    suspend fun delete(loggedSet: LoggedSetEntity)

    @Query("DELETE FROM logged_sets WHERE sessionId = :sessionId AND exerciseId = :exerciseId AND setNumber = :setNumber")
    suspend fun deleteBySessionExerciseAndNumber(sessionId: Long, exerciseId: Long, setNumber: Int)
}
