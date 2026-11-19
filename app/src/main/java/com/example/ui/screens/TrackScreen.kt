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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.ChallengeDayStatus
import com.example.data.ChallengeEntity
import com.example.data.DateUtils
import com.example.data.HabitAutoSource
import com.example.data.HabitEntity
import com.example.domain.ProductivityScoreEngine
import com.example.ui.LifeOsUiState
import com.example.ui.LifeOsViewModel
import com.example.ui.components.AppleSegmentedControl
import com.example.ui.components.AppleSubScreenHeader
import com.example.ui.components.LifeOsBadgeIcon
import com.example.ui.components.LifeOsBadgeType
import com.example.ui.components.LiquidGlassCard
import com.example.ui.theme.JetBrainsMonoFamily

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrackScreen(
    state: LifeOsUiState,
    viewModel: LifeOsViewModel,
    onBack: (() -> Unit)? = null
) {
    var subTab by remember { mutableIntStateOf(0) } // 0 = Habits, 1 = Challenges
    var showHabitDialog by remember { mutableStateOf(false) }
    var showChallengeDialog by remember { mutableStateOf(false) }
    var auditDialogText by remember { mutableStateOf<String?>(null) }

    val dateIso = state.selectedDateIso
    val last28Days = remember(dateIso) { DateUtils.lastNDaysIso(28, dateIso) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 116.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "track_header") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (onBack != null) {
                        AppleSubScreenHeader(
                            title = "Habits & Challenges",
                            subtitle = "${state.habits.size} habits • ${state.challenges.size} challenges",
                            onBack = onBack,
                            badgeType = LifeOsBadgeType.STREAK
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LifeOsBadgeIcon(type = LifeOsBadgeType.STREAK, size = 44.dp)
                            Text("Track Consistency", style = MaterialTheme.typography.displaySmall)
                        }
                    }
                    AppleSegmentedControl(
                        options = listOf("Habits (${state.habits.size})", "Challenges (${state.challenges.size})"),
                        selectedIndex = subTab,
                        onSelect = { subTab = it }
                    )
                }
            }

            if (subTab == 0) {
                // Dedicated Habit Analytics & Cross-Device Sync Banner
                item(key = "habit_analytics_banner") {
                    val todayProgressList = state.habits.map { h ->
                        ProductivityScoreEngine.resolveHabitDayProgress(
                            habit = h,
                            dateIso = dateIso,
                            habitLogs = state.habitLogs,
                            waterLogs = state.waterLogs,
                            stepLogs = state.stepLogs,
                            focusSessions = state.focusSessions
                        )
                    }
                    val doneTodayCount = todayProgressList.count { it.completed }
                    val autoCount = state.habits.count { HabitAutoSource.fromNameOrDefault(it.autoSource) != HabitAutoSource.NONE }
                    val bestStreak = state.habits.maxOfOrNull { h ->
                        ProductivityScoreEngine.calculateHabitLongestStreak(
                            habit = h,
                            logs = state.habitLogs,
                            todayIso = dateIso,
                            waterLogs = state.waterLogs,
                            stepLogs = state.stepLogs,
                            focusSessions = state.focusSessions
                        )
                    } ?: 0

                    LiquidGlassCard(
                        cornerRadius = 22.dp,
                        accentTint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CloudDone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Cloud Sync • Custom Units • Auto-Fill",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                                Text(
                                    text = "$doneTodayCount/${state.habits.size} today",
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HabitStatChip("Best Streak", "${bestStreak}d", Modifier.weight(1f))
                                HabitStatChip("Auto-Linked", "$autoCount", Modifier.weight(1f))
                                HabitStatChip("Total Habits", "${state.habits.size}", Modifier.weight(1f))
                            }
                        }
                    }
                }

                if (state.habits.isEmpty()) {
                    item(key = "empty_habits") {
                        LiquidGlassCard(
                            cornerRadius = 22.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("No habits tracked yet", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Tap + to build custom habits with units (minutes, glasses, reps, steps), Health/metric auto-fill, reminders, streak tracking, and 28-day heatmaps.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(state.habits, key = { it.id }) { habit ->
                        val todayProg = ProductivityScoreEngine.resolveHabitDayProgress(
                            habit = habit,
                            dateIso = dateIso,
                            habitLogs = state.habitLogs,
                            waterLogs = state.waterLogs,
                            stepLogs = state.stepLogs,
                            focusSessions = state.focusSessions
                        )
                        val streak = ProductivityScoreEngine.calculateHabitCurrentStreak(
                            habit = habit,
                            logs = state.habitLogs,
                            todayIso = dateIso,
                            waterLogs = state.waterLogs,
                            stepLogs = state.stepLogs,
                            focusSessions = state.focusSessions
                        )
                        val longest = ProductivityScoreEngine.calculateHabitLongestStreak(
                            habit = habit,
                            logs = state.habitLogs,
                            todayIso = dateIso,
                            waterLogs = state.waterLogs,
                            stepLogs = state.stepLogs,
                            focusSessions = state.focusSessions
                        )
                        val (rate30d, missed30d) = ProductivityScoreEngine.calculateHabitCompletionRate(
                            habit = habit,
                            logs = state.habitLogs,
                            endDateIso = dateIso,
                            lookbackDays = 30,
                            waterLogs = state.waterLogs,
                            stepLogs = state.stepLogs,
                            focusSessions = state.focusSessions
                        )

                        LiquidGlassCard(
                            cornerRadius = 22.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("habit_card_${habit.id}")
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable(enabled = !todayProg.isAutoFilled) {
                                                viewModel.toggleHabitForDate(habit.id, dateIso, todayProg.completed)
                                            }
                                    ) {
                                        Icon(
                                            imageVector = if (todayProg.completed) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                            contentDescription = "Toggle habit",
                                            tint = if (todayProg.completed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(26.dp)
                                        )
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(habit.title, style = MaterialTheme.typography.titleMedium)
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Filled.LocalFireDepartment,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = "Streak ${streak}d • Best ${longest}d • ${rate30d}% (${missed30d} missed)",
                                                    fontFamily = JetBrainsMonoFamily,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    IconButton(onClick = { viewModel.deleteHabit(habit.id) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete habit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                // Badges: Custom Unit Target / Auto-Fill Source / Reminder Time
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = todayProg.formattedProgress,
                                            fontFamily = JetBrainsMonoFamily,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                        )
                                    }
                                    if (todayProg.isAutoFilled) {
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = MaterialTheme.colorScheme.tertiaryContainer
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    Icons.Filled.AutoAwesome,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp),
                                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                                )
                                                Text(
                                                    text = todayProg.autoSource.label,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                                )
                                            }
                                        }
                                    }
                                    if (habit.reminderTime.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    Icons.Filled.NotificationsActive,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp),
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                                Text(
                                                    text = habit.reminderTime,
                                                    fontFamily = JetBrainsMonoFamily,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }
                                    }
                                }

                                // Progress bar & quick increment controls for measurable or auto-filled habits
                                if (todayProg.targetValue > 1 || todayProg.isAutoFilled) {
                                    LinearProgressIndicator(
                                        progress = { todayProg.ratio },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(7.dp)
                                            .clip(RoundedCornerShape(999.dp)),
                                        color = if (todayProg.completed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        strokeCap = StrokeCap.Round
                                    )
                                }

                                if (todayProg.isAutoFilled) {
                                    Text(
                                        text = "Automatically filled from your ${todayProg.autoSource.label.removePrefix("Auto: ")} logs — zero manual ticking required.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else if (todayProg.targetValue > 1) {
                                    val stepBig = when {
                                        todayProg.targetValue >= 1000 -> 500
                                        todayProg.targetValue >= 100 -> 25
                                        todayProg.targetValue >= 15 -> 5
                                        else -> 2
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilledTonalButton(
                                            onClick = { viewModel.incrementHabitProgress(habit, dateIso, 1) },
                                            modifier = Modifier.weight(1f)
                                        ) { Text("+1") }
                                        FilledTonalButton(
                                            onClick = { viewModel.incrementHabitProgress(habit, dateIso, stepBig) },
                                            modifier = Modifier.weight(1f)
                                        ) { Text("+$stepBig") }
                                        OutlinedButton(
                                            onClick = { viewModel.toggleHabitForDate(habit.id, dateIso, todayProg.completed) },
                                            modifier = Modifier.weight(1f)
                                        ) { Text(if (todayProg.completed) "Reset" else "Complete") }
                                    }
                                }

                                // 28-day intensity heatmap (2 rows x 14 days)
                                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    last28Days.chunked(14).forEach { weekRow ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            weekRow.forEach { d ->
                                                val dayProg = ProductivityScoreEngine.resolveHabitDayProgress(
                                                    habit = habit,
                                                    dateIso = d,
                                                    habitLogs = state.habitLogs,
                                                    waterLogs = state.waterLogs,
                                                    stepLogs = state.stepLogs,
                                                    focusSessions = state.focusSessions
                                                )
                                                val cellColor = when {
                                                    dayProg.completed -> MaterialTheme.colorScheme.tertiary
                                                    dayProg.ratio >= 0.5f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                                                    dayProg.ratio > 0f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
                                                    else -> MaterialTheme.colorScheme.surfaceContainerHighest
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .size(18.dp)
                                                        .clip(RoundedCornerShape(5.dp))
                                                        .background(cellColor)
                                                        .clickable(enabled = !todayProg.isAutoFilled) {
                                                            viewModel.toggleHabitForDate(habit.id, d, dayProg.completed)
                                                        }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Strict Challenge Tracker
                if (state.challenges.isEmpty()) {
                    item(key = "empty_challenges") {
                        LiquidGlassCard(
                            cornerRadius = 22.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("No active challenges", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Example: No Instagram from 2026-11-19 to 2027-01-01. Future dates cannot be auto-completed, failed days remain visible, and edits keep an audit trail.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(state.challenges, key = { it.id }) { challenge ->
                        val logs = state.challengeLogs.filter { it.challengeId == challenge.id }
                        val logByDate = logs.associateBy { it.dateIso }
                        val totalDays = DateUtils.daysBetweenInclusive(challenge.startDateIso, challenge.endDateIso)
                        val successDays = logs.count { it.status == ChallengeDayStatus.SUCCESS.name }
                        val failedDays = logs.count { it.status == ChallengeDayStatus.FAILED.name }
                        val pct = ((successDays.toFloat() / totalDays) * 100).toInt().coerceIn(0, 100)
                        val todayStatus = logByDate[dateIso]?.status ?: ChallengeDayStatus.NOT_RECORDED.name
                        val canMarkDate = !DateUtils.isFutureIso(dateIso) && dateIso >= challenge.startDateIso && dateIso <= challenge.endDateIso

                        LiquidGlassCard(
                            cornerRadius = 22.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(challenge.title, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = "${challenge.startDateIso} -> ${challenge.endDateIso} ($totalDays days)",
                                            fontFamily = JetBrainsMonoFamily,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row {
                                        IconButton(onClick = {
                                            val history = logs.filter { it.auditHistory.isNotBlank() }
                                                .joinToString("\n") { "${it.dateIso}: ${it.auditHistory}" }
                                                .ifBlank { "No audit edits recorded yet." }
                                            auditDialogText = history
                                        }) {
                                            Icon(Icons.Filled.History, contentDescription = "Audit history")
                                        }
                                        IconButton(onClick = { viewModel.deleteChallenge(challenge.id) }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Delete challenge")
                                        }
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                                        Text("Success: $successDays", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.errorContainer) {
                                        Text("Failed: $failedDays", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                        Text("$pct%", fontFamily = JetBrainsMonoFamily, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }

                                LinearProgressIndicator(
                                    progress = { pct / 100f },
                                    modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(999.dp)),
                                    color = MaterialTheme.colorScheme.tertiary,
                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    strokeCap = StrokeCap.Round
                                )

                                if (canMarkDate) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilledTonalButton(
                                            onClick = { viewModel.setChallengeDayStatus(challenge.id, dateIso, ChallengeDayStatus.SUCCESS) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(if (todayStatus == ChallengeDayStatus.SUCCESS.name) "Success ✓" else "Success")
                                        }
                                        OutlinedButton(
                                            onClick = { viewModel.setChallengeDayStatus(challenge.id, dateIso, ChallengeDayStatus.FAILED) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = if (todayStatus == ChallengeDayStatus.FAILED.name) "Failed ✕" else "Failed",
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "Selected date ($dateIso) is outside active range or in the future (locked).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { if (subTab == 0) showHabitDialog = true else showChallengeDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 104.dp)
                .testTag("add_track_fab")
        ) {
            Icon(Icons.Filled.Add, contentDescription = if (subTab == 0) "Add Habit" else "Add Challenge")
        }
    }

    if (showHabitDialog) {
        var title by remember { mutableStateOf("") }
        var weekdaysMask by remember { mutableIntStateOf(127) }
        var selectedUnit by remember { mutableStateOf("Check-in") }
        var targetText by remember { mutableStateOf("1") }
        var autoSource by remember { mutableStateOf(HabitAutoSource.NONE) }
        var reminderTime by remember { mutableStateOf("") }
        val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
        val unitPresets = listOf("Check-in", "Minutes", "Glasses", "ml", "Steps", "Reps", "Pages")

        AlertDialog(
            onDismissRequest = { showHabitDialog = false },
            title = { Text("New Custom Habit") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Habit Name (e.g. Cold Shower, Read 20 Pages)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Scheduled Days", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        dayLabels.forEachIndexed { i, l ->
                            val bit = 1 shl i
                            val active = (weekdaysMask and bit) != 0
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest)
                                    .clickable {
                                        val next = weekdaysMask xor bit
                                        if (next > 0) weekdaysMask = next
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(l, style = MaterialTheme.typography.labelSmall, color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Text("Auto-Fill Source (Health / Focus Sync)", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        HabitAutoSource.entries.forEach { src ->
                            FilterChip(
                                selected = autoSource == src,
                                onClick = {
                                    autoSource = src
                                    when (src) {
                                        HabitAutoSource.STEPS -> {
                                            selectedUnit = "Steps"
                                            targetText = state.preferences.stepsTarget.toString()
                                            if (title.isBlank()) title = "Daily Walk Goal"
                                        }
                                        HabitAutoSource.WATER -> {
                                            selectedUnit = "Glasses"
                                            targetText = (state.preferences.waterTargetMl / 250).coerceAtLeast(1).toString()
                                            if (title.isBlank()) title = "Daily Hydration"
                                        }
                                        HabitAutoSource.FOCUS_MINUTES -> {
                                            selectedUnit = "Minutes"
                                            targetText = state.preferences.focusTargetMinutes.toString()
                                            if (title.isBlank()) title = "Deep Focus Session"
                                        }
                                        HabitAutoSource.NONE -> Unit
                                    }
                                },
                                label = { Text(src.label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Text("Custom Unit & Daily Target", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        unitPresets.forEach { u ->
                            FilterChip(
                                selected = selectedUnit == u,
                                onClick = {
                                    selectedUnit = u
                                    if (u == "Check-in") targetText = "1"
                                    else if (targetText == "1") targetText = when (u) {
                                        "Minutes" -> "30"
                                        "Glasses" -> "8"
                                        "ml" -> "2500"
                                        "Steps" -> "8000"
                                        "Reps" -> "20"
                                        "Pages" -> "15"
                                        else -> "1"
                                    }
                                },
                                label = { Text(u, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = targetText,
                            onValueChange = { targetText = it.filter(Char::isDigit).take(6) },
                            label = { Text("Target ($selectedUnit)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = reminderTime,
                            onValueChange = { reminderTime = it.take(5) },
                            label = { Text("Remind (HH:mm)") },
                            placeholder = { Text("08:00") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            val targetVal = (targetText.toIntOrNull() ?: 1).coerceIn(1, 200000)
                            viewModel.saveHabit(
                                HabitEntity(
                                    title = title.trim(),
                                    scheduleType = if (weekdaysMask == 127) "DAILY" else "WEEKLY",
                                    weekdaysMask = weekdaysMask,
                                    startDateIso = DateUtils.todayIso(),
                                    reminderTime = reminderTime.trim(),
                                    targetValue = targetVal,
                                    unit = selectedUnit,
                                    autoSource = autoSource.name
                                )
                            )
                            showHabitDialog = false
                        }
                    },
                    enabled = title.isNotBlank()
                ) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showHabitDialog = false }) { Text("Cancel") } }
        )
    }

    if (showChallengeDialog) {
        var title by remember { mutableStateOf("") }
        var start by remember { mutableStateOf(DateUtils.todayIso()) }
        var end by remember { mutableStateOf(DateUtils.offsetDateIso(DateUtils.todayIso(), 42)) }
        AlertDialog(
            onDismissRequest = { showChallengeDialog = false },
            title = { Text("New Strict Challenge") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Challenge (e.g. No Instagram)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = start,
                        onValueChange = { start = it.take(10) },
                        label = { Text("Start Date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = end,
                        onValueChange = { end = it.take(10) },
                        label = { Text("End Date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank() && start.length == 10 && end.length == 10 && end >= start) {
                            viewModel.saveChallenge(
                                ChallengeEntity(title = title.trim(), startDateIso = start, endDateIso = end)
                            )
                            showChallengeDialog = false
                        }
                    },
                    enabled = title.isNotBlank() && start.length == 10 && end.length == 10 && end >= start
                ) { Text("Start") }
            },
            dismissButton = { TextButton(onClick = { showChallengeDialog = false }) { Text("Cancel") } }
        )
    }

    if (auditDialogText != null) {
        AlertDialog(
            onDismissRequest = { auditDialogText = null },
            title = { Text("Challenge Edit Audit History") },
            text = { Text(auditDialogText ?: "", style = MaterialTheme.typography.bodySmall) },
            confirmButton = { TextButton(onClick = { auditDialogText = null }) { Text("Close") } }
        )
    }
}

@Composable
private fun HabitStatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(vertical = 8.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
