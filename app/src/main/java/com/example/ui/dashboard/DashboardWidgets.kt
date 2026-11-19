package com.example.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ChallengeDayStatus
import com.example.data.DashboardWidgetType
import com.example.data.DateUtils
import com.example.data.GoalStatus
import com.example.data.ReminderEntity
import com.example.data.TaskEntity
import com.example.data.TaskStatus
import com.example.domain.MotivationQuotesProvider
import com.example.domain.ProductivityScoreEngine
import com.example.ui.LifeOsUiState
import com.example.ui.components.ProductivityScoreRing
import com.example.ui.theme.JetBrainsMonoFamily

fun widgetIcon(type: DashboardWidgetType): ImageVector = when (type) {
    DashboardWidgetType.DAILY_PRODUCTIVITY_SCORE -> Icons.Filled.Speed
    DashboardWidgetType.MOTIVATION_GOAL -> Icons.Filled.AutoAwesome
    DashboardWidgetType.TASKS_SUMMARY -> Icons.Filled.TaskAlt
    DashboardWidgetType.HABIT_PROGRESS -> Icons.Filled.LocalFireDepartment
    DashboardWidgetType.ACTIVE_CHALLENGES -> Icons.Filled.EmojiEvents
    DashboardWidgetType.WATER_INTAKE -> Icons.Filled.WaterDrop
    DashboardWidgetType.AUTOMATIC_STEPS -> Icons.AutoMirrored.Filled.DirectionsWalk
    DashboardWidgetType.FOCUS_TIME -> Icons.Filled.Timer
    DashboardWidgetType.UPCOMING_REMINDERS -> Icons.Filled.NotificationsActive
    DashboardWidgetType.GOAL_PROGRESS -> Icons.Filled.Flag
}

fun widgetBadgeType(type: DashboardWidgetType): com.example.ui.components.LifeOsBadgeType = when (type) {
    DashboardWidgetType.DAILY_PRODUCTIVITY_SCORE -> com.example.ui.components.LifeOsBadgeType.INSIGHTS
    DashboardWidgetType.MOTIVATION_GOAL -> com.example.ui.components.LifeOsBadgeType.MOTIVATION
    DashboardWidgetType.TASKS_SUMMARY -> com.example.ui.components.LifeOsBadgeType.TASKS
    DashboardWidgetType.HABIT_PROGRESS -> com.example.ui.components.LifeOsBadgeType.STREAK
    DashboardWidgetType.ACTIVE_CHALLENGES -> com.example.ui.components.LifeOsBadgeType.CHALLENGE
    DashboardWidgetType.WATER_INTAKE -> com.example.ui.components.LifeOsBadgeType.WATER
    DashboardWidgetType.AUTOMATIC_STEPS -> com.example.ui.components.LifeOsBadgeType.STEPS
    DashboardWidgetType.FOCUS_TIME -> com.example.ui.components.LifeOsBadgeType.FOCUS
    DashboardWidgetType.UPCOMING_REMINDERS -> com.example.ui.components.LifeOsBadgeType.NOTIFICATIONS
    DashboardWidgetType.GOAL_PROGRESS -> com.example.ui.components.LifeOsBadgeType.GOALS
}

