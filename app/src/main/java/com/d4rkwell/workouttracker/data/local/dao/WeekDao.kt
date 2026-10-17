package com.d4rkwell.workouttracker.data.local.dao

import androidx.room.*
import com.d4rkwell.workouttracker.data.local.entity.WeekEntity
import com.d4rkwell.workouttracker.data.local.entity.WeekWithDays
import kotlinx.coroutines.flow.Flow

@Dao
interface WeekDao {

    @Query("SELECT * FROM weeks ORDER BY orderIndex ASC")
    fun observeAllWeeks(): Flow<List<WeekEntity>>

    @Transaction
    @Query("SELECT * FROM weeks ORDER BY orderIndex ASC")
    fun observeAllWeeksWithDays(): Flow<List<WeekWithDays>>

    @Transaction
    @Query("SELECT * FROM weeks WHERE weekId = :weekId")
    fun observeWeekWithDays(weekId: Long): Flow<WeekWithDays?>

    @Query("SELECT * FROM weeks WHERE weekId = :weekId")
    suspend fun getWeek(weekId: Long): WeekEntity?

    @Query("SELECT COALESCE(MAX(orderIndex), -1) FROM weeks")
    suspend fun getMaxOrderIndex(): Int

    @Insert
    suspend fun insert(week: WeekEntity): Long

    @Update
    suspend fun update(week: WeekEntity)

    @Delete
    suspend fun delete(week: WeekEntity)
}
