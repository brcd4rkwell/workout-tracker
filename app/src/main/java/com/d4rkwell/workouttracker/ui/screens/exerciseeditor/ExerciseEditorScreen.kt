package com.d4rkwell.workouttracker.ui.screens.exerciseeditor

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.d4rkwell.workouttracker.util.formatSeconds

private val RestPresets = listOf(60, 90, 120, 150, 180, 240, 300) // 1m, 1:30, 2m, 2:30, 3m, 4m, 5m

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseEditorScreen(
    viewModel: ExerciseEditorViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val form by viewModel.form.collectAsState()
    val context = LocalContext.current

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

            Text("Warmup & Working Sets", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = form.warmupSets,
                    onValueChange = viewModel::updateWarmupSets,
                    label = { Text("Warmup sets") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = form.workingSets,
                    onValueChange = viewModel::updateWorkingSets,
                    label = { Text("Working sets") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                "Warmup sets are light ramp-up sets logged before your working sets. Set to 0 if this exercise doesn't need any.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text("Reps", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
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

            Text("How-To Video", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = form.videoUrl,
                onValueChange = viewModel::updateVideoUrl,
                label = { Text("YouTube link (optional)") },
                placeholder = { Text("https://youtube.com/watch?v=...") },
                singleLine = true,
                trailingIcon = {
                    if (form.videoUrl.isNotBlank()) {
                        IconButton(onClick = {
                            try {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(form.videoUrl))
                                )
                            } catch (_: Exception) {
                                // Malformed or unopenable link — silently ignore rather than crash;
                                // the person can fix the URL text directly.
                            }
                        }) {
                            Icon(Icons.Filled.PlayCircle, contentDescription = "Watch video")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Shows a Watch button next to this exercise during your workout.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
