package com.d4rkwell.workouttracker.ui.screens.workoutsession

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import com.d4rkwell.workouttracker.ui.components.ConfirmDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSessionScreen(
    viewModel: WorkoutSessionViewModel,
    onExit: () -> Unit,
    onFinished: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val timerState by viewModel.timerState.collectAsState()
    val draft by viewModel.draft.collectAsState()
    val loggedSets by viewModel.loggedSetsForCurrentExercise.collectAsState()

    var showExitConfirm by remember { mutableStateOf(false) }

    // Keep the screen on for the duration of the workout session.
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.dayTitle.ifBlank { "Workout" }) },
                navigationIcon = {
                    IconButton(onClick = { showExitConfirm = true }) {
                        Icon(Icons.Filled.Close, contentDescription = "Exit workout")
                    }
                }
            )
        }
    ) { padding ->
        if (!uiState.isLoaded) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            when (uiState.stage) {
                SessionStage.WARMUP -> WarmupStage(
                    warmupMinutes = uiState.warmupMinutes,
                    timerState = timerState,
                    onStartTimer = viewModel::startWarmupTimer,
                    onAddTime = viewModel::addTimerSeconds,
                    onSkip = viewModel::skipWarmup,
                    onBeginWorkout = viewModel::finishWarmupAndBegin
                )

                SessionStage.EXERCISES -> {
                    val exercise = uiState.currentExercise
                    if (exercise != null) {
                        ExerciseStage(
                            exercise = exercise,
                            exerciseIndex = uiState.currentExerciseIndex,
                            totalExercises = uiState.exercises.size,
                            loggedSets = loggedSets,
                            previousBest = uiState.previousBestForCurrentExercise,
                            draftWeight = draft.weight,
                            draftReps = draft.reps,
                            draftNote = draft.note,
                            timerState = timerState,
                            onDraftWeightChange = viewModel::updateDraftWeight,
                            onDraftRepsChange = viewModel::updateDraftReps,
                            onDraftNoteChange = viewModel::updateDraftNote,
                            onLogSet = viewModel::logCurrentSet,
                            onDeleteSet = viewModel::deleteLoggedSet,
                            onSkipTimer = viewModel::skipTimer,
                            onAddTimerSeconds = viewModel::addTimerSeconds,
                            onPrevExercise = viewModel::goToPreviousExercise,
                            onNextExercise = viewModel::goToNextExercise,
                            canGoPrev = uiState.currentExerciseIndex > 0,
                            isLastExercise = uiState.isLastExercise
                        )
                    }
                }

                SessionStage.FINISHED -> {
                    val sessionTotalSets by viewModel.totalLoggedSetsForSession.collectAsState()
                    FinishedStage(
                        dayTitle = uiState.dayTitle,
                        startedAtMillis = uiState.sessionStartedAtMillis,
                        endedAtMillis = System.currentTimeMillis(),
                        totalSetsLogged = sessionTotalSets,
                        onDone = onFinished
                    )
                }
            }
        }
    }

    if (showExitConfirm) {
        ConfirmDialog(
            title = "Exit workout?",
            message = "Your progress so far is saved. You can resume this workout later from the day screen.",
            confirmLabel = "Exit",
            isDestructive = false,
            onDismiss = { showExitConfirm = false },
            onConfirm = {
                showExitConfirm = false
                onExit()
            }
        )
    }
}
