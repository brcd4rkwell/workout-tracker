package com.d4rkwell.workouttracker.data.local.dao

import androidx.room.*
import com.d4rkwell.workouttracker.data.local.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Query("SELECT * FROM exercises WHERE dayId = :dayId ORDER BY orderIndex ASC")
    fun observeExercisesForDay(dayId: Long): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE exerciseId = :exerciseId")
    suspend fun getExercise(exerciseId: Long): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE exerciseId = :exerciseId")
    fun observeExercise(exerciseId: Long): Flow<ExerciseEntity?>

    @Query("SELECT COALESCE(MAX(orderIndex), -1) FROM exercises WHERE dayId = :dayId")
    suspend fun getMaxOrderIndex(dayId: Long): Int

    @Query("SELECT * FROM exercises WHERE name = :name ORDER BY exerciseId DESC LIMIT 1")
    suspend fun findMostRecentByName(name: String): ExerciseEntity?

    @Insert
    suspend fun insert(exercise: ExerciseEntity): Long

    @Update
    suspend fun update(exercise: ExerciseEntity)

    @Delete
    suspend fun delete(exercise: ExerciseEntity)
}
