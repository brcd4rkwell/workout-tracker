package com.d4rkwell.workouttracker.ui.screens.weightprogress

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.d4rkwell.workouttracker.data.local.entity.WeightEntryEntity
import com.d4rkwell.workouttracker.ui.components.ConfirmDialog
import com.d4rkwell.workouttracker.ui.components.EmptyState
import com.d4rkwell.workouttracker.util.formatDate
import com.d4rkwell.workouttracker.util.formatWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightProgressScreen(
    viewModel: WeightProgressViewModel,
    onBack: () -> Unit
) {
    val entries by viewModel.entries.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var entryPendingDelete by remember { mutableStateOf<WeightEntryEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weight Progress") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add Weight") }
            )
        }
    ) { padding ->
        if (entries.isEmpty()) {
            EmptyState(
                title = "No weight logged yet",
                subtitle = "Log your starting weight, then check back in every month or every 4 weeks to track your progress over the program.",
                actionLabel = "Log Starting Weight",
                onAction = { showAddDialog = true },
                modifier = Modifier.padding(padding).fillMaxSize()
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { SummaryCard(entries) }

                item {
                    Text(
                        "History",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(entries.sortedByDescending { it.loggedAtEpochMillis }, key = { it.weightEntryId }) { entry ->
                    WeightEntryRow(entry = entry, onDeleteRequest = { entryPendingDelete = entry })
                }

                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }

    if (showAddDialog) {
        AddWeightDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { weight, note ->
                showAddDialog = false
                viewModel.addEntry(weight, note)
            }
        )
    }

    entryPendingDelete?.let { target ->
        ConfirmDialog(
            title = "Delete this entry?",
            message = "Remove the ${formatWeight(target.weight)} entry from ${formatDate(target.loggedAtEpochMillis)}.",
            onDismiss = { entryPendingDelete = null },
            onConfirm = {
                viewModel.deleteEntry(target)
                entryPendingDelete = null
            }
        )
    }
}

@Composable
private fun SummaryCard(entries: List<WeightEntryEntity>) {
    val sorted = entries.sortedBy { it.loggedAtEpochMillis }
    val first = sorted.first()
    val latest = sorted.last()
    val totalChange = latest.weight - first.weight

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Starting", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatWeight(first.weight), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Current", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatWeight(latest.weight), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
            if (sorted.size > 1) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (totalChange <= 0) Icons.Filled.TrendingDown else Icons.Filled.TrendingUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    val sign = if (totalChange > 0) "+" else ""
                    Text(
                        "$sign${formatWeight(totalChange)} since you started",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun WeightEntryRow(entry: WeightEntryEntity, onDeleteRequest: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(formatWeight(entry.weight), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    formatDate(entry.loggedAtEpochMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (entry.note.isNotBlank()) {
                    Text(
                        entry.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDeleteRequest) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete entry")
            }
        }
    }
}

@Composable
private fun AddWeightDialog(
    onDismiss: () -> Unit,
    onConfirm: (weight: Double, note: String) -> Unit
) {
    var weightText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Weight") },
        text = {
            Column {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Weight") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { weightText.toDoubleOrNull()?.let { onConfirm(it, noteText.trim()) } },
                enabled = weightText.toDoubleOrNull() != null
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