@Composable
fun DashboardWidgetCardShell(
    widgetType: DashboardWidgetType,
    isEditMode: Boolean,
    isDragging: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    dragHandleModifier: Modifier = Modifier,
    headerActionLabel: String? = null,
    onHeaderAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val badge = widgetBadgeType(widgetType)
    com.example.ui.components.LiquidGlassCard(
        cornerRadius = 26.dp,
        accentTint = if (isDragging) MaterialTheme.colorScheme.primary else null,
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_widget_${widgetType.name.lowercase()}")
            .then(
                if (isEditMode || isDragging) {
                    Modifier.border(
                        width = if (isDragging) 2.dp else 1.2.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = if (isDragging) 0.9f else 0.5f),
                        shape = RoundedCornerShape(26.dp)
                    )
                } else {
                    Modifier
                }
            )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isEditMode) {
                        Box(
                            modifier = dragHandleModifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
                                .testTag("drag_handle_${widgetType.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DragIndicator,
                                contentDescription = "Drag to reorder ${widgetType.title}",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        com.example.ui.components.LifeOsBadgeIcon(type = badge, size = 36.dp)
                    }
                    Text(
                        text = widgetType.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isEditMode) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onMoveUp,
                            enabled = canMoveUp,
                            modifier = Modifier.testTag("move_up_${widgetType.name.lowercase()}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowUp,
                                contentDescription = "Move ${widgetType.title} up"
                            )
                        }
                        IconButton(
                            onClick = onMoveDown,
                            enabled = canMoveDown,
                            modifier = Modifier.testTag("move_down_${widgetType.name.lowercase()}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = "Move ${widgetType.title} down"
                            )
                        }
                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier.testTag("remove_widget_${widgetType.name.lowercase()}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Remove ${widgetType.title}",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                } else if (headerActionLabel != null && onHeaderAction != null) {
                    TextButton(onClick = onHeaderAction) {
                        Text(headerActionLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
fun DailyProductivityScoreWidgetContent(
    state: LifeOsUiState,
    onInspectFormula: () -> Unit
) {
    val today = state.todayScore
    val yesterday = state.yesterdayScore
    val delta = if (today.scorePercent != null && yesterday.scorePercent != null) {
        today.scorePercent - yesterday.scorePercent
    } else null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                    )
                )
            )
            .clickable(onClick = onInspectFormula)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ProductivityScoreRing(scorePercent = today.scorePercent, size = 104.dp)

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (delta != null) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (delta >= 0) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = if (delta >= 0) "+$delta% vs yesterday" else "$delta% vs yesterday",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (delta >= 0) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            } else {
                Text(
                    text = if (today.hasAnyTrackedData) "Transparent dynamic score" else "Track an item to compute score",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            val applicable = today.categories.filter { it.applicable }
            if (applicable.isEmpty()) {
                Text(
                    text = "Untracked categories are automatically excluded — zero fabricated metrics.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                applicable.take(3).forEach { cat ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${cat.name} (${cat.effectiveWeightPercent.toInt()}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = cat.summaryText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Text(
                text = "Tap for full formula breakdown ->",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun MotivationGoalWidgetContent(
    state: LifeOsUiState,
    onOpenMotivationGoal: () -> Unit
) {
    var quoteOffset by remember(state.selectedDateIso) { mutableIntStateOf(0) }
    val quote = remember(state.selectedDateIso, quoteOffset) {
        MotivationQuotesProvider.quoteForDate(state.selectedDateIso, quoteOffset)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)
                    )
                )
            )
            .clickable(onClick = onOpenMotivationGoal)
            .padding(14.dp)
            .testTag("motivation_goal_widget_body"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.FormatQuote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (quoteOffset == 0) "Daily Quote • ${DateUtils.formatShortDay(state.selectedDateIso)}" else "Quote #${quoteOffset + 1}",
                    fontFamily = JetBrainsMonoFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = quote.theme,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Text(
            text = "\"${quote.text}\"",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "— ${quote.author}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = quote.authorRole,
                    style = MaterialTheme.typography.labelSmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { quoteOffset += 1 },
                    modifier = Modifier.testTag("widget_next_motivation_quote")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Next motivation quote",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                FilledTonalButton(
                    onClick = onOpenMotivationGoal,
                    modifier = Modifier.testTag("widget_open_motivation_goal")
                ) {
                    Text("Daily Feed", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun TasksSummaryWidgetContent(
    state: LifeOsUiState,
    onToggleTask: (TaskEntity) -> Unit,
    onOpenPlan: () -> Unit
) {
    val dateIso = state.selectedDateIso
    val dayTasks = state.tasks.filter { it.dueDateIso == dateIso || it.completedDateIso == dateIso }
    val completedCount = dayTasks.count { it.status == TaskStatus.COMPLETED.name }
    val pendingCount = dayTasks.count { it.status != TaskStatus.COMPLETED.name }
    val overdueCount = state.tasks.count { it.status != TaskStatus.COMPLETED.name && it.dueDateIso < dateIso }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricPill("Completed", "$completedCount", MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
            MetricPill("Pending", "$pendingCount", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
            MetricPill("Overdue", "$overdueCount", MaterialTheme.colorScheme.error, Modifier.weight(1f))
        }

        if (dayTasks.isEmpty()) {
            EmptyWidgetPrompt(
                message = "No tasks scheduled for ${DateUtils.formatShortDay(dateIso)}.",
                actionLabel = "Add Task",
                onAction = onOpenPlan
            )
        } else {
            dayTasks.take(3).forEach { task ->
                val done = task.status == TaskStatus.COMPLETED.name
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f))
                        .clickable { onToggleTask(task) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (done) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = if (done) "Completed" else "Pending",
                        tint = if (done) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${task.priority} • ${task.category}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HabitProgressWidgetContent(
    state: LifeOsUiState,
    onToggleHabit: (String, Boolean) -> Unit,
    onOpenTrack: () -> Unit
) {
    val dateIso = state.selectedDateIso
    val dayBit = DateUtils.dayOfWeekBit(dateIso)
    val activeHabits = state.habits.filter {
        it.startDateIso <= dateIso && (it.endDateIso.isBlank() || dateIso <= it.endDateIso) && ((it.weekdaysMask and dayBit) != 0)
    }

    if (activeHabits.isEmpty()) {
        EmptyWidgetPrompt(
            message = "Build daily momentum with custom-unit or auto-filled habits.",
            actionLabel = "Create Habit",
            onAction = onOpenTrack
        )
        return
    }

    val progressByHabit = activeHabits.associateWith { habit ->
        ProductivityScoreEngine.resolveHabitDayProgress(
            habit = habit,
            dateIso = dateIso,
            habitLogs = state.habitLogs,
            waterLogs = state.waterLogs,
            stepLogs = state.stepLogs,
            focusSessions = state.focusSessions
        )
    }
    val completedCount = progressByHabit.values.count { it.completed }
    val progress = progressByHabit.values.sumOf { it.ratio.toDouble() }.toFloat() / activeHabits.size

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "${(progress * 100).toInt()}% completed",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.tertiary
            )
            Text(
                text = "$completedCount/${activeHabits.size} habits",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = MaterialTheme.colorScheme.tertiary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeCap = StrokeCap.Round
        )
        activeHabits.take(3).forEach { habit ->
            val prog = progressByHabit[habit] ?: return@forEach
            val streak = ProductivityScoreEngine.calculateHabitCurrentStreak(
                habit = habit,
                logs = state.habitLogs,
                todayIso = dateIso,
                waterLogs = state.waterLogs,
                stepLogs = state.stepLogs,
                focusSessions = state.focusSessions
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f))
                    .clickable(enabled = !prog.isAutoFilled) { onToggleHabit(habit.id, prog.completed) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (prog.completed) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (prog.completed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = habit.title,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (prog.isAutoFilled || prog.targetValue > 1) {
                            Text(
                                text = if (prog.isAutoFilled) "${prog.formattedProgress} • ${prog.autoSource.label}" else prog.formattedProgress,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
                Text(
                    text = "${streak}d streak",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ActiveChallengesWidgetContent(
    state: LifeOsUiState,
    onMarkChallenge: (String, ChallengeDayStatus) -> Unit,
    onOpenChallenges: () -> Unit
) {
    val dateIso = state.selectedDateIso
    val active = state.challenges.filter { dateIso >= it.startDateIso && dateIso <= it.endDateIso }
    val isFuture = DateUtils.isFutureIso(dateIso)

    if (active.isEmpty()) {
        EmptyWidgetPrompt(
            message = "Start a strict challenge (e.g. No Instagram till Jan 1).",
            actionLabel = "Start Challenge",
            onAction = onOpenChallenges
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        active.take(2).forEach { challenge ->
            val logs = state.challengeLogs.filter { it.challengeId == challenge.id }
            val todayLog = logs.firstOrNull { it.dateIso == dateIso }
            val status = todayLog?.status ?: ChallengeDayStatus.NOT_RECORDED.name
            val totalDays = DateUtils.daysBetweenInclusive(challenge.startDateIso, challenge.endDateIso)
            val successCount = logs.count { it.status == ChallengeDayStatus.SUCCESS.name }
            val failCount = logs.count { it.status == ChallengeDayStatus.FAILED.name }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = challenge.title,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$successCount pass • $failCount fail",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${challenge.startDateIso} -> ${challenge.endDateIso} ($totalDays days)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!isFuture) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = { onMarkChallenge(challenge.id, ChallengeDayStatus.SUCCESS) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (status == ChallengeDayStatus.SUCCESS.name) "Passed ✓" else "Pass")
                        }
                        OutlinedButton(
                            onClick = { onMarkChallenge(challenge.id, ChallengeDayStatus.FAILED) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (status == ChallengeDayStatus.FAILED.name) "Failed ✕" else "Failed",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WaterIntakeWidgetContent(
    state: LifeOsUiState,
    onQuickAddWater: (Int) -> Unit,
    onOpenWaterDetails: () -> Unit
) {
    val todayWaterMl = state.waterLogs.filter { it.dateIso == state.selectedDateIso }.sumOf { it.amountMl }
    val targetMl = state.preferences.waterTargetMl.coerceAtLeast(250)
    val progress = (todayWaterMl.toFloat() / targetMl).coerceIn(0f, 1f)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "$todayWaterMl",
                    fontFamily = JetBrainsMonoFamily,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "/ $targetMl ml",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeCap = StrokeCap.Round
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = { onQuickAddWater(250) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("water_quick_250")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("250 ml")
            }
            FilledTonalButton(
                onClick = { onQuickAddWater(500) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("water_quick_500")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("500 ml")
            }
            OutlinedButton(onClick = onOpenWaterDetails) {
                Text("Logs")
            }
        }
    }
}

@Composable
fun AutomaticStepsWidgetContent(
    state: LifeOsUiState,
    onOpenStepsSheet: () -> Unit
) {
    val stepState = state.stepTracking
    val scheme = MaterialTheme.colorScheme

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (stepState.isTrackingUnavailable) {
            Text(
                text = "Automatic step tracking is unavailable on this device (no built-in step sensor or authorized Health Connect data).",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onOpenStepsSheet,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.testTag("steps_manage_button")
            ) {
                Text("Check Sources")
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${String.format(java.util.Locale.getDefault(), "%,d", stepState.todaySteps)} / ${String.format(java.util.Locale.getDefault(), "%,d", stepState.dailyGoal)}",
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = scheme.tertiary
                    )
                    Text(
                        text = "${String.format(java.util.Locale.getDefault(), "%.1f%%", stepState.progressPercent)} • " +
                            if (stepState.goalReached) "Goal achieved!" else "${String.format(java.util.Locale.getDefault(), "%,d", stepState.remainingSteps)} remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                    Text(
                        text = "${stepState.activeSourceLabel} • ${stepState.walkingActivityLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.primary
                    )
                }
                FilledTonalButton(
                    onClick = onOpenStepsSheet,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("steps_manage_button")
                ) {
                    Text(if (stepState.sensorTrackingActive || stepState.healthConnectGranted) "Details" else "Enable")
                }
            }
            LinearProgressIndicator(
                progress = { stepState.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = scheme.tertiary,
                trackColor = scheme.surfaceContainerHighest.copy(alpha = 0.65f),
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusTimeWidgetContent(
    state: LifeOsUiState,
    onOpenFocusStudio: () -> Unit
) {
    val todaySessions = state.focusSessions.filter { it.dateIso == state.selectedDateIso }
    val totalMinutes = todaySessions.sumOf { it.actualSeconds } / 60
    val targetMin = state.preferences.focusTargetMinutes.coerceAtLeast(15)
    val progress = (totalMinutes.toFloat() / targetMin).coerceIn(0f, 1f)
    val categories = todaySessions.map { it.category }.distinct().ifEmpty { listOf("Java", "SQL", "DSA", "React") }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "${totalMinutes}m / ${targetMin}m",
                    fontFamily = JetBrainsMonoFamily,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${todaySessions.size} deep work session${if (todaySessions.size == 1) "" else "s"} logged",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onOpenFocusStudio,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.testTag("widget_start_focus_button")
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (state.timerState.running) "Resume" else "Focus")
            }
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeCap = StrokeCap.Round
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            categories.take(4).forEach { cat ->
                AssistChip(onClick = onOpenFocusStudio, label = { Text(cat, style = MaterialTheme.typography.labelSmall) })
            }
        }
    }
}

@Composable
fun UpcomingRemindersWidgetContent(
    state: LifeOsUiState,
    onSnooze: (ReminderEntity) -> Unit,
    onOpenReminders: () -> Unit
) {
    val upcoming = state.reminders.filter { it.enabled }.sortedBy { it.scheduledEpochMs }.take(3)
    if (upcoming.isEmpty()) {
        EmptyWidgetPrompt(
            message = "No active smart reminders scheduled.",
            actionLabel = "Add Reminder",
            onAction = onOpenReminders
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        upcoming.forEach { r ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(r.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = "${DateUtils.formatTime(r.scheduledEpochMs)} • ${r.moduleType}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(onClick = { onSnooze(r) }) {
                    Icon(Icons.Filled.Snooze, contentDescription = "Snooze 15 minutes", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun GoalProgressWidgetContent(
    state: LifeOsUiState,
    onOpenGoals: () -> Unit
) {
    val activeGoals = state.goals.filter { it.status == GoalStatus.ACTIVE.name }.take(3)
    if (activeGoals.isEmpty()) {
        EmptyWidgetPrompt(
            message = "Set 7, 30, 60, or 90-day milestones in Goal Center.",
            actionLabel = "Add Goal",
            onAction = onOpenGoals
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        activeGoals.forEach { goal ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f))
                    .clickable(onClick = onOpenGoals)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${goal.progressPercent}%",
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                LinearProgressIndicator(
                    progress = { goal.progressPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round
                )
                Text(
                    text = "${goal.horizon.replace("_", " ")} • Target ${goal.targetDateIso}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MetricPill(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.12f))
            .padding(vertical = 10.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge,
            color = accent
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyWidgetPrompt(
    message: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        FilledTonalButton(onClick = onAction) {
            Text(actionLabel, style = MaterialTheme.typography.labelMedium)
        }
    }
}
