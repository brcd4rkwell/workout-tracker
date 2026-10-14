package com.d4rkwell.workouttracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d4rkwell.workouttracker.data.local.entity.ExerciseEntity
import com.d4rkwell.workouttracker.util.formatRepRange
import com.d4rkwell.workouttracker.util.formatSeconds

@Composable
fun ExercisePlanCard(
    exercise: ExerciseEntity,
    onClick: () -> Unit,
    onDeleteRequest: () -> Unit,
    showDragHandle: Boolean = false
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showDragHandle) {
                Icon(
                    Icons.Filled.DragHandle,
                    contentDescription = "Reorder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 10.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatChip(label = "${exercise.targetSets} sets")
                    Spacer(Modifier.width(6.dp))
                    StatChip(label = "${formatRepRange(exercise.targetRepsLow, exercise.targetRepsHigh)} reps")
                    Spacer(Modifier.width(6.dp))
                    StatChip(label = "${formatSeconds(exercise.restSeconds)} rest")
                }
                if (exercise.note.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "📝 ${exercise.note}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Exercise options")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Delete exercise") },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onDeleteRequest()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
