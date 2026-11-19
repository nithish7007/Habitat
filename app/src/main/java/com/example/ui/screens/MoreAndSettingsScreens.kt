package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.BackupPreview
import com.example.data.DateUtils
import com.example.data.ReminderEntity
import com.example.data.ThemeMode
import com.example.ui.LifeOsUiState
import com.example.ui.LifeOsViewModel
import com.example.ui.components.AppleGroupedRow
import com.example.ui.components.AppleSegmentedControl
import com.example.ui.components.AppleSubScreenHeader
import com.example.ui.components.LifeOsBadgeType
import com.example.ui.components.LiquidGlassCard
import com.example.ui.dashboard.WidgetCustomizerSheet
import com.example.ui.theme.JetBrainsMonoFamily
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun MoreHubScreen(
    state: LifeOsUiState,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val modules = listOf(
        Triple("notifications", "Smart Notification Center", Pair(LifeOsBadgeType.NOTIFICATIONS, "13 smart categories, quiet hours & previews")),
        Triple("ai_assistant", "LIFE OS AI Assistant", Pair(LifeOsBadgeType.AI, "Powered by Firebase AI Logic (Gemini)")),
        Triple("memories", "Private Memories", Pair(LifeOsBadgeType.MEMORIES, "${state.memories.size} personal photo memories")),
        Triple("notes", "Notes & Code Snippets", Pair(LifeOsBadgeType.NOTES, "${state.notes.size} organized notes across 8 folders")),
        Triple("steps", "Automatic Step Counter", Pair(LifeOsBadgeType.STEPS, "${state.stepSensorStatus.todaySensorSteps} live sensor steps today")),
        Triple("water", "Water & Hydration", Pair(LifeOsBadgeType.WATER, "Hourly reminders & daily target tracker")),
        Triple("nutrition", "Nutrition & Protein", Pair(LifeOsBadgeType.FOOD, "Daily meals, protein & calories")),
        Triple("timetable", "Class Timetable", Pair(LifeOsBadgeType.CALENDAR, "${state.classSchedules.size} scheduled classes")),
        Triple("jobs", "Job & Interview Tracker", Pair(LifeOsBadgeType.CAREER, "${state.jobApplications.size} tracked applications")),
        Triple("motivation_goal", "Motivation Goal", Pair(LifeOsBadgeType.MOTIVATION, "Daily changing English quotes & authors")),
        Triple("routines", "Routine Builder", Pair(LifeOsBadgeType.ROUTINES, "${state.routines.size} structured routines")),
        Triple("goals", "Goal Center", Pair(LifeOsBadgeType.GOALS, "${state.goals.size} active & tracked goals")),
        Triple("journal", "Life Journal", Pair(LifeOsBadgeType.JOURNAL, "${state.journalEntries.size} daily reflections")),
        Triple("reminders", "Custom Reminders", Pair(LifeOsBadgeType.NOTIFICATIONS, "${state.reminders.count { it.enabled }} active alerts")),
        Triple("achievements", "Achievements", Pair(LifeOsBadgeType.ACHIEVEMENTS, "${state.achievements.count { it.unlocked }}/${state.achievements.size} unlocked")),
        Triple("search", "Global Search", Pair(LifeOsBadgeType.INSIGHTS, "Search across all LIFE OS data")),
        Triple("backup", "Backup & Export", Pair(LifeOsBadgeType.ACHIEVEMENTS, "Cloud sync + JSON & CSV export/import")),
        Triple("settings", "Settings & Weights", Pair(LifeOsBadgeType.SETTINGS, "Theme, widgets, score weights & account"))
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 720.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 116.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                AppleSubScreenHeader(
                    title = "All Modules",
                    subtitle = "${modules.size} integrated LIFE OS modules",
                    onBack = onBack,
                    badgeType = LifeOsBadgeType.SETTINGS
                )
            }
            item {
                LiquidGlassCard(cornerRadius = 24.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        modules.forEachIndexed { idx, (route, title, meta) ->
                            AppleGroupedRow(
                                badgeType = meta.first,
                                title = title,
                                subtitle = meta.second,
                                showDivider = idx != modules.lastIndex,
                                onClick = { onNavigate(route) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RemindersScreen(
    state: LifeOsUiState,
    viewModel: LifeOsViewModel,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        scope.launch {
            snackbarHostState.showSnackbar(
                if (granted) "Notification permission granted" else "Notification permission denied — reminders saved locally"
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 720.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 116.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                AppleSubScreenHeader(
                    title = "Smart Reminders",
                    subtitle = "${state.reminders.count { it.enabled }} active WorkManager alerts",
                    onBack = onBack,
                    badgeType = LifeOsBadgeType.NOTIFICATIONS,
                    trailing = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            TextButton(onClick = { notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                                Text("Enable Alerts")
                            }
                        }
                    }
                )
            }
            item {
                Text(
                    text = "Quiet Hours: ${state.preferences.quietHoursStart}:00 – ${state.preferences.quietHoursEnd}:00 (IST) • Scheduled via WorkManager",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (state.reminders.isEmpty()) {
                item { Text("No reminders yet. Tap + to schedule a WorkManager reminder.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(state.reminders, key = { it.id }) { r ->
                    LiquidGlassCard(
                        cornerRadius = 20.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(r.title, style = MaterialTheme.typography.titleSmall)
                                Text("${DateUtils.formatTime(r.scheduledEpochMs)} • ${r.moduleType}", fontFamily = JetBrainsMonoFamily, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { viewModel.snoozeReminder(r, 15) }) {
                                    Icon(Icons.Filled.Snooze, contentDescription = "Snooze 15m")
                                }
                                Switch(checked = r.enabled, onCheckedChange = { viewModel.saveReminder(r.copy(enabled = it)) })
                                IconButton(onClick = { viewModel.deleteReminder(r.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete reminder")
                                }
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 104.dp)
        ) { Icon(Icons.Filled.Add, contentDescription = "Add Reminder") }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var moduleType by remember { mutableStateOf("TASK") }
        var minutesFromNow by remember { mutableStateOf("30") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New Smart Reminder") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Reminder Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = minutesFromNow, onValueChange = { minutesFromNow = it.filter(Char::isDigit).take(4) }, label = { Text("Remind in (minutes)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("TASK", "HABIT", "WATER", "FOCUS", "GOAL").forEach { m ->
                            FilterChip(selected = moduleType == m, onClick = { moduleType = m }, label = { Text(m) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mins = (minutesFromNow.toLongOrNull() ?: 30L).coerceAtLeast(1L)
                        if (title.isNotBlank()) {
                            viewModel.saveReminder(
                                ReminderEntity(
                                    title = title.trim(),
                                    moduleType = moduleType,
                                    scheduledEpochMs = System.currentTimeMillis() + mins * 60_000L,
                                    enabled = true
                                )
                            )
                            showAddDialog = false
                        }
                    },
                    enabled = title.isNotBlank()
                ) { Text("Schedule") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun AchievementsScreen(state: LifeOsUiState, onBack: () -> Unit) {
    val totalXp = state.achievements.filter { it.unlocked }.sumOf { it.xp }
    val level = (totalXp / 250) + 1

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 720.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 116.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                AppleSubScreenHeader(
                    title = "Achievements",
                    subtitle = "Level $level • $totalXp XP",
                    onBack = onBack,
                    badgeType = LifeOsBadgeType.ACHIEVEMENTS
                )
            }
            item {
                LiquidGlassCard(
                    cornerRadius = 24.dp,
                    accentTint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Level $level", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text("Genuine achievements calculated from verified data", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("$totalXp XP", fontFamily = JetBrainsMonoFamily, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            items(state.achievements, key = { it.id }) { a ->
                LiquidGlassCard(
                    cornerRadius = 20.dp,
                    accentTint = if (a.unlocked) MaterialTheme.colorScheme.tertiary else null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(a.title, style = MaterialTheme.typography.titleMedium)
                            Text(a.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(a.progressLabel, fontFamily = JetBrainsMonoFamily, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (a.unlocked) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Text(
                                text = if (a.unlocked) "UNLOCKED +${a.xp}" else "${a.xp} XP",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlobalSearchScreen(state: LifeOsUiState, onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val results = remember(query, state.tasks, state.habits, state.challenges, state.notes, state.goals, state.journalEntries) {
        if (query.isBlank()) emptyList()
        else buildList {
            state.tasks.filter { it.title.contains(query, true) || it.category.contains(query, true) }.forEach { add(Triple("Task", it.title, "${it.status} • ${it.dueDateIso}")) }
            state.habits.filter { it.title.contains(query, true) }.forEach { add(Triple("Habit", it.title, it.scheduleType)) }
            state.challenges.filter { it.title.contains(query, true) }.forEach { add(Triple("Challenge", it.title, "${it.startDateIso} -> ${it.endDateIso}")) }
            state.notes.filter { it.title.contains(query, true) || it.content.contains(query, true) }.forEach { add(Triple("Note", it.title.ifBlank { "Untitled" }, it.labelsCsv)) }
            state.goals.filter { it.title.contains(query, true) }.forEach { add(Triple("Goal", it.title, "${it.progressPercent}% • ${it.status}")) }
            state.journalEntries.filter { it.highlights.contains(query, true) || it.learned.contains(query, true) }.forEach { add(Triple("Journal", it.dateIso, it.highlights.take(60))) }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 720.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 116.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                AppleSubScreenHeader(
                    title = "Global Search",
                    subtitle = "Instant lookup across all modules",
                    onBack = onBack,
                    badgeType = LifeOsBadgeType.INSIGHTS
                )
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search tasks, habits, challenges, notes, goals, journal...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotBlank() && results.isEmpty()) {
                item { Text("No matches found for \"$query\".", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(results) { (kind, title, sub) ->
                LiquidGlassCard(
                    cornerRadius = 18.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(kind.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(title, style = MaterialTheme.typography.titleSmall)
                        if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun BackupExportScreen(
    viewModel: LifeOsViewModel,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingPreview by remember { mutableStateOf<BackupPreview?>(null) }

    val exportJsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch {
                val json = viewModel.generateJsonBackup()
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                }.onSuccess { snackbarHostState.showSnackbar("JSON backup exported successfully") }
            }
        }
    }
    val exportCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            scope.launch {
                val csv = viewModel.generateTasksCsv()
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray()) }
                }.onSuccess { snackbarHostState.showSnackbar("Tasks CSV exported successfully") }
            }
        }
    }
    val importJsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val raw = runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
                if (raw != null) {
                    viewModel.parseBackupPreview(raw)
                        .onSuccess { pendingPreview = it }
                        .onFailure { snackbarHostState.showSnackbar(it.message ?: "Invalid backup JSON") }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 720.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 116.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                AppleSubScreenHeader(
                    title = "Backup & Export",
                    subtitle = "Cloud Firestore + JSON/CSV backup",
                    onBack = onBack,
                    badgeType = LifeOsBadgeType.ACHIEVEMENTS
                )
            }
            item {
                LiquidGlassCard(cornerRadius = 22.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Automatic Cloud Firestore Sync", style = MaterialTheme.typography.titleMedium)
                        Text("All LIFE OS modules synchronize automatically under your Firebase Authentication UID with local Room offline persistence.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                LiquidGlassCard(cornerRadius = 22.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Local Export & Restore", style = MaterialTheme.typography.titleMedium)
                        Button(onClick = { exportJsonLauncher.launch("lifeos_backup_${DateUtils.todayIso()}.json") }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Backup, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Export Full JSON Backup")
                        }
                        FilledTonalButton(onClick = { exportCsvLauncher.launch("lifeos_tasks_${DateUtils.todayIso()}.csv") }, modifier = Modifier.fillMaxWidth()) {
                            Text("Export Tasks as CSV")
                        }
                        OutlinedButton(onClick = { importJsonLauncher.launch(arrayOf("application/json", "text/*")) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.UploadFile, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Import JSON Backup (With Preview)")
                        }
                    }
                }
            }
        }
    }

    pendingPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = { pendingPreview = null },
            title = { Text("Restore Backup Preview") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Validated LIFE OS JSON backup:", style = MaterialTheme.typography.titleSmall)
                    Text("• Tasks: ${preview.tasksCount}\n• Habits: ${preview.habitsCount}\n• Challenges: ${preview.challengesCount}\n• Notes: ${preview.notesCount}\n• Goals: ${preview.goalsCount}\n• Journal: ${preview.journalCount}", style = MaterialTheme.typography.bodySmall)
                    Text("Choose Merge to keep existing records or Replace to overwrite.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = {
                        scope.launch {
                            viewModel.restoreBackup(preview.rawJson, replaceExisting = false)
                            pendingPreview = null
                            snackbarHostState.showSnackbar("Backup merged safely")
                        }
                    }) { Text("Merge") }
                    Button(onClick = {
                        scope.launch {
                            viewModel.restoreBackup(preview.rawJson, replaceExisting = true)
                            pendingPreview = null
                            snackbarHostState.showSnackbar("Backup restored (replaced)")
                        }
                    }) { Text("Replace") }
                }
            },
            dismissButton = { TextButton(onClick = { pendingPreview = null }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: LifeOsUiState,
    viewModel: LifeOsViewModel,
    snackbarHostState: SnackbarHostState,
    onSignOut: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var showWidgetSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val prefs = state.preferences

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 720.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 116.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                AppleSubScreenHeader(
                    title = "Settings & Profile",
                    subtitle = "Appearance, score weights & targets",
                    onBack = onBack,
                    badgeType = LifeOsBadgeType.SETTINGS
                )
            }

            item {
                LiquidGlassCard(cornerRadius = 22.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(state.displayName, style = MaterialTheme.typography.titleLarge)
                        if (state.email.isNotBlank()) Text(state.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("UID: ${state.userId.take(12)}... • Timezone: Asia/Kolkata", fontFamily = JetBrainsMonoFamily, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            item {
                LiquidGlassCard(cornerRadius = 22.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Appearance & Dashboard", style = MaterialTheme.typography.titleMedium)
                        val modes = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)
                        AppleSegmentedControl(
                            options = listOf("System", "Light", "Dark"),
                            selectedIndex = modes.indexOf(prefs.themeMode).coerceAtLeast(0),
                            onSelect = { idx -> viewModel.updateThemeMode(modes[idx]) }
                        )
                        FilledTonalButton(
                            onClick = { showWidgetSheet = true },
                            modifier = Modifier.fillMaxWidth().testTag("settings_customize_widgets_button")
                        ) {
                            Icon(Icons.Filled.DashboardCustomize, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Customize Dashboard Widgets (${prefs.enabledWidgets.size}/10)")
                        }
                    }
                }
            }

            item {
                LiquidGlassCard(cornerRadius = 22.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Daily Productivity Score Weights", style = MaterialTheme.typography.titleMedium)
                        WeightSliderRow("Tasks", prefs.weightTasks) { v -> viewModel.updatePreferences { it.copy(weightTasks = v) } }
                        WeightSliderRow("Habits", prefs.weightHabits) { v -> viewModel.updatePreferences { it.copy(weightHabits = v) } }
                        WeightSliderRow("Challenges", prefs.weightChallenges) { v -> viewModel.updatePreferences { it.copy(weightChallenges = v) } }
                        WeightSliderRow("Water", prefs.weightWater) { v -> viewModel.updatePreferences { it.copy(weightWater = v) } }
                        WeightSliderRow("Steps", prefs.weightSteps) { v -> viewModel.updatePreferences { it.copy(weightSteps = v) } }
                    }
                }
            }

            item {
                LiquidGlassCard(cornerRadius = 22.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Daily Wellness & Focus Targets", style = MaterialTheme.typography.titleMedium)
                        TargetSliderRow("Water Target", "${prefs.waterTargetMl} ml", prefs.waterTargetMl.toFloat(), 1000f..5000f) { v ->
                            viewModel.updatePreferences { it.copy(waterTargetMl = (v / 250).roundToInt() * 250) }
                        }
                        TargetSliderRow("Steps Target", "${prefs.stepsTarget} steps", prefs.stepsTarget.toFloat(), 2000f..20000f) { v ->
                            viewModel.updatePreferences { it.copy(stepsTarget = (v / 500).roundToInt() * 500) }
                        }
                        TargetSliderRow("Focus Target", "${prefs.focusTargetMinutes} min", prefs.focusTargetMinutes.toFloat(), 15f..300f) { v ->
                            viewModel.updatePreferences { it.copy(focusTargetMinutes = (v / 15).roundToInt() * 15) }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier.fillMaxWidth().testTag("sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Sign Out")
                    }
                    Button(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.DeleteForever, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Delete All Account Data")
                    }
                }
            }
        }
    }

    if (showWidgetSheet) {
        WidgetCustomizerSheet(
            enabledWidgets = prefs.enabledWidgets,
            onAddWidget = { viewModel.addWidget(it) },
            onRemoveWidget = { viewModel.removeWidget(it) },
            onMoveWidget = { f, t -> viewModel.moveWidget(f, t) },
            onResetDefaults = { viewModel.resetWidgetsToDefault() },
            onDismiss = { showWidgetSheet = false }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete All Data?") },
            text = { Text("This permanently deletes all your tasks, habits, challenges, notes, journal entries, focus sessions, water logs, and steps from this device and Cloud Firestore.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        scope.launch {
                            viewModel.deleteAllUserData()
                            showDeleteConfirm = false
                            snackbarHostState.showSnackbar("All account data deleted")
                        }
                    }
                ) { Text("Delete Permanently") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun WeightSliderRow(label: String, value: Int, onChange: (Int) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text("$value%", fontFamily = JetBrainsMonoFamily, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value.toFloat(), onValueChange = { onChange(it.roundToInt()) }, valueRange = 0f..100f)
    }
}

@Composable
private fun TargetSliderRow(label: String, valueLabel: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(valueLabel, fontFamily = JetBrainsMonoFamily, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        }
        Slider(value = value.coerceIn(range.start, range.endInclusive), onValueChange = onChange, valueRange = range)
    }
}
