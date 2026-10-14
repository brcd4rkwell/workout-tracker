package com.d4rkwell.workouttracker.timer

enum class TimerType {
    WARMUP,
    REST
}

data class TimerUiState(
    val isRunning: Boolean = false,
    val type: TimerType? = null,
    val totalSeconds: Int = 0,
    val remainingSeconds: Int = 0,
    val label: String = ""
) {
    val progress: Float
        get() = if (totalSeconds <= 0) 0f else remainingSeconds.toFloat() / totalSeconds.toFloat()

    val isFinished: Boolean
        get() = isRunning && remainingSeconds <= 0
}
