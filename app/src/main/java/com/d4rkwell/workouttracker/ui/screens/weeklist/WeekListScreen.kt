package com.d4rkwell.workouttracker.ui.screens.weeklist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.d4rkwell.workouttracker.data.local.entity.WeekWithDays
import com.d4rkwell.workouttracker.ui.components.ConfirmDialog
import com.d4rkwell.workouttracker.ui.components.EmptyState
import com.d4rkwell.workouttracker.ui.components.TextFieldDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekListScreen(
    viewModel: WeekListViewModel,
    onOpenWeek: (Long) -> Unit,
    onOpenHistory: () -> Unit
) {
    val weeks by viewModel.weeks.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var weekPendingDelete by remember { mutableStateOf<WeekWithDays?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Program") },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Filled.History, contentDescription = "Workout history")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add Week") }
            )
        }
    ) { padding ->
        if (weeks.isEmpty()) {
            EmptyState(
                title = "No weeks yet",
                subtitle = "Add your first training week to start building your program. You can add weeks one at a time as you go.",
                actionLabel = "Add Week 1",
                onAction = { showAddDialog = true },
                modifier = Modifier.padding(padding).fillMaxSize()
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(weeks, key = { it.week.weekId }) { weekWithDays ->
                    WeekRow(
                        weekWithDays = weekWithDays,
                        onClick = { onOpenWeek(weekWithDays.week.weekId) },
                        onDeleteRequest = { weekPendingDelete = weekWithDays }
                    )
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }

    if (showAddDialog) {
        val nextNumber = weeks.size + 1
        TextFieldDialog(
            title = "Add Week",
            label = "Week title",
            initialValue = "Week $nextNumber",
            confirmLabel = "Add",
            onDismiss = { showAddDialog = false },
            onConfirm = { title ->
                showAddDialog = false
                viewModel.addWeek(title) { newId -> onOpenWeek(newId) }
            }
        )
    }

    weekPendingDelete?.let { target ->
        ConfirmDialog(
            title = "Delete ${target.week.title}?",
            message = "This removes the week and all its days, exercises, and note content. Logged workout history for this week's exercises is kept for your records.",
            onDismiss = { weekPendingDelete = null },
            onConfirm = {
                viewModel.deleteWeek(target.week)
                weekPendingDelete = null
            }
        )
    }
}

@Composable
private fun WeekRow(
    weekWithDays: WeekWithDays,
    onClick: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(weekWithDays.week.title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                val dayCount = weekWithDays.days.size
                Text(
                    text = if (dayCount == 0) "No days added yet" else "$dayCount training day${if (dayCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (weekWithDays.week.note.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = weekWithDays.week.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Week options")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Delete week") },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onDeleteRequest()
                        }
                    )
                }
            }

            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
