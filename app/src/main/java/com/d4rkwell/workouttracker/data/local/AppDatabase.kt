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
 * Schema version 4: added logged_sets.extraParts (for logging a drop set —
 * several weight×reps parts — as one logged set). Still using destructive
 * fallback rather than a hand-written migration (this app is still in
 * active early development with no external users) — an update to this
 * version wipes the local database once. The app now has a Backup
 * feature (see the backup/ package): back up before installing this
 * update, then restore afterward, to avoid losing anything. If this app
 * reaches real outside users before the next schema change, replace
 * fallbackToDestructiveMigration() with a proper Migration() instead.
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
    version = 4,
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
