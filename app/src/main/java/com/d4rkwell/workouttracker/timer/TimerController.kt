package com.d4rkwell.workouttracker.timer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-wide singleton that drives the rest/warmup countdown. Kept as a
 * single shared instance (not per-screen) so the countdown keeps
 * ticking correctly even if the user navigates between screens (e.g.
 * rest timer auto-starts after logging a set, user checks something
 * else, comes back — timer state is still correct).
 *
 * Deliberately simple (no foreground service) — the timer is driven by
 * a CountDownTimer on the main thread while the app is alive, which is
 * the right tradeoff for a workout companion the user is actively
 * holding/looking at between sets.
 */
class TimerController(private val appContext: Context) {

    private var countDownTimer: CountDownTimer? = null

    private val _state = MutableStateFlow(TimerUiState())
    val state: StateFlow<TimerUiState> = _state.asStateFlow()

    fun start(type: TimerType, totalSeconds: Int, label: String) {
        if (totalSeconds <= 0) return
        cancelInternal()

        _state.value = TimerUiState(
            isRunning = true,
            type = type,
            totalSeconds = totalSeconds,
            remainingSeconds = totalSeconds,
            label = label
        )

        countDownTimer = object : CountDownTimer(totalSeconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = ((millisUntilFinished + 999) / 1000).toInt()
                _state.value = _state.value.copy(remainingSeconds = secondsLeft)
            }

            override fun onFinish() {
                _state.value = _state.value.copy(remainingSeconds = 0)
                playFinishSignal()
            }
        }.start()
    }

    /** Add (or subtract, with a negative value) seconds to the currently running timer. */
    fun adjustTime(deltaSeconds: Int) {
        val current = _state.value
        if (current.type == null) return
        val newRemaining = (current.remainingSeconds + deltaSeconds).coerceAtLeast(0)
        val newTotal = maxOf(current.totalSeconds, newRemaining)
        if (newRemaining <= 0) {
            cancelInternal()
            _state.value = current.copy(remainingSeconds = 0, totalSeconds = newTotal)
            return
        }
        start(current.type, newRemaining, current.label).also {
            // start() resets totalSeconds to newRemaining; restore the larger total so
            // the progress ring reflects the original duration rather than resetting to full.
            _state.value = _state.value.copy(totalSeconds = newTotal)
        }
    }

    fun skip() {
        cancelInternal()
        _state.value = TimerUiState()
    }

    fun dismissFinished() {
        cancelInternal()
        _state.value = TimerUiState()
    }

    private fun cancelInternal() {
        countDownTimer?.cancel()
        countDownTimer = null
    }

    private fun playFinishSignal() {
        vibrate()
        playTone()
    }

    private fun vibrate() {
        try {
            val vibrator: Vibrator = if (android.os.Build.VERSION.SDK_INT >= 31) {
                val vm = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 250, 150, 250, 150, 400), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 250, 150, 250, 150, 400), -1)
            }
        } catch (_: Exception) {
            // Vibration is a nice-to-have; never crash the timer over it.
        }
    }

    private fun playTone() {
        try {
            val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
            tg.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 600)
        } catch (_: Exception) {
            // Audio can legitimately fail (silent mode handling etc.) — non-fatal.
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: TimerController? = null

        fun getInstance(context: Context): TimerController {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TimerController(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
