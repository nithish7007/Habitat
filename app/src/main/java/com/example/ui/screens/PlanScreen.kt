package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DateUtils
import com.example.data.RecurrenceRule
import com.example.data.TaskEntity
import com.example.data.TaskPriority
import com.example.data.TaskStatus
import com.example.ui.LifeOsUiState
import com.example.ui.LifeOsViewModel
import com.example.ui.components.AppleSubScreenHeader
import com.example.ui.components.LifeOsBadgeIcon
import com.example.ui.components.LifeOsBadgeType
import com.example.ui.components.LiquidGlassCard
import com.example.ui.theme.JetBrainsMonoFamily
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlanScreen(
    state: LifeOsUiState,
    viewModel: LifeOsViewModel,
    snackbarHostState: SnackbarHostState,
    onBack: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    var subTab by remember { mutableIntStateOf(0) } // 0 = Tasks, 1 = Daily Time Blocks
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("ALL") } // ALL, PENDING, COMPLETED, OVERDUE
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var showTaskDialog by remember { mutableStateOf(false) }

    val todayIso = state.selectedDateIso
    val filteredTasks = remember(state.tasks, searchQuery, statusFilter, todayIso) {
        state.tasks.filter { t ->
            val matchQuery = searchQuery.isBlank() ||
                t.title.contains(searchQuery, ignoreCase = true) ||
                t.category.contains(searchQuery, ignoreCase = true) ||
                t.labelsCsv.contains(searchQuery, ignoreCase = true)
            val matchStatus = when (statusFilter) {
                "PENDING" -> t.status != TaskStatus.COMPLETED.name
                "COMPLETED" -> t.status == TaskStatus.COMPLETED.name
                "OVERDUE" -> t.status != TaskStatus.COMPLETED.name && t.dueDateIso < todayIso
                else -> true
            }
            matchQuery && matchStatus
        }.sortedWith(
            compareBy<TaskEntity> { it.status == TaskStatus.COMPLETED.name }
                .thenBy { when (it.priority) { "HIGH" -> 0; "MEDIUM" -> 1; else -> 2 } }
                .thenBy { it.dueDateIso }
        )
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 116.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "plan_header") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (onBack != null) {
                        AppleSubScreenHeader(
                            title = "Plan & Tasks",
                            subtitle = "${state.tasks.count { it.status != TaskStatus.COMPLETED.name }} open • Time blocks",
                            onBack = onBack,
                            badgeType = LifeOsBadgeType.TASKS
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LifeOsBadgeIcon(type = LifeOsBadgeType.TASKS, size = 44.dp)
                            Text("Plan & Tasks", style = MaterialTheme.typography.displaySmall)
                        }
                    }
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        listOf("Task Manager", "Daily Time Blocks").forEachIndexed { idx, label ->
                            SegmentedButton(
                                selected = subTab == idx,
                                onClick = { subTab = idx },
                                shape = SegmentedButtonDefaults.itemShape(idx, 2)
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
            }

            if (subTab == 0) {
                item(key = "task_filters") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search tasks, categories, labels...") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("ALL" to "All", "PENDING" to "Active", "OVERDUE" to "Overdue", "COMPLETED" to "Done").forEach { (key, label) ->
                                FilterChip(
                                    selected = statusFilter == key,
                                    onClick = { statusFilter = key },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }

                if (filteredTasks.isEmpty()) {
                    item(key = "empty_tasks") {
                        LiquidGlassCard(
                            cornerRadius = 22.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("No matching tasks", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Tap + to create a prioritized task with due date, time block, and recurrence.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(filteredTasks, key = { it.id }) { task ->
                        val done = task.status == TaskStatus.COMPLETED.name
                        val overdue = !done && task.dueDateIso < todayIso
                        LiquidGlassCard(
                            cornerRadius = 20.dp,
                            onClick = {
                                editingTask = task
                                showTaskDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                IconButton(onClick = {
                                    viewModel.toggleTaskCompleted(task)
                                    scope.launch {
                                        val res = snackbarHostState.showSnackbar(
                                            message = if (done) "Task moved back to pending" else "Task completed!",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (res == SnackbarResult.ActionPerformed) {
                                            viewModel.toggleTaskCompleted(task.copy(status = if (done) "PENDING" else "COMPLETED"))
                                        }
                                    }
                                }) {
                                    Icon(
                                        imageVector = if (done) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                        contentDescription = "Toggle task",
                                        tint = if (done) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                                    )
                                }

                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (task.description.isNotBlank()) {
                                        Text(
                                            text = task.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = when (task.priority) {
                                                TaskPriority.HIGH.name -> MaterialTheme.colorScheme.errorContainer
                                                TaskPriority.MEDIUM.name -> MaterialTheme.colorScheme.primaryContainer
                                                else -> MaterialTheme.colorScheme.surfaceContainerHigh
                                            }
                                        ) {
                                            Text(
                                                text = task.priority,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = "${task.category} • ${task.dueDateIso}",
                                            fontFamily = JetBrainsMonoFamily,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (task.timeBlockStart.isNotBlank()) {
                                            Text(
                                                text = "• ${task.timeBlockStart}",
                                                fontFamily = JetBrainsMonoFamily,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                }

                                IconButton(onClick = { viewModel.deleteTask(task.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete task", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            } else {
                // Smart Daily Planner Time Blocks
                val dayTasks = state.tasks.filter { it.dueDateIso == todayIso }
                    .sortedBy { it.timeBlockStart.ifBlank { "99:99" } }
                item(key = "planner_date") {
                    Text(
                        text = "Schedule for ${DateUtils.formatPrettyDate(todayIso)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (dayTasks.isEmpty()) {
                    item(key = "planner_empty") {
                        LiquidGlassCard(
                            cornerRadius = 22.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("No time blocks scheduled today", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "Add a task with a Time Block (e.g. 09:00 - 10:30) to build your structured study & work schedule.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(dayTasks, key = { "block_${it.id}" }) { t ->
                        LiquidGlassCard(
                            cornerRadius = 20.dp,
                            onClick = { viewModel.toggleTaskCompleted(t) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                                    Text(
                                        text = t.timeBlockStart.ifBlank { "Anytime" },
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(t.title, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "${t.category} • ${if (t.timeBlockEnd.isNotBlank()) "until ${t.timeBlockEnd}" else t.priority}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = if (t.status == TaskStatus.COMPLETED.name) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (t.status == TaskStatus.COMPLETED.name) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                editingTask = null
                showTaskDialog = true
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 104.dp)
                .testTag("add_task_fab")
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Task")
        }
    }

    if (showTaskDialog) {
        TaskEditorDialog(
            initial = editingTask,
            defaultDateIso = todayIso,
            onSave = { task ->
                viewModel.saveTask(task)
                showTaskDialog = false
            },
            onDismiss = { showTaskDialog = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskEditorDialog(
    initial: TaskEntity?,
    defaultDateIso: String,
    onSave: (TaskEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var desc by remember { mutableStateOf(initial?.description ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "Study") }
    var dueDate by remember { mutableStateOf(initial?.dueDateIso ?: defaultDateIso) }
    var timeStart by remember { mutableStateOf(initial?.timeBlockStart ?: "") }
    var timeEnd by remember { mutableStateOf(initial?.timeBlockEnd ?: "") }
    var priority by remember { mutableStateOf(initial?.priority ?: TaskPriority.MEDIUM.name) }
    var recurrence by remember { mutableStateOf(initial?.recurrence ?: RecurrenceRule.NONE.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New Task" else "Edit Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("task_title_input")
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description (optional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { dueDate = it.take(10) },
                        label = { Text("Due (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = timeStart,
                        onValueChange = { timeStart = it.take(10) },
                        label = { Text("Start (09:00)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = timeEnd,
                        onValueChange = { timeEnd = it.take(10) },
                        label = { Text("End (10:30)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TaskPriority.entries.forEach { p ->
                        FilterChip(
                            selected = priority == p.name,
                            onClick = { priority = p.name },
                            label = { Text(p.name) }
                        )
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RecurrenceRule.entries.forEach { r ->
                        FilterChip(
                            selected = recurrence == r.name,
                            onClick = { recurrence = r.name },
                            label = { Text(r.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            (initial ?: TaskEntity()).copy(
                                title = title.trim(),
                                description = desc.trim(),
                                category = category.trim().ifBlank { "Personal" },
                                dueDateIso = dueDate.ifBlank { defaultDateIso },
                                timeBlockStart = timeStart.trim(),
                                timeBlockEnd = timeEnd.trim(),
                                priority = priority,
                                recurrence = recurrence
                            )
                        )
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("save_task_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
