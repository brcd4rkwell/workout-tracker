package com.d4rkwell.workouttracker.backup

import android.content.Context
import android.net.Uri
import com.d4rkwell.workouttracker.data.local.AppDatabase
import com.d4rkwell.workouttracker.data.local.entity.DayEntity
import com.d4rkwell.workouttracker.data.local.entity.ExerciseEntity
import com.d4rkwell.workouttracker.data.local.entity.LoggedSetEntity
import com.d4rkwell.workouttracker.data.local.entity.WeekEntity
import com.d4rkwell.workouttracker.data.local.entity.WeightEntryEntity
import com.d4rkwell.workouttracker.data.local.entity.WorkoutSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Exports the entire local database (your program plan AND your logged
 * history) to a single human-inspectable JSON file, and restores it back.
 * This is the backup/restore feature, and doubles as the "export/import
 * your program" feature — importing a file someone else made (e.g. a
 * spreadsheet converted to this JSON shape) loads it as your program too.
 *
 * Deliberately plain org.json (built into Android, no extra library
 * needed) rather than a serialization library, since this backup format
 * needs to stay stable and easy to hand-edit if someone wants to build a
 * backup file from a spreadheet by hand.
 */
object BackupManager {

    fun suggestedFileName(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return "workout-tracker-backup-${sdf.format(java.util.Date())}.json"
    }

