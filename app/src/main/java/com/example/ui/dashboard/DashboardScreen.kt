package com.example.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.data.DashboardWidgetType
import com.example.data.DateUtils
import com.example.ui.LifeOsUiState
import com.example.ui.LifeOsViewModel
import com.example.ui.components.ApplePillBadge
import com.example.ui.components.AppleSectionHeader
import com.example.ui.components.LifeOsBadgeIcon
import com.example.ui.components.LifeOsBadgeType
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.ReactiveMinimalMascot
import com.example.ui.components.StepProgressCard
import com.example.ui.components.UserAvatarCircle
import com.example.ui.components.WeeklyTrendBarChart
import com.example.ui.theme.JetBrainsMonoFamily
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun DashboardScreen(
    state: LifeOsUiState,
    viewModel: LifeOsViewModel,
    snackbarHostState: SnackbarHostState,
    onNavigateToPlan: () -> Unit,
    onNavigateToTrack: () -> Unit,
    onNavigateToFocus: () -> Unit,
    onNavigateToGoals: () -> Unit,
    onNavigateToMotivationGoal: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToMore: () -> Unit,
    onNavigateRoute: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme
    var isEditMode by remember { mutableStateOf(false) }
    var showCustomizerSheet by remember { mutableStateOf(false) }
    var showScoreFormulaDialog by remember { mutableStateOf(false) }
    var showWaterDialog by remember { mutableStateOf(false) }

    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    val visibleWidgetStates = state.visibleWidgetStates
    val enabledWidgets = remember(visibleWidgetStates) { visibleWidgetStates.map { it.type } }
    val availableCount = (DashboardWidgetType.entries.size - enabledWidgets.size).coerceAtLeast(0)
    val streak = state.streakReport
    val finance = state.financeSummary
    val todayWaterMl = state.waterLogs.filter { it.dateIso == state.selectedDateIso }.sumOf { it.amountMl }
    val stepState = state.stepTracking
    val completedTopics = state.learningTopics.count { it.completed }
    val upcomingInterview = state.jobApplications.firstOrNull {
        it.interviewDateIso.isNotBlank() && it.interviewDateIso >= DateUtils.todayIso()
    }
    val todayDow = com.example.domain.LifeOsAiAssistant.isoDayOfWeekMon1Sun7(state.selectedDateIso)
    val nextClass = state.classSchedules
        .filter { it.dayOfWeek == todayDow }
        .minByOrNull { it.startTime }
        ?: state.classSchedules.minByOrNull { it.dayOfWeek }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .testTag("dashboard_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 118.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Apple Classic Large-Title Top Greeting Header
            item(key = "apple_top_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        UserAvatarCircle(
                            avatarPath = state.profile.avatarLocalPath,
                            displayName = state.displayName,
                            size = 48.dp,
                            onClick = { onNavigateRoute("profile") }
                        )
                        Column {
                            Text(
                                text = DateUtils.formatPrettyDate(state.selectedDateIso).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant
                            )
                            Text(
                                text = "${DateUtils.currentGreeting()}, ${state.displayName.substringBefore(" ")}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = scheme.onBackground
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalIconButton(
                            onClick = { onNavigateRoute("notifications") },
                            modifier = Modifier.testTag("dashboard_notifications_button")
                        ) {
                            Icon(Icons.Filled.Notifications, contentDescription = "Smart Notification Center")
                        }
                        FilledTonalIconButton(
                            onClick = onNavigateToSearch,
                            modifier = Modifier.testTag("dashboard_search_button")
                        ) {
                            Icon(Icons.Filled.Search, contentDescription = "Global Search")
                        }
                        FilledTonalIconButton(
                            onClick = { showCustomizerSheet = true },
                            modifier = Modifier.testTag("customize_widgets_button")
                        ) {
                            Icon(Icons.Filled.DashboardCustomize, contentDescription = "Customize Dashboard Widgets")
                        }
                    }
                }
            }

            // Hero Liquid Glass Companion + Streak + Date Navigation Card
            item(key = "hero_header") {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentTint = scheme.primary
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            ReactiveMinimalMascot(
                                mood = streak.mascotMood,
                                celebrate = streak.qualifiedToday,
                                size = 66.dp
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ApplePillBadge(
                                        text = "${streak.currentStreak}d Streak",
                                        tint = scheme.primary
                                    )
                                    ApplePillBadge(
                                        text = "LVL ${streak.level} • ${streak.totalXp} XP",
                                        tint = scheme.tertiary
                                    )
                                }
                                Text(
                                    text = streak.mascotStatusLine,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = scheme.onSurface
                                )
                            }
                        }

                        // Historical Date Selector + Inline Reorder Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { viewModel.selectDate(DateUtils.offsetDateIso(state.selectedDateIso, -1)) },
                                    modifier = Modifier.testTag("prev_date_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous day")
                                }
                                ApplePillBadge(
                                    text = if (state.selectedDateIso == DateUtils.todayIso()) "Today" else state.selectedDateIso,
                                    tint = scheme.secondary
                                )
                                IconButton(
                                    onClick = { viewModel.selectDate(DateUtils.offsetDateIso(state.selectedDateIso, 1)) },
                                    enabled = state.selectedDateIso < DateUtils.todayIso(),
                                    modifier = Modifier.testTag("next_date_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next day")
                                }
                            }

                            FilledTonalButton(
                                onClick = { isEditMode = !isEditMode },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("toggle_edit_widgets_mode")
                            ) {
                                Icon(
                                    imageVector = if (isEditMode) Icons.Filled.Check else Icons.Filled.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (isEditMode) "Done" else "Reorder")
                            }
                        }
                    }
                }
            }

            // Quick Actions Apple Liquid Glass Dock Strip
            item(key = "quick_actions_strip") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val actions = listOf(
                        Triple("AI Assistant", LifeOsBadgeType.AI, "ai_assistant"),
                        Triple("Motivation", LifeOsBadgeType.MOTIVATION, "motivation_goal"),
                        Triple("Water", LifeOsBadgeType.WATER, "water"),
                        Triple("Steps", LifeOsBadgeType.STEPS, "steps"),
                        Triple("Memories", LifeOsBadgeType.MEMORIES, "memories"),
                        Triple("Notes", LifeOsBadgeType.NOTES, "notes"),
                        Triple("Money", LifeOsBadgeType.FINANCE, "money"),
                        Triple("Alerts", LifeOsBadgeType.NOTIFICATIONS, "notifications")
                    )
                    items(actions.size) { idx ->
                        val (label, badge, route) = actions[idx]
                        LiquidGlassCard(
                            cornerRadius = 22.dp,
                            onClick = {
                                if (route == "motivation_goal") onNavigateToMotivationGoal()
                                else onNavigateRoute(route)
                            },
                            modifier = Modifier.testTag("quick_action_$route")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                LifeOsBadgeIcon(type = badge, size = 38.dp)
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = scheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Automatic Hardware / Health Connect Step Progress Card
            item(key = "home_automatic_step_card") {
                StepProgressCard(
                    stepState = stepState,
                    onOpenDetails = { onNavigateRoute("steps") },
                    compact = true
                )
            }

            // Life At-A-Glance Apple Bento Summary (Finance, Hydration, Classes/Learning, Interviews)
            item(key = "life_bento_summary") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LiquidGlassCard(
                            cornerRadius = 22.dp,
                            onClick = { onNavigateRoute("money") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LifeOsBadgeIcon(type = LifeOsBadgeType.FINANCE, size = 30.dp)
                                    Text("Balance", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
                                }
                                Text(
                                    text = "₹${String.format(Locale.getDefault(), "%,.0f", finance.availableBalance)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Today: ₹${finance.todayExpense.toInt()} spent",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = scheme.onSurfaceVariant
                                )
                            }
                        }

                        LiquidGlassCard(
                            cornerRadius = 22.dp,
                            onClick = { onNavigateRoute("water") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LifeOsBadgeIcon(type = LifeOsBadgeType.WATER, size = 30.dp)
                                    Text("Hydration", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
                                }
                                Text(
                                    text = "$todayWaterMl ml",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                val remWater = (state.preferences.waterTargetMl - todayWaterMl).coerceAtLeast(0)
                                Text(
                                    text = if (remWater == 0) "Daily goal reached" else "$remWater ml remaining",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = scheme.primary
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LiquidGlassCard(
                            cornerRadius = 22.dp,
                            onClick = { onNavigateRoute(if (nextClass != null) "timetable" else "learn") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LifeOsBadgeIcon(type = LifeOsBadgeType.LEARNING, size = 30.dp)
                                    Text("Classes & Learn", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
                                }
                                Text(
                                    text = nextClass?.className ?: "$completedTopics / ${state.learningTopics.size}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = nextClass?.let { "${it.startTime} • ${completedTopics}/${state.learningTopics.size} skills" }
                                        ?: "${state.classSchedules.size} classes • ${completedTopics} done",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = scheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }

                        LiquidGlassCard(
                            cornerRadius = 22.dp,
                            onClick = { onNavigateRoute("jobs") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LifeOsBadgeIcon(type = LifeOsBadgeType.CAREER, size = 30.dp)
                                    Text("Interviews", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
                                }
                                Text(
                                    text = upcomingInterview?.company ?: "${state.jobApplications.size} Apps",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = upcomingInterview?.let { "${it.interviewDateIso} ${it.interviewTime}".trim() } ?: "Track interviews",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = scheme.secondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Edit Mode Helper Banner
            item(key = "edit_banner") {
                AnimatedVisibility(visible = isEditMode) {
                    LiquidGlassCard(
                        cornerRadius = 18.dp,
                        accentTint = scheme.primary
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Drag handles or tap arrows to reorder • Tap X to hide",
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { showCustomizerSheet = true }) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add ($availableCount)")
                            }
                        }
                    }
                }
            }

            // Sync Error Banner (if any)
            if (state.syncError != null) {
                item(key = "sync_error") {
                    LiquidGlassCard(
                        cornerRadius = 18.dp,
                        accentTint = scheme.error
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = state.syncError,
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.error,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.dismissSyncError() }) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }

            // Empty Dashboard State when all widgets removed
            if (enabledWidgets.isEmpty()) {
                item(key = "empty_dashboard_widgets") {
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ReactiveMinimalMascot(mood = streak.mascotMood, size = 84.dp)
                            Text("Your Dashboard Is Clean", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Choose from ${DashboardWidgetType.entries.size} live widgets — Productivity Score, Motivation Goal, Tasks, Habits, Challenges, Water, Steps, Focus, Reminders, and Goals.",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { showCustomizerSheet = true },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.testTag("empty_add_widgets_button")
                            ) {
                                Icon(Icons.Filled.DashboardCustomize, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Add Dashboard Widgets")
                            }
                        }
                    }
                }
            }

            // Dynamic Ordered Customizable Widgets Feed (Room WidgetStateEntity-backed)
            itemsIndexed(
                items = visibleWidgetStates,
                key = { _, widgetState -> widgetState.id }
            ) { index, widgetState ->
                val widget = widgetState.type
                val isDragging = draggingIndex == index
                val swapThresholdPx = 240f

                val dragHandleMod = Modifier.pointerInput(enabledWidgets.size, index, isEditMode) {
                    if (!isEditMode) return@pointerInput
                    detectDragGestures(
                        onDragStart = {
                            draggingIndex = index
                            dragOffsetY = 0f
                        },
                        onDragEnd = {
                            draggingIndex = -1
                            dragOffsetY = 0f
                        },
                        onDragCancel = {
                            draggingIndex = -1
                            dragOffsetY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffsetY += dragAmount.y
                            if (dragOffsetY > swapThresholdPx && draggingIndex in 0 until enabledWidgets.lastIndex) {
                                viewModel.moveWidget(draggingIndex, draggingIndex + 1)
                                draggingIndex += 1
                                dragOffsetY = 0f
                            } else if (dragOffsetY < -swapThresholdPx && draggingIndex > 0) {
                                viewModel.moveWidget(draggingIndex, draggingIndex - 1)
                                draggingIndex -= 1
                                dragOffsetY = 0f
                            }
                        }
                    )
                }

                val removeWithUndo: () -> Unit = {
                    viewModel.removeWidget(widget)
                    scope.launch {
                        val res = snackbarHostState.showSnackbar(
                            message = "${widget.title} removed from dashboard",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Short
                        )
                        if (res == SnackbarResult.ActionPerformed) {
                            viewModel.restoreWidgetAt(widget, index)
                        }
                    }
                }

                DashboardWidgetCardShell(
                    widgetType = widget,
                    isEditMode = isEditMode,
                    isDragging = isDragging,
                    canMoveUp = index > 0,
                    canMoveDown = index < enabledWidgets.lastIndex,
                    onMoveUp = { viewModel.moveWidget(index, index - 1) },
                    onMoveDown = { viewModel.moveWidget(index, index + 1) },
                    onRemove = removeWithUndo,
                    dragHandleModifier = dragHandleMod,
                    headerActionLabel = when (widget) {
                        DashboardWidgetType.DAILY_PRODUCTIVITY_SCORE -> "Formula"
                        DashboardWidgetType.MOTIVATION_GOAL -> "Explore"
                        DashboardWidgetType.TASKS_SUMMARY -> "Open Plan"
                        DashboardWidgetType.HABIT_PROGRESS -> "All Habits"
                        DashboardWidgetType.ACTIVE_CHALLENGES -> "Manage"
                        DashboardWidgetType.WATER_INTAKE -> "Tracker"
                        DashboardWidgetType.AUTOMATIC_STEPS -> "Dashboard"
                        DashboardWidgetType.FOCUS_TIME -> "Studio"
                        DashboardWidgetType.UPCOMING_REMINDERS -> "Alerts"
                        DashboardWidgetType.GOAL_PROGRESS -> "Goals"
                    },
                    onHeaderAction = {
                        when (widget) {
                            DashboardWidgetType.DAILY_PRODUCTIVITY_SCORE -> showScoreFormulaDialog = true
                            DashboardWidgetType.MOTIVATION_GOAL -> onNavigateToMotivationGoal()
                            DashboardWidgetType.TASKS_SUMMARY -> onNavigateToPlan()
                            DashboardWidgetType.HABIT_PROGRESS -> onNavigateToTrack()
                            DashboardWidgetType.ACTIVE_CHALLENGES -> onNavigateToTrack()
                            DashboardWidgetType.WATER_INTAKE -> onNavigateRoute("water")
                            DashboardWidgetType.AUTOMATIC_STEPS -> onNavigateRoute("steps")
                            DashboardWidgetType.FOCUS_TIME -> onNavigateToFocus()
                            DashboardWidgetType.UPCOMING_REMINDERS -> onNavigateToReminders()
                            DashboardWidgetType.GOAL_PROGRESS -> onNavigateToGoals()
                        }
                    },
                    modifier = Modifier
                        .zIndex(if (isDragging) 4f else 0f)
                        .graphicsLayer {
                            translationY = if (isDragging) dragOffsetY else 0f
                            scaleX = if (isDragging) 1.02f else 1f
                            scaleY = if (isDragging) 1.02f else 1f
                        }
                ) {
                    when (widget) {
                        DashboardWidgetType.DAILY_PRODUCTIVITY_SCORE -> {
                            DailyProductivityScoreWidgetContent(
                                state = state,
                                onInspectFormula = { showScoreFormulaDialog = true }
                            )
                        }
                        DashboardWidgetType.MOTIVATION_GOAL -> {
                            MotivationGoalWidgetContent(
                                state = state,
                                onOpenMotivationGoal = onNavigateToMotivationGoal
                            )
                        }
                        DashboardWidgetType.TASKS_SUMMARY -> {
                            TasksSummaryWidgetContent(
                                state = state,
                                onToggleTask = { task ->
                                    viewModel.toggleTaskCompleted(task)
                                    scope.launch {
                                        val res = snackbarHostState.showSnackbar(
                                            message = if (task.status == "COMPLETED") "Marked pending" else "Task completed!",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (res == SnackbarResult.ActionPerformed) {
                                            viewModel.toggleTaskCompleted(task.copy(status = if (task.status == "COMPLETED") "PENDING" else "COMPLETED"))
                                        }
                                    }
                                },
                                onOpenPlan = onNavigateToPlan
                            )
                        }
                        DashboardWidgetType.HABIT_PROGRESS -> {
                            HabitProgressWidgetContent(
                                state = state,
                                onToggleHabit = { id, done -> viewModel.toggleHabitForDate(id, state.selectedDateIso, done) },
                                onOpenTrack = onNavigateToTrack
                            )
                        }
                        DashboardWidgetType.ACTIVE_CHALLENGES -> {
                            ActiveChallengesWidgetContent(
                                state = state,
                                onMarkChallenge = { id, status -> viewModel.setChallengeDayStatus(id, state.selectedDateIso, status) },
                                onOpenChallenges = onNavigateToTrack
                            )
                        }
                        DashboardWidgetType.WATER_INTAKE -> {
                            WaterIntakeWidgetContent(
                                state = state,
                                onQuickAddWater = { ml -> viewModel.addWater(ml, state.selectedDateIso) },
                                onOpenWaterDetails = { showWaterDialog = true }
                            )
                        }
                        DashboardWidgetType.AUTOMATIC_STEPS -> {
                            AutomaticStepsWidgetContent(
                                state = state,
                                onOpenStepsSheet = { onNavigateRoute("steps") }
                            )
                        }
                        DashboardWidgetType.FOCUS_TIME -> {
                            FocusTimeWidgetContent(
                                state = state,
                                onOpenFocusStudio = onNavigateToFocus
                            )
                        }
                        DashboardWidgetType.UPCOMING_REMINDERS -> {
                            UpcomingRemindersWidgetContent(
                                state = state,
                                onSnooze = { r -> viewModel.snoozeReminder(r, 15) },
                                onOpenReminders = onNavigateToReminders
                            )
                        }
                        DashboardWidgetType.GOAL_PROGRESS -> {
                            GoalProgressWidgetContent(
                                state = state,
                                onOpenGoals = onNavigateToGoals
                            )
                        }
                    }
                }
            }

            // Weekly Productivity Trend Chart
            item(key = "weekly_chart_card") {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AppleSectionHeader(
                            title = "7-Day Productivity Trend",
                            subtitle = "Computed exclusively from your logged activity"
                        )
                        WeeklyTrendBarChart(
                            labels = state.weeklyScores.map { it.first },
                            values = state.weeklyScores.map { it.second },
                            maxValue = 100
                        )
                    }
                }
            }

            item(key = "customize_footer") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showCustomizerSheet = true },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("footer_customize_widgets_button")
                    ) {
                        Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Widgets (${enabledWidgets.size}/${DashboardWidgetType.entries.size})")
                    }
                    FilledTonalButton(
                        onClick = onNavigateToMore,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.testTag("dashboard_more_hub_button")
                    ) {
                        Text("All Tools")
                    }
                }
            }
        }
    }

    if (showCustomizerSheet) {
        WidgetCustomizerSheet(
            enabledWidgets = enabledWidgets,
            onAddWidget = { viewModel.addWidget(it) },
            onRemoveWidget = { viewModel.removeWidget(it) },
            onMoveWidget = { from, to -> viewModel.moveWidget(from, to) },
            onResetDefaults = { viewModel.resetWidgetsToDefault() },
            onDismiss = { showCustomizerSheet = false }
        )
    }

    if (showScoreFormulaDialog) {
        AlertDialog(
            onDismissRequest = { showScoreFormulaDialog = false },
            title = { Text("Productivity Score Calculation") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Date: ${state.selectedDateIso} • Final Score: ${state.todayScore.scorePercent?.let { "$it%" } ?: "N/A"}",
                        style = MaterialTheme.typography.titleSmall,
                        color = scheme.primary
                    )
                    Text(
                        text = "Categories without data are excluded and remaining weights normalize to 100%.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                    HorizontalDivider()
                    state.todayScore.categories.forEach { c ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(c.name, style = MaterialTheme.typography.titleSmall)
                                Text(c.summaryText, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                            }
                            Text(
                                text = if (c.applicable) "${"%.1f".format(c.contributedPoints)} / ${c.effectiveWeightPercent.toInt()}%" else "Excluded",
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (c.applicable) scheme.tertiary else scheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showScoreFormulaDialog = false }) { Text("Close") }
            }
        )
    }

    if (showWaterDialog) {
        var customMl by remember { mutableStateOf("350") }
        val dayLogs = state.waterLogs.filter { it.dateIso == state.selectedDateIso }
        AlertDialog(
            onDismissRequest = { showWaterDialog = false },
            title = { Text("Water Intake • ${state.selectedDateIso}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Total: ${dayLogs.sumOf { it.amountMl }} ml / ${state.preferences.waterTargetMl} ml",
                        style = MaterialTheme.typography.titleSmall,
                        color = scheme.secondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = customMl,
                            onValueChange = { customMl = it.filter(Char::isDigit).take(4) },
                            label = { Text("Custom ml") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = {
                            val ml = customMl.toIntOrNull() ?: 250
                            if (ml in 10..5000) viewModel.addWater(ml, state.selectedDateIso)
                        }) {
                            Text("Add")
                        }
                    }
                    if (dayLogs.isNotEmpty()) {
                        HorizontalDivider()
                        dayLogs.take(5).forEach { log ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${log.amountMl} ml at ${DateUtils.formatTime(log.createdAtMs)}", style = MaterialTheme.typography.bodySmall)
                                IconButton(onClick = { viewModel.deleteWaterLog(log.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete water log", tint = scheme.error)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showWaterDialog = false }) { Text("Done") } }
        )
    }
}

