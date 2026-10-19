package com.d4rkwell.workouttracker.ui.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d4rkwell.workouttracker.ui.components.ConfirmDialog
import com.d4rkwell.workouttracker.ui.components.EmptyState
import com.d4rkwell.workouttracker.util.formatDate
import com.d4rkwell.workouttracker.util.formatDurationBetween

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBack: () -> Unit,
    onOpenSession: (Long) -> Unit
) {
    val sessions by viewModel.sessions.collectAsState()
    var sessionPendingDelete by remember { mutableStateOf<HistorySessionRow?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (sessions.isEmpty()) {
            EmptyState(
                title = "No workouts logged yet",
                subtitle = "Completed workouts will show up here so you can look back on your progress.",
                modifier = Modifier.padding(padding).fillMaxSize()
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sessions, key = { it.session.sessionId }) { row ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenSession(row.session.sessionId) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(row.dayTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    formatDate(row.session.startedAtEpochMillis),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "Duration: ${formatDurationBetween(row.session.startedAtEpochMillis, row.session.endedAtEpochMillis)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { sessionPendingDelete = row }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete this workout")
                            }
                            Icon(
                                Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    sessionPendingDelete?.let { target ->
        ConfirmDialog(
            title = "Delete this workout?",
            message = "This permanently removes the ${target.dayTitle} session from ${formatDate(target.session.startedAtEpochMillis)}, including every set you logged in it.",
            onDismiss = { sessionPendingDelete = null },
            onConfirm = {
                viewModel.deleteSession(target.session)
                sessionPendingDelete = null
            }
        )
    }
}