    suspend fun exportToUri(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val json = buildBackupJson(db)
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(json.toString(2).toByteArray(Charsets.UTF_8))
        } ?: throw IllegalStateException("Could not open the chosen file for writing")
    }

    /**
     * Reads a backup file and replaces the current database contents with
     * it. [merge] = false wipes existing data first (a true restore);
     * [merge] = true adds the file's weeks/days/exercises alongside what's
     * already there (useful for importing a program someone shared, without
     * losing your own history).
     */
    suspend fun importFromUri(context: Context, uri: Uri, merge: Boolean): Int = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes().toString(Charsets.UTF_8)
        } ?: throw IllegalStateException("Could not open the chosen file for reading")

        val json = JSONObject(text)
        val db = AppDatabase.getInstance(context)
        restoreFromJson(db, json, merge)
    }

    // ---------- Export ----------

    private suspend fun buildBackupJson(db: AppDatabase): JSONObject {
        val weeks = db.weekDao().observeAllWeeks().firstValue()
        val days = weeks.flatMap { week -> db.dayDao().observeDaysForWeek(week.weekId).firstValue() }
        val exercises = days.flatMap { day -> db.exerciseDao().observeExercisesForDay(day.dayId).firstValue() }
        val sessions = db.workoutSessionDao().observeCompletedSessions().firstValue()
        val loggedSets = sessions.flatMap { session ->
            db.loggedSetDao().observeSetsForSession(session.sessionId).firstValue()
        }
        val weightEntries = db.weightEntryDao().observeAllEntries().firstValue()

        return JSONObject().apply {
            put("backupFormatVersion", BACKUP_FORMAT_VERSION)
            put("exportedAtEpochMillis", System.currentTimeMillis())
            put("weeks", JSONArray(weeks.map { it.toJson() }))
            put("days", JSONArray(days.map { it.toJson() }))
            put("exercises", JSONArray(exercises.map { it.toJson() }))
            put("sessions", JSONArray(sessions.map { it.toJson() }))
            put("loggedSets", JSONArray(loggedSets.map { it.toJson() }))
            put("weightEntries", JSONArray(weightEntries.map { it.toJson() }))
        }
    }

    // ---------- Import ----------

    private suspend fun restoreFromJson(db: AppDatabase, json: JSONObject, merge: Boolean): Int {
        if (!merge) {
            db.clearAllTables()
        }

        // Old id -> new id maps, since a fresh import must not collide with
        // existing primary keys when merging.
        val weekIdMap = HashMap<Long, Long>()
        val dayIdMap = HashMap<Long, Long>()
        val exerciseIdMap = HashMap<Long, Long>()
        val sessionIdMap = HashMap<Long, Long>()
        var importedCount = 0

        val weeksArray = json.optJSONArray("weeks") ?: JSONArray()
        for (i in 0 until weeksArray.length()) {
            val obj = weeksArray.getJSONObject(i)
            val oldId = obj.getLong("weekId")
            val newId = db.weekDao().insert(
                WeekEntity(
                    orderIndex = obj.getInt("orderIndex"),
                    title = obj.getString("title"),
                    note = obj.optString("note", "")
                )
            )
            weekIdMap[oldId] = newId
            importedCount++
        }

        val daysArray = json.optJSONArray("days") ?: JSONArray()
        for (i in 0 until daysArray.length()) {
            val obj = daysArray.getJSONObject(i)
            val oldId = obj.getLong("dayId")
            val oldWeekId = obj.getLong("weekId")
            val newWeekId = weekIdMap[oldWeekId] ?: continue
            val newId = db.dayDao().insert(
                DayEntity(
                    weekId = newWeekId,
                    orderIndex = obj.getInt("orderIndex"),
                    title = obj.getString("title"),
                    warmupMinutes = obj.optInt("warmupMinutes", 5),
                    note = obj.optString("note", "")
                )
            )
            dayIdMap[oldId] = newId
            importedCount++
        }

        val exercisesArray = json.optJSONArray("exercises") ?: JSONArray()
        for (i in 0 until exercisesArray.length()) {
            val obj = exercisesArray.getJSONObject(i)
            val oldId = obj.getLong("exerciseId")
            val oldDayId = obj.getLong("dayId")
            val newDayId = dayIdMap[oldDayId] ?: continue
            val newId = db.exerciseDao().insert(
                ExerciseEntity(
                    dayId = newDayId,
                    orderIndex = obj.getInt("orderIndex"),
                    name = obj.getString("name"),
                    warmupSets = obj.optInt("warmupSets", 0),
                    workingSets = obj.getInt("workingSets"),
                    targetRepsLow = obj.getInt("targetRepsLow"),
                    targetRepsHigh = obj.getInt("targetRepsHigh"),
                    restSeconds = obj.getInt("restSeconds"),
                    note = obj.optString("note", ""),
                    videoUrl = obj.optString("videoUrl", "")
                )
            )
            exerciseIdMap[oldId] = newId
            importedCount++
        }

        val sessionsArray = json.optJSONArray("sessions") ?: JSONArray()
        for (i in 0 until sessionsArray.length()) {
            val obj = sessionsArray.getJSONObject(i)
            val oldId = obj.getLong("sessionId")
            val oldDayId = obj.getLong("dayId")
            // A session whose day wasn't part of this import (e.g. a plan-only
            // export) still gets restored — it just keeps its original dayId as
            // a dangling reference, since session/history rows are intentionally
            // not foreign-keyed to the plan (see WorkoutSessionEntity).
            val newDayId = dayIdMap[oldDayId] ?: oldDayId
            val newId = db.workoutSessionDao().insert(
                WorkoutSessionEntity(
                    dayId = newDayId,
                    dayTitle = obj.optString("dayTitle", ""),
                    startedAtEpochMillis = obj.getLong("startedAtEpochMillis"),
                    endedAtEpochMillis = if (obj.isNull("endedAtEpochMillis")) null else obj.getLong("endedAtEpochMillis"),
                    isCompleted = obj.optBoolean("isCompleted", true)
                )
            )
            sessionIdMap[oldId] = newId
            importedCount++
        }

        val loggedSetsArray = json.optJSONArray("loggedSets") ?: JSONArray()
        for (i in 0 until loggedSetsArray.length()) {
            val obj = loggedSetsArray.getJSONObject(i)
            val oldSessionId = obj.getLong("sessionId")
            val oldExerciseId = obj.getLong("exerciseId")
            val newSessionId = sessionIdMap[oldSessionId] ?: continue
            val newExerciseId = exerciseIdMap[oldExerciseId] ?: oldExerciseId
            db.loggedSetDao().insert(
                LoggedSetEntity(
                    sessionId = newSessionId,
                    exerciseId = newExerciseId,
                    exerciseName = obj.optString("exerciseName", ""),
                    isWarmup = obj.optBoolean("isWarmup", false),
                    setNumber = obj.getInt("setNumber"),
                    weight = obj.getDouble("weight"),
                    reps = obj.getInt("reps"),
                    note = obj.optString("note", ""),
                    loggedAtEpochMillis = obj.getLong("loggedAtEpochMillis")
                )
            )
            importedCount++
        }

        val weightEntriesArray = json.optJSONArray("weightEntries") ?: JSONArray()
        for (i in 0 until weightEntriesArray.length()) {
            val obj = weightEntriesArray.getJSONObject(i)
            db.weightEntryDao().insert(
                WeightEntryEntity(
                    weight = obj.getDouble("weight"),
                    note = obj.optString("note", ""),
                    loggedAtEpochMillis = obj.getLong("loggedAtEpochMillis")
                )
            )
            importedCount++
        }

        return importedCount
    }

    // ---------- Entity <-> JSON ----------

    private fun WeekEntity.toJson() = JSONObject().apply {
        put("weekId", weekId)
        put("orderIndex", orderIndex)
        put("title", title)
        put("note", note)
    }

    private fun DayEntity.toJson() = JSONObject().apply {
        put("dayId", dayId)
        put("weekId", weekId)
        put("orderIndex", orderIndex)
        put("title", title)
        put("warmupMinutes", warmupMinutes)
        put("note", note)
    }

    private fun ExerciseEntity.toJson() = JSONObject().apply {
        put("exerciseId", exerciseId)
        put("dayId", dayId)
        put("orderIndex", orderIndex)
        put("name", name)
        put("warmupSets", warmupSets)
        put("workingSets", workingSets)
        put("targetRepsLow", targetRepsLow)
        put("targetRepsHigh", targetRepsHigh)
        put("restSeconds", restSeconds)
        put("note", note)
        put("videoUrl", videoUrl)
    }

    private fun WorkoutSessionEntity.toJson() = JSONObject().apply {
        put("sessionId", sessionId)
        put("dayId", dayId)
        put("dayTitle", dayTitle)
        put("startedAtEpochMillis", startedAtEpochMillis)
        put("endedAtEpochMillis", endedAtEpochMillis ?: JSONObject.NULL)
        put("isCompleted", isCompleted)
    }

    private fun LoggedSetEntity.toJson() = JSONObject().apply {
        put("loggedSetId", loggedSetId)
        put("sessionId", sessionId)
        put("exerciseId", exerciseId)
        put("exerciseName", exerciseName)
        put("isWarmup", isWarmup)
        put("setNumber", setNumber)
        put("weight", weight)
        put("reps", reps)
        put("note", note)
        put("loggedAtEpochMillis", loggedAtEpochMillis)
    }

    private fun WeightEntryEntity.toJson() = JSONObject().apply {
        put("weightEntryId", weightEntryId)
        put("weight", weight)
        put("note", note)
        put("loggedAtEpochMillis", loggedAtEpochMillis)
    }
}

/** Small helper: get the first (current) value of a Flow without collecting it as a StateFlow. */
private suspend fun <T> Flow<T>.firstValue(): T = this.first()
