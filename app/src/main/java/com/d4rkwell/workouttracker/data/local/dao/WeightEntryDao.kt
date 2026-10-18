package com.d4rkwell.workouttracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.d4rkwell.workouttracker.data.local.entity.WeightEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightEntryDao {

    @Query("SELECT * FROM weight_entries ORDER BY loggedAtEpochMillis ASC")
    fun observeAllEntries(): Flow<List<WeightEntryEntity>>

    @Query("SELECT * FROM weight_entries ORDER BY loggedAtEpochMillis DESC LIMIT 1")
    fun observeLatestEntry(): Flow<WeightEntryEntity?>

    @Query("SELECT * FROM weight_entries ORDER BY loggedAtEpochMillis ASC LIMIT 1")
    suspend fun getFirstEntry(): WeightEntryEntity?

    @Insert
    suspend fun insert(entry: WeightEntryEntity): Long

    @Update
    suspend fun update(entry: WeightEntryEntity)

    @Delete
    suspend fun delete(entry: WeightEntryEntity)
}
