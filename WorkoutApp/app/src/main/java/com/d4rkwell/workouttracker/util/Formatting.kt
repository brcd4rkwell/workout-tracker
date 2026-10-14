package com.d4rkwell.workouttracker.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/** Formats seconds as m:ss (e.g. 90 -> "1:30"), or h:mm:ss if an hour or more. */
fun formatSeconds(totalSeconds: Int): String {
    val s = abs(totalSeconds)
    val hours = s / 3600
    val minutes = (s % 3600) / 60
    val secs = s % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, secs)
    }
}

/** Formats a rep range, collapsing to a single number when low == high. */
fun formatRepRange(low: Int, high: Int): String =
    if (low == high) "$low" else "$low–$high"

/** Formats a weight value, dropping the decimal when it's a whole number. */
fun formatWeight(weight: Double): String {
    return if (weight == weight.toLong().toDouble()) {
        weight.toLong().toString()
    } else {
        String.format(Locale.US, "%.1f", weight)
    }
}

fun formatDate(epochMillis: Long): String {
    val sdf = SimpleDateFormat("EEE, MMM d yyyy", Locale.getDefault())
    return sdf.format(Date(epochMillis))
}

fun formatDateTime(epochMillis: Long): String {
    val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return sdf.format(Date(epochMillis))
}

fun formatDurationBetween(startMillis: Long, endMillis: Long?): String {
    if (endMillis == null) return "In progress"
    val totalSeconds = ((endMillis - startMillis) / 1000).toInt().coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
}
