package com.d4rkwell.workouttracker.ui.screens.workoutsession

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.d4rkwell.workouttracker.timer.TimerUiState
import com.d4rkwell.workouttracker.ui.components.CircularTimerDisplay

@Composable
fun WarmupStage(
    warmupMinutes: Int,
    timerState: TimerUiState,
    onStartTimer: () -> Unit,
    onAddTime: (Int) -> Unit,
    onSkip: () -> Unit,
    onBeginWorkout: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Warm-up",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Get your body ready before the first exercise.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        val isRunning = timerState.isRunning
        val isFinished = isRunning && timerState.remainingSeconds <= 0

        CircularTimerDisplay(
            progress = if (isRunning) timerState.progress else 1f,
            remainingSeconds = if (isRunning) timerState.remainingSeconds else warmupMinutes * 60,
            label = if (isFinished) "Done!" else "",
        )

        Spacer(Modifier.height(32.dp))

        if (!isRunning) {
            Button(
                onClick = onStartTimer,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Start Warm-up Timer")
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { onAddTime(30) },
                    modifier = Modifier.weight(1f)
                ) { Text("+30s") }
                OutlinedButton(
                    onClick = { onAddTime(-30) },
                    modifier = Modifier.weight(1f)
                ) { Text("-30s") }
            }
        }

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = onSkip) {
            Icon(Icons.Filled.SkipNext, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Skip warm-up")
        }

        Spacer(Modifier.height(8.dp))

        if (isFinished) {
            Button(
                onClick = onBeginWorkout,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Start Workout")
            }
        }
    }
}
