package com.d4rkwell.workouttracker.ui.screens.exerciseeditor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.d4rkwell.workouttracker.util.formatSeconds

private val RestPresets = listOf(30, 60, 90, 120, 180)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseEditorScreen(
    viewModel: ExerciseEditorViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val form by viewModel.form.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (form.isEditing) "Edit Exercise" else "Add Exercise") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (!form.isLoaded) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = form.name,
                onValueChange = viewModel::updateName,
                label = { Text("Exercise name") },
                placeholder = { Text("e.g. Barbell Bench Press") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = form.muscleGroup,
                onValueChange = viewModel::updateMuscleGroup,
                label = { Text("Muscle group (optional)") },
                placeholder = { Text("e.g. Chest") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Sets & Reps", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = form.targetSets,
                    onValueChange = viewModel::updateSets,
                    label = { Text("Sets") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = form.targetRepsLow,
                    onValueChange = viewModel::updateRepsLow,
                    label = { Text("Reps (low)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = form.targetRepsHigh,
                    onValueChange = viewModel::updateRepsHigh,
                    label = { Text("Reps (high)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                "Tip: set both rep fields the same for a fixed rep target (e.g. 10 / 10).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text("Rest Between Sets", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

            OutlinedTextField(
                value = form.restSeconds,
                onValueChange = viewModel::updateRestSeconds,
                label = { Text("Rest (seconds)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RestPresets.forEach { preset ->
                    val selected = form.restSeconds.toIntOrNull() == preset
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.updateRestSeconds(preset.toString()) },
                        label = { Text(formatSeconds(preset)) }
                    )
                }
            }

            Text("Your Note", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = form.note,
                onValueChange = viewModel::updateNote,
                label = { Text("Note (shown next to the exercise name)") },
                placeholder = { Text("e.g. \"keep elbows tucked\", \"use the cambered bar\"") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { viewModel.save(onSaved) },
                enabled = viewModel.isValid,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (form.isEditing) "Save Changes" else "Add Exercise")
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
