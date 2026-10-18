package com.d4rkwell.workouttracker.backup

/**
 * Schema version of the backup file format itself (independent of the
 * Room database version) — bump this only if the JSON shape below
 * changes, so a future version of the app can detect and handle older
 * backup files gracefully.
 */
const val BACKUP_FORMAT_VERSION = 1
