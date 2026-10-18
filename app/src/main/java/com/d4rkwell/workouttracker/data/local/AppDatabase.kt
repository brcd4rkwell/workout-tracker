package com.d4rkwell.workouttracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.d4rkwell.workouttracker.data.local.dao.DayDao
import com.d4rkwell.workouttracker.data.local.dao.ExerciseDao
import com.d4rkwell.workouttracker.data.local.dao.LoggedSetDao
import com.d4rkwell.workouttracker.data.local.dao.WeekDao
import com.d4rkwell.workouttracker.data.local.dao.WeightEntryDao
import com.d4rkwell.workouttracker.data.local.dao.WorkoutSessionDao
import com.d4rkwell.workouttracker.data.local.entity.DayEntity
import com.d4rkwell.workouttracker.data.local.entity.ExerciseEntity
import com.d4rkwell.workouttracker.data.local.entity.LoggedSetEntity
import com.d4rkwell.workouttracker.data.local.entity.WeekEntity
import com.d4rkwell.workouttracker.data.local.entity.WeightEntryEntity
import com.d4rkwell.workouttracker.data.local.entity.WorkoutSessionEntity

/**
 * The single local database for the whole app. Everything lives on-device
 * only — there is no network/sync layer, by design (fully offline app).
 *
 * Schema version bumped to 2 for the warmup/working-sets split, the
 * exercise video link, and bodyweight tracking. Since this app has no
 * released users yet (early development), we use destructive fallback
 * rather than a hand-written migration — an app update will reset the
 * local database once. If this app is already in real use by the time
 * you read this comment, replace fallbackToDestructiveMigration() with a
 * proper Migration(1, 2) instead, to preserve existing data.
 */
@Database(
    entities = [
        WeekEntity::class,
        DayEntity::class,
        ExerciseEntity::class,
        WorkoutSessionEntity::class,
        LoggedSetEntity::class,
        WeightEntryEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun weekDao(): WeekDao
    abstract fun dayDao(): DayDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun loggedSetDao(): LoggedSetDao
    abstract fun weightEntryDao(): WeightEntryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "workout_tracker.db"
                )
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
