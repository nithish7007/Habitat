package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.DateUtils
import com.example.data.TaskStatus
import com.example.ui.LifeOsUiState
import com.example.ui.components.ApplePillBadge
import com.example.ui.components.LifeOsBadgeIcon
import com.example.ui.components.LifeOsBadgeType
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.ReactiveMinimalMascot
import java.util.Locale

@Composable
fun LifeHubScreen(
    state: LifeOsUiState,
    onNavigate: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val today = state.selectedDateIso
    val todayTasks = state.tasks.filter { it.dueDateIso == today }
    val pendingTasks = todayTasks.count { it.status != TaskStatus.COMPLETED.name }
    val waterMl = state.waterLogs.filter { it.dateIso == today }.sumOf { it.amountMl }
    val todaySteps = maxOf(
        state.stepLogs.firstOrNull { it.dateIso == today }?.steps ?: 0,
        if (today == DateUtils.todayIso()) state.stepSensorStatus.todaySensorSteps else 0
    )
    val focusMin = state.focusSessions.filter { it.dateIso == today }.sumOf { it.actualSeconds } / 60
    val streak = state.streakReport
    val nutrition = state.todayNutrition

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .testTag("life_hub_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 118.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "life_header") {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Life & Wellness",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tasks, habits, hydration, steps, nutrition, memories & notes",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }

            // Duolingo-Style Streak Hero Card
            item(key = "streak_hero") {
                LiquidGlassCard(
                    accentTint = scheme.primary,
                    onClick = { onNavigate("streak") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("life_streak_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ReactiveMinimalMascot(
                            mood = streak.mascotMood,
                            celebrate = streak.qualifiedToday,
                            size = 68.dp
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${streak.currentStreak}-Day Streak",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                ApplePillBadge(
                                    text = "LVL ${streak.level} • ${streak.totalXp} XP",
                                    tint = scheme.tertiary
                                )
                            }
                            Text(
                                text = streak.mascotStatusLine,
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant
                            )
                            LinearProgressIndicator(
                                progress = { (streak.xpInCurrentLevel.toFloat() / streak.xpForNextLevel).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(999.dp))
                            )
                        }
                    }
                }
            }

            // Core Productivity Pair
            item(key = "productivity_row") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LifeModuleTile(
                        title = "Tasks & Plan",
                        subtitle = "$pendingTasks pending today",
                        badge = LifeOsBadgeType.TASKS,
                        onClick = { onNavigate("plan") },
                        modifier = Modifier.weight(1f).testTag("life_tile_plan")
                    )
                    LifeModuleTile(
                        title = "Habits & Streak",
                        subtitle = "${state.habits.size} habits • ${state.challenges.size} challenges",
                        badge = LifeOsBadgeType.STREAK,
                        onClick = { onNavigate("track") },
                        modifier = Modifier.weight(1f).testTag("life_tile_track")
                    )
                }
            }

            // Health: Water & Steps Pair
            item(key = "health_pair_row") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LifeModuleTile(
                        title = "Water Tracker",
                        subtitle = "$waterMl / ${state.preferences.waterTargetMl} ml",
                        badge = LifeOsBadgeType.WATER,
                        onClick = { onNavigate("water") },
                        modifier = Modifier.weight(1f).testTag("life_tile_water")
                    )
                    LifeModuleTile(
                        title = "Steps & Walking",
                        subtitle = "${String.format(Locale.getDefault(), "%,d", todaySteps)} / ${state.preferences.stepsTarget}",
                        badge = LifeOsBadgeType.STEPS,
                        onClick = { onNavigate("steps") },
                        modifier = Modifier.weight(1f).testTag("life_tile_steps")
                    )
                }
            }

            // Nutrition & Focus Pair
            item(key = "nutrition_focus_row") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LifeModuleTile(
                        title = "Nutrition & Protein",
                        subtitle = "${"%.0f".format(nutrition.totalProteinG)}g / ${nutrition.proteinTargetG}g protein",
                        badge = LifeOsBadgeType.FOOD,
                        onClick = { onNavigate("nutrition") },
                        modifier = Modifier.weight(1f).testTag("life_tile_nutrition")
                    )
                    LifeModuleTile(
                        title = "Focus Studio",
                        subtitle = "$focusMin min focused today",
                        badge = LifeOsBadgeType.FOCUS,
                        onClick = { onNavigate("focus") },
                        modifier = Modifier.weight(1f).testTag("life_tile_focus")
                    )
                }
            }

            // Dedicated Private Memories vs Separate Notes & Code Pair
            item(key = "memories_notes_row") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LifeModuleTile(
                        title = "Memories",
                        subtitle = "${state.memories.size} private moments",
                        badge = LifeOsBadgeType.MEMORIES,
                        onClick = { onNavigate("memories") },
                        modifier = Modifier.weight(1f).testTag("life_tile_memories")
                    )
                    LifeModuleTile(
                        title = "Notes & Code",
                        subtitle = "${state.notes.size} structured notes",
                        badge = LifeOsBadgeType.NOTES,
                        onClick = { onNavigate("notes") },
                        modifier = Modifier.weight(1f).testTag("life_tile_notes")
                    )
                }
            }

            // Goals & Daily Journal Pair
            item(key = "reflection_row") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LifeModuleTile(
                        title = "Goals",
                        subtitle = "${state.goals.size} active goals",
                        badge = LifeOsBadgeType.GOALS,
                        onClick = { onNavigate("goals") },
                        modifier = Modifier.weight(1f)
                    )
                    LifeModuleTile(
                        title = "Daily Journal",
                        subtitle = "${state.journalEntries.size} entries saved",
                        badge = LifeOsBadgeType.JOURNAL,
                        onClick = { onNavigate("journal") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Routines & Insights Pair
            item(key = "routines_insights_row") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LifeModuleTile(
                        title = "Routines",
                        subtitle = "${state.routines.size} daily routines",
                        badge = LifeOsBadgeType.ROUTINES,
                        onClick = { onNavigate("routines") },
                        modifier = Modifier.weight(1f)
                    )
                    LifeModuleTile(
                        title = "Insights",
                        subtitle = state.todayScore.scorePercent?.let { "Today: $it% score" } ?: "Weekly analytics",
                        badge = LifeOsBadgeType.INSIGHTS,
                        onClick = { onNavigate("insights") },
                        modifier = Modifier.weight(1f).testTag("life_tile_insights")
                    )
                }
            }

            // Motivation & Smart Notifications Pair
            item(key = "motivation_notifications_row") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LifeModuleTile(
                        title = "Motivation Goal",
                        subtitle = "Daily quotes + AI original",
                        badge = LifeOsBadgeType.MOTIVATION,
                        onClick = { onNavigate("motivation_goal") },
                        modifier = Modifier.weight(1f)
                    )
                    LifeModuleTile(
                        title = "Smart Alerts",
                        subtitle = "13 notification channels",
                        badge = LifeOsBadgeType.NOTIFICATIONS,
                        onClick = { onNavigate("notifications") },
                        modifier = Modifier.weight(1f).testTag("life_tile_notifications")
                    )
                }
            }

            item(key = "ai_cta") {
                FilledTonalButton(
                    onClick = { onNavigate("ai_assistant") },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("life_open_ai_button")
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Ask LIFE OS AI Assistant about your day")
                }
            }
        }
    }
}

@Composable
private fun LifeModuleTile(
    title: String,
    subtitle: String,
    badge: LifeOsBadgeType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassCard(
        onClick = onClick,
        cornerRadius = 22.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LifeOsBadgeIcon(type = badge, size = 40.dp)
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}
