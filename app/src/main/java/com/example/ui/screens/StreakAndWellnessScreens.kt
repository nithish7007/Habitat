package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.DateUtils
import com.example.data.MealType
import com.example.data.NutritionEntryEntity
import com.example.domain.NutritionEngine
import com.example.ui.LifeOsUiState
import com.example.ui.LifeOsViewModel
import com.example.ui.components.AnimatedWaterBottle
import com.example.ui.components.ApplePillBadge
import com.example.ui.components.AppleSegmentedControl
import com.example.ui.components.AppleSubScreenHeader
import com.example.ui.components.LifeOSGlassCard
import com.example.ui.components.LifeOsBadgeType
import com.example.ui.components.ReactiveMinimalMascot
import com.example.ui.theme.JetBrainsMonoFamily

// ---------------- DUOLINGO-STYLE STREAK SCREEN ----------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StreakDashboardScreen(
    state: LifeOsUiState,
    onBack: () -> Unit
) {
    val streak = state.streakReport

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .testTag("streak_dashboard_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 118.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                AppleSubScreenHeader(
                    title = "Streak & Momentum",
                    subtitle = "${streak.currentStreak}-day active streak • Level ${streak.level}",
                    onBack = onBack,
                    badgeType = LifeOsBadgeType.STREAK
                )
            }

            item {
                LifeOSGlassCard(accentTint = MaterialTheme.colorScheme.primary) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ReactiveMinimalMascot(
                            mood = streak.mascotMood,
                            celebrate = streak.qualifiedToday,
                            size = 104.dp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Filled.LocalFireDepartment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(30.dp)
                            )
                            Text(
                                text = "${streak.currentStreak} Day Streak",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = streak.mascotStatusLine,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StreakStatChip("Longest", "${streak.longestStreak}d")
                            StreakStatChip("Level", "Lv ${streak.level}")
                            StreakStatChip("Total XP", "${streak.totalXp}")
                            StreakStatChip("Freezes", "${state.profile.streakFreezesAvailable}")
                        }
                        LinearProgressIndicator(
                            progress = { (streak.xpInCurrentLevel.toFloat() / streak.xpForNextLevel).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(999.dp))
                        )
                        Text(
                            text = "${streak.xpInCurrentLevel} / ${streak.xpForNextLevel} XP to Level ${streak.level + 1}",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 7-Day Weekly Strip
            item {
                LifeOSGlassCard {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Last 7 Days", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            streak.weeklyStrip.forEach { (dayLabel, active) ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (active) MaterialTheme.colorScheme.tertiary
                                                else MaterialTheme.colorScheme.surfaceContainerHighest
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (active) {
                                            Icon(
                                                Icons.Filled.Check,
                                                contentDescription = "Active",
                                                tint = MaterialTheme.colorScheme.onTertiary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Text(dayLabel, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            // 28-Day Monthly Heatmap
            item {
                LifeOSGlassCard {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("28-Day Consistency Heatmap", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        val rows = streak.monthlyHeatmap.chunked(7)
                        rows.forEach { week ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                week.forEach { (_, intensity) ->
                                    val color = when (intensity) {
                                        0 -> MaterialTheme.colorScheme.surfaceContainerHighest
                                        1 -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)
                                        2 -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.68f)
                                        else -> MaterialTheme.colorScheme.tertiary
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(color)
                                    )
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.AcUnit, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Qualifying actions: completing any task, habit, challenge, focus session, water log, or meal.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Badges & Milestones
            item {
                LifeOSGlassCard {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Unlocked Streak Milestones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        if (streak.activeMilestones.isEmpty()) {
                            Text(
                                text = "Complete your first qualifying activity today to unlock Day 1 Ignition!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                streak.activeMilestones.forEach { badge ->
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = badge,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreakStatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---------------- UPGRADED WATER TRACKER SCREEN ----------------

@Composable
fun WaterTrackerScreen(
    state: LifeOsUiState,
    viewModel: LifeOsViewModel,
    onBack: () -> Unit
) {
    val today = state.selectedDateIso
    val dayLogs = state.waterLogs.filter { it.dateIso == today }.sortedByDescending { it.createdAtMs }
    val totalMl = dayLogs.sumOf { it.amountMl }
    val targetMl = state.preferences.waterTargetMl.coerceAtLeast(250)
    val progress = (totalMl.toFloat() / targetMl).coerceIn(0f, 1.5f)
    var customMlText by remember { mutableStateOf("350") }
    var targetText by remember(targetMl) { mutableStateOf(targetMl.toString()) }
    var showTargetDialog by remember { mutableStateOf(false) }

    val remainingMl = (targetMl - totalMl).coerceAtLeast(0)
    val last7Water = remember(state.waterLogs, today) {
        val byDate = state.waterLogs.groupBy { it.dateIso }.mapValues { (_, list) -> list.sumOf { it.amountMl } }
        DateUtils.lastNDaysIso(7, today).map { iso -> iso to (byDate[iso] ?: 0) }
    }
    val intervalOptions = listOf(30, 60, 90, 120)
    val selectedIntervalIdx = intervalOptions.indexOf(state.profile.waterReminderIntervalMin).let { if (it >= 0) it else 1 }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .testTag("water_tracker_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 118.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                AppleSubScreenHeader(
                    title = "Water & Hydration",
                    subtitle = "$totalMl / $targetMl ml • ${if (remainingMl == 0) "Target reached" else "$remainingMl ml left"}",
                    onBack = onBack,
                    badgeType = LifeOsBadgeType.WATER
                )
            }

            item {
                LifeOSGlassCard(accentTint = MaterialTheme.colorScheme.secondary) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            AnimatedWaterBottle(
                                progressFraction = progress,
                                currentMl = totalMl,
                                targetMl = targetMl
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Today's Hydration", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "$totalMl ml / $targetMl ml",
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = if (totalMl >= targetMl) "Goal reached! Great hydration." else "$remainingMl ml remaining today.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedButton(
                                    onClick = { showTargetDialog = true },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.testTag("water_edit_goal_button")
                                ) {
                                    Text("Edit Daily Goal")
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StreakStatChip("Target", "${targetMl}ml")
                            StreakStatChip("Consumed", "${totalMl}ml")
                            StreakStatChip("Remaining", "${remainingMl}ml")
                            StreakStatChip("Progress", "${((totalMl.toFloat() / targetMl) * 100f).toInt().coerceIn(0, 100)}%")
                        }
                    }
                }
            }

            // Quick-add hydration buttons (Manual logging only — never auto-logged)
            item {
                LifeOSGlassCard {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Quick Log Water", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            ApplePillBadge(text = "Manual Log", tint = MaterialTheme.colorScheme.secondary)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(100, 200, 250, 500).forEach { ml ->
                                FilledTonalButton(
                                    onClick = { viewModel.addWater(ml, today) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("water_quick_$ml")
                                ) {
                                    Text("+${ml}ml", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customMlText,
                                onValueChange = { customMlText = it.filter(Char::isDigit).take(4) },
                                label = { Text("Custom amount (ml)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    val ml = customMlText.toIntOrNull() ?: 0
                                    if (ml in 10..5000) viewModel.addWater(ml, today)
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("water_add_custom_button")
                            ) {
                                Icon(Icons.Filled.WaterDrop, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add")
                            }
                        }
                    }
                }
            }

            // 7-Day Hydration Progress Chart
            item {
                LifeOSGlassCard {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("7-Day Hydration Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "Goal ${targetMl}ml",
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        val chartMax = maxOf(targetMl, last7Water.maxOfOrNull { it.second } ?: targetMl).coerceAtLeast(250)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(116.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            last7Water.forEach { (iso, ml) ->
                                val barFraction = (ml.toFloat() / chartMax.toFloat()).coerceIn(0.05f, 1f)
                                val reached = ml >= targetMl
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom
                                ) {
                                    Text(
                                        text = if (ml >= 1000) "${"%.1f".format(ml / 1000f)}L" else "${ml}",
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(22.dp)
                                            .height((72f * barFraction).dp)
                                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                                            .background(
                                                when {
                                                    reached -> MaterialTheme.colorScheme.tertiary
                                                    ml > 0 -> MaterialTheme.colorScheme.secondary
                                                    else -> MaterialTheme.colorScheme.surfaceContainerHighest
                                                }
                                            )
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = iso.takeLast(2),
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (iso == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Hydration Reminders (Hourly Default + Custom Interval)
            item {
                LifeOSGlassCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Column {
                                    Text("Hourly Hydration Reminders", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = if (state.profile.waterReminderEnabled) {
                                            "Active every ${state.profile.waterReminderIntervalMin} min • Auto-pauses when target reached"
                                        } else {
                                            "Paused • Enable to receive scheduled drink reminders"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = state.profile.waterReminderEnabled,
                                onCheckedChange = { enabled ->
                                    viewModel.toggleWaterReminder(enabled, state.profile.waterReminderIntervalMin)
                                },
                                modifier = Modifier.testTag("water_reminder_switch")
                            )
                        }
                        AppleSegmentedControl(
                            options = listOf("30m", "60m", "90m", "120m"),
                            selectedIndex = selectedIntervalIdx,
                            onSelect = { idx ->
                                viewModel.toggleWaterReminder(
                                    enabled = state.profile.waterReminderEnabled,
                                    intervalMin = intervalOptions[idx]
                                )
                            }
                        )
                    }
                }
            }

            // Today's Logs
            item {
                Text("Today's Hydration History (${dayLogs.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            if (dayLogs.isEmpty()) {
                item {
                    LifeOSGlassCard {
                        Text(
                            text = "No water logged yet for $today. Tap a quick-log button above whenever you drink water.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(dayLogs, key = { it.id }) { log ->
                    LifeOSGlassCard(cornerRadius = 18.dp) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Column {
                                    Text("${log.amountMl} ml", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text(DateUtils.formatTime(log.createdAtMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = { viewModel.deleteWaterLog(log.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete log", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTargetDialog) {
        AlertDialog(
            onDismissRequest = { showTargetDialog = false },
            title = { Text("Daily Water Goal (ml)") },
            text = {
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it.filter(Char::isDigit).take(5) },
                    label = { Text("Target ml (e.g. 2500)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    val v = targetText.toIntOrNull()?.coerceIn(250, 20000) ?: 2500
                    viewModel.updatePreferences { it.copy(waterTargetMl = v) }
                    showTargetDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showTargetDialog = false }) { Text("Cancel") } }
        )
    }
}

// ---------------- NUTRITION & PROTEIN TRACKER SCREEN ----------------

@Composable
fun NutritionTrackerScreen(
    state: LifeOsUiState,
    viewModel: LifeOsViewModel,
    onBack: () -> Unit
) {
    val today = state.selectedDateIso
    val summary = state.todayNutrition
    val dayEntries = state.nutritionEntries.filter { it.dateIso == today }
    var selectedMeal by remember { mutableStateOf(MealType.BREAKFAST) }
    var showAddFoodDialog by remember { mutableStateOf(false) }
    var showTargetDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .testTag("nutrition_tracker_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 118.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                AppleSubScreenHeader(
                    title = "Nutrition & Protein",
                    subtitle = "${"%.0f".format(summary.totalProteinG)}g protein • ${summary.totalCalories} kcal today",
                    onBack = onBack,
                    badgeType = LifeOsBadgeType.FOOD
                )
            }

            // Summary Macro Card
            item {
                LifeOSGlassCard(accentTint = MaterialTheme.colorScheme.tertiary) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Today's Intake", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { showTargetDialog = true }) { Text("Edit Targets") }
                        }

                        // Protein Progress
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Protein", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${"%.1f".format(summary.totalProteinG)}g / ${summary.proteinTargetG}g",
                                fontFamily = JetBrainsMonoFamily,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { summary.proteinProgressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(999.dp))
                        )

                        // Calories Progress
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Calories", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${summary.totalCalories} / ${summary.calorieTargetKcal} kcal",
                                fontFamily = JetBrainsMonoFamily,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { summary.calorieProgressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(999.dp))
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            StreakStatChip("Carbs", "${"%.1f".format(summary.totalCarbsG)}g")
                            StreakStatChip("Fat", "${"%.1f".format(summary.totalFatG)}g")
                            StreakStatChip("Meals", "${summary.entriesCount}")
                        }
                    }
                }
            }

            // Meal Segmented Control + Custom Add Button
            item {
                val mealEntriesList = MealType.entries
                AppleSegmentedControl(
                    options = mealEntriesList.map { it.label },
                    selectedIndex = mealEntriesList.indexOf(selectedMeal).coerceAtLeast(0),
                    onSelect = { idx -> selectedMeal = mealEntriesList[idx] }
                )
            }

            item {
                Button(
                    onClick = { showAddFoodDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_custom_food_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Log Custom Food to ${selectedMeal.label}")
                }
            }

            // Quick Estimated Food Presets
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Quick Add Common Foods (Estimated)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "Values below are general estimates — tap + to log or use Custom Food for exact package nutrition.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(NutritionEngine.commonEstimatedFoods.take(6), key = { it.name }) { preset ->
                LifeOSGlassCard(cornerRadius = 18.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(preset.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                            Text(
                                text = "${preset.quantityLabel} • Est. ${preset.proteinG}g P • ${preset.calories} kcal • ${preset.carbsG}g C • ${preset.fatG}g F",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalButton(
                            onClick = {
                                viewModel.saveNutritionEntry(
                                    NutritionEntryEntity(
                                        userId = state.userId,
                                        dateIso = today,
                                        mealType = selectedMeal.name,
                                        foodName = preset.name,
                                        quantityLabel = preset.quantityLabel,
                                        proteinG = preset.proteinG,
                                        calories = preset.calories,
                                        carbsG = preset.carbsG,
                                        fatG = preset.fatG,
                                        isEstimated = true
                                    )
                                )
                            }
                        ) { Text("+ Add") }
                    }
                }
            }

            // Logged Entries for Selected Meal
            val mealEntries = dayEntries.filter { it.mealType == selectedMeal.name }
            item {
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))
                Text("${selectedMeal.label} Log (${mealEntries.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            if (mealEntries.isEmpty()) {
                item {
                    LifeOSGlassCard {
                        Text(
                            text = "No foods logged for ${selectedMeal.label} yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            } else {
                items(mealEntries, key = { it.id }) { entry ->
                    LifeOSGlassCard(cornerRadius = 18.dp) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.foodName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "${entry.quantityLabel} • ${entry.proteinG}g protein • ${entry.calories} kcal" +
                                        if (entry.isEstimated) " (Estimated)" else " (Custom)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { viewModel.deleteNutritionEntry(entry.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete entry", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddFoodDialog) {
        var name by remember { mutableStateOf("") }
        var qty by remember { mutableStateOf("1 serving") }
        var protein by remember { mutableStateOf("") }
        var cals by remember { mutableStateOf("") }
        var carbs by remember { mutableStateOf("") }
        var fat by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddFoodDialog = false },
            title = { Text("Add Food • ${selectedMeal.label}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Food name") }, singleLine = true)
                    OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text("Quantity (e.g. 150g)") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = protein,
                            onValueChange = { protein = it },
                            label = { Text("Protein (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = cals,
                            onValueChange = { cals = it.filter(Char::isDigit) },
                            label = { Text("Calories") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = carbs,
                            onValueChange = { carbs = it },
                            label = { Text("Carbs (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = fat,
                            onValueChange = { fat = it },
                            label = { Text("Fat (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = name.isNotBlank(),
                    onClick = {
                        viewModel.saveNutritionEntry(
                            NutritionEntryEntity(
                                userId = state.userId,
                                dateIso = today,
                                mealType = selectedMeal.name,
                                foodName = name.trim(),
                                quantityLabel = qty.ifBlank { "1 serving" },
                                proteinG = protein.toDoubleOrNull() ?: 0.0,
                                calories = cals.toIntOrNull() ?: 0,
                                carbsG = carbs.toDoubleOrNull() ?: 0.0,
                                fatG = fat.toDoubleOrNull() ?: 0.0,
                                isEstimated = false
                            )
                        )
                        showAddFoodDialog = false
                    }
                ) { Text("Save Food") }
            },
            dismissButton = { TextButton(onClick = { showAddFoodDialog = false }) { Text("Cancel") } }
        )
    }

    if (showTargetDialog) {
        var pTarget by remember { mutableStateOf(state.profile.proteinTargetG.toString()) }
        var cTarget by remember { mutableStateOf(state.profile.calorieTargetKcal.toString()) }
        AlertDialog(
            onDismissRequest = { showTargetDialog = false },
            title = { Text("Daily Nutrition Targets") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pTarget,
                        onValueChange = { pTarget = it.filter(Char::isDigit).take(3) },
                        label = { Text("Daily Protein Target (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = cTarget,
                        onValueChange = { cTarget = it.filter(Char::isDigit).take(4) },
                        label = { Text("Daily Calorie Target (kcal)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateProfile {
                        it.copy(
                            proteinTargetG = pTarget.toIntOrNull()?.coerceIn(10, 500) ?: it.proteinTargetG,
                            calorieTargetKcal = cTarget.toIntOrNull()?.coerceIn(800, 10000) ?: it.calorieTargetKcal
                        )
                    }
                    showTargetDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showTargetDialog = false }) { Text("Cancel") } }
        )
    }
}
