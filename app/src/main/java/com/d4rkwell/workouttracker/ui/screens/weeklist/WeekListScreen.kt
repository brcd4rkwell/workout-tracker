package com.d4rkwell.workouttracker.ui.screens.weeklist

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.d4rkwell.workouttracker.backup.BackupManager
import com.d4rkwell.workouttracker.data.local.entity.WeekWithDays
import com.d4rkwell.workouttracker.ui.components.ConfirmDialog
import com.d4rkwell.workouttracker.ui.components.EmptyState
import com.d4rkwell.workouttracker.ui.components.TextFieldDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekListScreen(
    viewModel: WeekListViewModel,
    onOpenWeek: (Long) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenWeightProgress: () -> Unit
) {
    val weeks by viewModel.weeks.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var weekPendingDelete by remember { mutableStateOf<WeekWithDays?>(null) }
    var weekPendingRename by remember { mutableStateOf<WeekWithDays?>(null) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showImportModeDialog by remember { mutableStateOf<android.net.Uri?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    BackupManager.exportToUri(context, uri)
                    Toast.makeText(context, "Backup saved", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            showImportModeDialog = uri
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Program") },
                actions = {
                    IconButton(onClick = onOpenWeightProgress) {
                        Icon(Icons.Filled.MonitorWeight, contentDescription = "Weight progress")
                    }
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Filled.History, contentDescription = "Workout history")
                    }
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(expanded = showOverflowMenu, onDismissRequest = { showOverflowMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Backup to file") },
                                leadingIcon = { Icon(Icons.Filled.CloudUpload, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    exportLauncher.launch(BackupManager.suggestedFileName())
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Restore / Import from file") },
                                leadingIcon = { Icon(Icons.Filled.CloudDownload, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    importLauncher.launch(arrayOf("application/json"))
                                }
                            )
                        }
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
                itemsIndexed(weeks, key = { _, item -> item.week.weekId }) { index, weekWithDays ->
                    WeekRow(
                        weekWithDays = weekWithDays,
                        onClick = { onOpenWeek(weekWithDays.week.weekId) },
                        onDeleteRequest = { weekPendingDelete = weekWithDays },
                        onRenameRequest = { weekPendingRename = weekWithDays },
                        onDuplicate = { viewModel.duplicateWeek(weekWithDays.week.weekId) },
                        onMoveUp = { viewModel.moveWeekUp(weekWithDays.week.weekId) },
                        onMoveDown = { viewModel.moveWeekDown(weekWithDays.week.weekId) },
                        canMoveUp = index > 0,
                        canMoveDown = index < weeks.size - 1
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

    weekPendingRename?.let { target ->
        TextFieldDialog(
            title = "Rename Week",
            label = "Week title",
            initialValue = target.week.title,
            confirmLabel = "Save",
            onDismiss = { weekPendingRename = null },
            onConfirm = { newTitle ->
                viewModel.renameWeek(target.week, newTitle)
                weekPendingRename = null
            }
        )
    }

    showImportModeDialog?.let { uri ->
        AlertDialog(
            onDismissRequest = { showImportModeDialog = null },
            title = { Text("Import File") },
            text = {
                Text(
                    "Replace everything with this file's contents (a full restore), " +
                        "or add its weeks alongside what you already have?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showImportModeDialog = null
                    coroutineScope.launch {
                        try {
                            val count = BackupManager.importFromUri(context, uri, merge = false)
                            Toast.makeText(context, "Restored $count items", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Restore failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }) { Text("Replace Everything") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportModeDialog = null
                    coroutineScope.launch {
                        try {
                            val count = BackupManager.importFromUri(context, uri, merge = true)
                            Toast.makeText(context, "Imported $count items", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }) { Text("Add Alongside") }
            }
        )
    }
}

@Composable
private fun WeekRow(
    weekWithDays: WeekWithDays,
    onClick: () -> Unit,
    onDeleteRequest: () -> Unit,
    onRenameRequest: () -> Unit,
    onDuplicate: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean
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

            Column {
                IconButton(
                    onClick = onMoveUp,
                    enabled = canMoveUp,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.KeyboardArrowUp,
                        contentDescription = "Move week up",
                        tint = if (canMoveUp) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = canMoveDown,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Move week down",
                        tint = if (canMoveDown) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Week options")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Rename week") },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onRenameRequest()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Duplicate week") },
                        leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onDuplicate()
                        }
                    )
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
