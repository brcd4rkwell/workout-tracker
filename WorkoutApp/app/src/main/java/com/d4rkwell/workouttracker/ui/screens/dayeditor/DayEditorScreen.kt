package com.d4rkwell.workouttracker.ui.screens.dayeditor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d4rkwell.workouttracker.data.local.entity.ExerciseEntity
import com.d4rkwell.workouttracker.ui.components.ConfirmDialog
import com.d4rkwell.workouttracker.ui.components.EmptyState
import com.d4rkwell.workouttracker.ui.components.ExercisePlanCard
import com.d4rkwell.workouttracker.ui.components.TextFieldDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayEditorScreen(
    viewModel: DayEditorViewModel,
    onBack: () -> Unit,
    onAddExercise: () -> Unit,
    onOpenExercise: (Long) -> Unit,
    onStartWorkout: () -> Unit
) {
    val dayWithExercises by viewModel.dayWithExercises.collectAsState()
    var showRenameDialog by remember { mutableStateOf(false) }
    var showWarmupDialog by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var exercisePendingDelete by remember { mutableStateOf<ExerciseEntity?>(null) }

    val day = dayWithExercises?.day
    val exercises = dayWithExercises?.exercises.orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(day?.title ?: "Day") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showNoteDialog = true }) {
                        Icon(Icons.Filled.EditNote, contentDescription = "Day note")
                    }
                    IconButton(onClick = { showRenameDialog = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Rename day")
                    }
                }
            )
        },
        floatingActionButton = {
            if (exercises.isEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onAddExercise,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Add Exercise") }
                )
            }
        },
        bottomBar = {
            if (exercises.isNotEmpty()) {
                BottomAppBar(
                    actions = {
                        IconButton(onClick = onAddExercise) {
                            Icon(Icons.Filled.Add, contentDescription = "Add exercise")
                        }
                    },
                    floatingActionButton = {
                        ExtendedFloatingActionButton(
                            onClick = onStartWorkout,
                            icon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                            text = { Text("Start Workout") },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // Warmup + Start Workout control strip
            if (day != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Whatshot, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Warm-up", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${day.warmupMinutes} min before you start",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { showWarmupDialog = true }) { Text("Edit") }
                    }
                }

                if (day.note.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = day.note,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            if (exercises.isEmpty()) {
                EmptyState(
                    title = "No exercises yet",
                    subtitle = "Add exercises with their sets, reps, and rest time. You can edit these any time.",
                    actionLabel = "Add Exercise",
                    onAction = onAddExercise,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(exercises, key = { it.exerciseId }) { exercise ->
                        ExercisePlanCard(
                            exercise = exercise,
                            onClick = { onOpenExercise(exercise.exerciseId) },
                            onDeleteRequest = { exercisePendingDelete = exercise }
                        )
                    }
                }
            }
        }
    }

    if (showRenameDialog && day != null) {
        TextFieldDialog(
            title = "Rename Day",
            label = "Day title",
            initialValue = day.title,
            confirmLabel = "Save",
            onDismiss = { showRenameDialog = false },
            onConfirm = { newTitle ->
                showRenameDialog = false
                viewModel.renameDay(day, newTitle)
            }
        )
    }

    if (showWarmupDialog && day != null) {
        TextFieldDialog(
            title = "Warm-up Duration",
            label = "Minutes",
            initialValue = day.warmupMinutes.toString(),
            confirmLabel = "Save",
            onDismiss = { showWarmupDialog = false },
            onConfirm = { value ->
                showWarmupDialog = false
                value.toIntOrNull()?.let { viewModel.updateWarmupMinutes(day, it) }
            }
        )
    }

    if (showNoteDialog && day != null) {
        TextFieldDialog(
            title = "Day Note",
            label = "Note for this day",
            initialValue = day.note,
            confirmLabel = "Save",
            onDismiss = { showNoteDialog = false },
            onConfirm = { note ->
                showNoteDialog = false
                viewModel.updateDayNote(day, note)
            }
        )
    }

    exercisePendingDelete?.let { target ->
        ConfirmDialog(
            title = "Delete ${target.name}?",
            message = "This removes it from this day's plan. Past logged sets for this exercise are kept in your history.",
            onDismiss = { exercisePendingDelete = null },
            onConfirm = {
                viewModel.deleteExercise(target)
                exercisePendingDelete = null
            }
        )
    }
}
