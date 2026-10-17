package com.d4rkwell.workouttracker.data.local.dao

import androidx.room.*
import com.d4rkwell.workouttracker.data.local.entity.DayEntity
import com.d4rkwell.workouttracker.data.local.entity.DayWithExercises
import kotlinx.coroutines.flow.Flow

@Dao
interface DayDao {

    @Query("SELECT * FROM days WHERE weekId = :weekId ORDER BY orderIndex ASC")
    fun observeDaysForWeek(weekId: Long): Flow<List<DayEntity>>

    @Transaction
    @Query("SELECT * FROM days WHERE dayId = :dayId")
    fun observeDayWithExercises(dayId: Long): Flow<DayWithExercises?>

    @Query("SELECT * FROM days WHERE dayId = :dayId")
    suspend fun getDay(dayId: Long): DayEntity?

    @Query("SELECT COALESCE(MAX(orderIndex), -1) FROM days WHERE weekId = :weekId")
    suspend fun getMaxOrderIndex(weekId: Long): Int

    @Insert
    suspend fun insert(day: DayEntity): Long

    @Update
    suspend fun update(day: DayEntity)

    @Delete
    suspend fun delete(day: DayEntity)
}
