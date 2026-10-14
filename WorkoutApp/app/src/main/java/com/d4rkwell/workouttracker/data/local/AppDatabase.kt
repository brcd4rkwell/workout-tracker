package com.d4rkwell.workouttracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.d4rkwell.workouttracker.data.local.dao.DayDao
import com.d4rkwell.workouttracker.data.local.dao.ExerciseDao
import com.d4rkwell.workouttracker.data.local.dao.LoggedSetDao
import com.d4rkwell.workouttracker.data.local.dao.WeekDao
import com.d4rkwell.workouttracker.data.local.dao.WorkoutSessionDao
import com.d4rkwell.workouttracker.data.local.entity.DayEntity
import com.d4rkwell.workouttracker.data.local.entity.ExerciseEntity
import com.d4rkwell.workouttracker.data.local.entity.LoggedSetEntity
import com.d4rkwell.workouttracker.data.local.entity.WeekEntity
import com.d4rkwell.workouttracker.data.local.entity.WorkoutSessionEntity

/**
 * The single local database for the whole app. Everything lives on-device
 * only — there is no network/sync layer, by design (fully offline app).
 */
@Database(
    entities = [
        WeekEntity::class,
        DayEntity::class,
        ExerciseEntity::class,
        WorkoutSessionEntity::class,
        LoggedSetEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun weekDao(): WeekDao
    abstract fun dayDao(): DayDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun loggedSetDao(): LoggedSetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "workout_tracker.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
