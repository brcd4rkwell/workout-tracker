package com.d4rkwell.workouttracker.ui.screens.weekdetail

import androidx.compose.foundation.clickable
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
import com.d4rkwell.workouttracker.data.local.entity.DayEntity
import com.d4rkwell.workouttracker.ui.components.ConfirmDialog
import com.d4rkwell.workouttracker.ui.components.EmptyState
import com.d4rkwell.workouttracker.ui.components.TextFieldDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekDetailScreen(
    viewModel: WeekDetailViewModel,
    onBack: () -> Unit,
    onOpenDay: (Long) -> Unit
) {
    val weekWithDays by viewModel.weekWithDays.collectAsState()
    var showAddDayDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var dayPendingDelete by remember { mutableStateOf<DayEntity?>(null) }
    var dayPendingRename by remember { mutableStateOf<DayEntity?>(null) }

    val week = weekWithDays?.week
    val days = weekWithDays?.days.orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(week?.title ?: "Week") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showNoteDialog = true }) {
                        Icon(Icons.Filled.EditNote, contentDescription = "Week note")
                    }
                    IconButton(onClick = { showRenameDialog = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Rename week")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDayDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add Day") }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (week?.note?.isNotBlank() == true) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = week.note,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (days.isEmpty()) {
                EmptyState(
                    title = "No days yet",
                    subtitle = "Add a training day (e.g. \"Day 1 - Push\") and then fill in its exercises.",
                    actionLabel = "Add Day 1",
                    onAction = { showAddDayDialog = true },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(days, key = { it.dayId }) { day ->
                        DayRow(
                            day = day,
                            onClick = { onOpenDay(day.dayId) },
                            onDeleteRequest = { dayPendingDelete = day },
                            onRenameRequest = { dayPendingRename = day }
                        )
                    }
                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }

    if (showAddDayDialog) {
        val nextNumber = days.size + 1
        TextFieldDialog(
            title = "Add Day",
            label = "Day title",
            initialValue = "Day $nextNumber",
            confirmLabel = "Add",
            onDismiss = { showAddDayDialog = false },
            onConfirm = { title ->
                showAddDayDialog = false
                viewModel.addDay(title) { newId -> onOpenDay(newId) }
            }
        )
    }

    if (showRenameDialog && week != null) {
        TextFieldDialog(
            title = "Rename Week",
            label = "Week title",
            initialValue = week.title,
            confirmLabel = "Save",
            onDismiss = { showRenameDialog = false },
            onConfirm = { newTitle ->
                showRenameDialog = false
                viewModel.renameWeek(week, newTitle)
            }
        )
    }

    if (showNoteDialog && week != null) {
        TextFieldDialog(
            title = "Week Note",
            label = "Note (e.g. deload, focus cue)",
            initialValue = week.note,
            confirmLabel = "Save",
            onDismiss = { showNoteDialog = false },
            onConfirm = { note ->
                showNoteDialog = false
                viewModel.updateWeekNote(week, note)
            }
        )
    }

    dayPendingDelete?.let { target ->
        ConfirmDialog(
            title = "Delete ${target.title}?",
            message = "This removes the day and all exercises in it. Past logged workouts for this day are kept in your history.",
            onDismiss = { dayPendingDelete = null },
            onConfirm = {
                viewModel.deleteDay(target)
                dayPendingDelete = null
            }
        )
    }

    dayPendingRename?.let { target ->
        TextFieldDialog(
            title = "Rename Day",
            label = "Day title",
            initialValue = target.title,
            confirmLabel = "Save",
            onDismiss = { dayPendingRename = null },
            onConfirm = { newTitle ->
                viewModel.renameDay(target, newTitle)
                dayPendingRename = null
            }
        )
    }
}

@Composable
private fun DayRow(
    day: DayEntity,
    onClick: () -> Unit,
    onDeleteRequest: () -> Unit,
    onRenameRequest: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.FitnessCenter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(day.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Warmup: ${day.warmupMinutes} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Day options")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Rename day") },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onRenameRequest()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete day") },
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
