package com.d4rkwell.workouttracker.ui.screens.workoutsession

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.d4rkwell.workouttracker.data.local.entity.ExerciseEntity
import com.d4rkwell.workouttracker.data.local.entity.LoggedSetEntity
import com.d4rkwell.workouttracker.timer.TimerUiState
import com.d4rkwell.workouttracker.ui.components.CircularTimerDisplay
import com.d4rkwell.workouttracker.util.formatRepRange
import com.d4rkwell.workouttracker.util.formatSeconds
import com.d4rkwell.workouttracker.util.formatWeight

@Composable
fun ExerciseStage(
    exercise: ExerciseEntity,
    exerciseIndex: Int,
    totalExercises: Int,
    loggedWarmupSets: List<LoggedSetEntity>,
    loggedWorkingSets: List<LoggedSetEntity>,
    isInWarmupSubstage: Boolean,
    previousBest: LoggedSetEntity?,
    draftWeight: String,
    draftReps: String,
    draftNote: String,
    timerState: TimerUiState,
    onDraftWeightChange: (String) -> Unit,
    onDraftRepsChange: (String) -> Unit,
    onDraftNoteChange: (String) -> Unit,
    onLogSet: () -> Unit,
    onDeleteSet: (LoggedSetEntity) -> Unit,
    onSkipTimer: () -> Unit,
    onAddTimerSeconds: (Int) -> Unit,
    onPrevExercise: () -> Unit,
    onNextExercise: () -> Unit,
    onWatchVideo: () -> Unit,
    canGoPrev: Boolean,
    isLastExercise: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Progress indicator across exercises
        Text(
            "Exercise ${exerciseIndex + 1} of $totalExercises",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                exercise.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (exercise.videoUrl.isNotBlank()) {
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = onWatchVideo, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                    Icon(Icons.Filled.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Watch")
                }
            }
        }

        if (exercise.note.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    "📝 ${exercise.note}",
                    modifier = Modifier.padding(10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Row {
            if (exercise.warmupSets > 0) {
                InfoChip("${exercise.warmupSets} warmup")
                Spacer(Modifier.width(6.dp))
            }
            InfoChip("${exercise.workingSets} working")
            Spacer(Modifier.width(6.dp))
            InfoChip("${formatRepRange(exercise.targetRepsLow, exercise.targetRepsHigh)} reps")
            Spacer(Modifier.width(6.dp))
            InfoChip("${formatSeconds(exercise.restSeconds)} rest")
        }

        if (previousBest != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Last time: ${formatWeight(previousBest.weight)} × ${previousBest.reps}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(16.dp))

        // Rest timer banner — shown whenever a rest timer is actively running
        if (timerState.isRunning && timerState.type == com.d4rkwell.workouttracker.timer.TimerType.REST) {
            RestTimerBanner(
                timerState = timerState,
                onAddTime = onAddTimerSeconds,
                onSkip = onSkipTimer
            )
            Spacer(Modifier.height(16.dp))
        }

        // Warmup sets section (only shown when this exercise has any)
        if (exercise.warmupSets > 0) {
            Text("Warmup sets", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            if (loggedWarmupSets.isEmpty()) {
                Text(
                    "No warmup sets logged yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    loggedWarmupSets.forEach { set ->
                        LoggedSetRow(set = set, onDelete = { onDeleteSet(set) })
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Working sets section
        Text("Working sets", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        if (loggedWorkingSets.isEmpty()) {
            Text(
                "No working sets logged yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                loggedWorkingSets.forEach { set ->
                    LoggedSetRow(set = set, onDelete = { onDeleteSet(set) })
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        val targetForCurrentSubstage = if (isInWarmupSubstage) exercise.warmupSets else exercise.workingSets
        val loggedForCurrentSubstage = if (isInWarmupSubstage) loggedWarmupSets.size else loggedWorkingSets.size
        val setsRemaining = targetForCurrentSubstage - loggedForCurrentSubstage

        if (setsRemaining > 0) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        if (isInWarmupSubstage)
                            "Log warmup set ${loggedForCurrentSubstage + 1} of $targetForCurrentSubstage"
                        else
                            "Log working set ${loggedForCurrentSubstage + 1} of $targetForCurrentSubstage",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(12.dp))
                    // Keying these fields by exercise id + warmup/working stage gives each
                    // one a distinct Compose identity, so the keyboard correctly switches
                    // to the numeric layout instead of getting stuck on whatever keyboard
                    // type a previously-focused text field (like the exercise name field)
                    // last used.
                    androidx.compose.runtime.key(exercise.exerciseId, isInWarmupSubstage) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = draftWeight,
                                onValueChange = onDraftWeightChange,
                                label = { Text("Weight") },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = androidx.compose.ui.text.input.ImeAction.Next
                                ),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = draftReps,
                                onValueChange = onDraftRepsChange,
                                label = { Text("Reps") },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = androidx.compose.ui.text.input.ImeAction.Done
                                ),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    com.d4rkwell.workouttracker.ui.components.ClearableTextField(
                        value = draftNote,
                        onValueChange = onDraftNoteChange,
                        label = "Note (optional)",
                        placeholder = "e.g. felt heavy, used belt",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = onLogSet,
                        enabled = draftWeight.toDoubleOrNull() != null && draftReps.toIntOrNull() != null,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isInWarmupSubstage) "Log Warmup Set" else "Log Set")
                    }
                }
            }
        } else {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "All sets logged for this exercise",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            if (canGoPrev) {
                OutlinedButton(
                    onClick = onPrevExercise,
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Previous")
                }
            }
            Button(
                onClick = onNextExercise,
                modifier = Modifier.weight(1f).height(52.dp)
            ) {
                Text(if (isLastExercise) "Finish Workout" else "Next Exercise")
                Spacer(Modifier.width(6.dp))
                Icon(
                    if (isLastExercise) Icons.Filled.Flag else Icons.Filled.ArrowForward,
                    contentDescription = null
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun RestTimerBanner(
    timerState: TimerUiState,
    onAddTime: (Int) -> Unit,
    onSkip: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Resting", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            CircularTimerDisplay(
                progress = timerState.progress,
                remainingSeconds = timerState.remainingSeconds,
                label = if (timerState.isFinished) "Time's up!" else "",
                size = 140.dp
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { onAddTime(15) }) { Text("+15s") }
                OutlinedButton(onClick = { onAddTime(-15) }) { Text("-15s") }
                TextButton(onClick = onSkip) {
                    Icon(Icons.Filled.SkipNext, contentDescription = null)
                    Text("Skip")
                }
            }
        }
    }
}

@Composable
private fun LoggedSetRow(set: LoggedSetEntity, onDelete: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${set.setNumber}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "${formatWeight(set.weight)} × ${set.reps}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            if (set.note.isNotBlank()) {
                Text(
                    set.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Remove set",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InfoChip(label: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
