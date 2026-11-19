package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.DateUtils
import com.example.data.TaskStatus
import com.example.ui.LifeOsUiState
import com.example.ui.components.ApplePillBadge
import com.example.ui.components.AppleSubScreenHeader
import com.example.ui.components.LifeOsBadgeIcon
import com.example.ui.components.LifeOsBadgeType
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.WeeklyTrendBarChart
import com.example.ui.theme.JetBrainsMonoFamily

@Composable
fun InsightsScreen(
    state: LifeOsUiState,
    onBack: (() -> Unit)? = null
) {
    val last7 = DateUtils.lastNDaysIso(7, state.selectedDateIso)
    val weeklyFocusMins = last7.map { d ->
        DateUtils.formatShortDay(d) to (state.focusSessions.filter { it.dateIso == d }.sumOf { it.actualSeconds } / 60)
    }
    val weeklyWater = last7.map { d ->
        DateUtils.formatShortDay(d) to state.waterLogs.filter { it.dateIso == d }.sumOf { it.amountMl }
    }
    val completedTasks = state.tasks.count { it.status == TaskStatus.COMPLETED.name }
    val totalFocusMins = state.focusSessions.sumOf { it.actualSeconds } / 60
    val totalStepsWeek = state.stepLogs.filter { it.dateIso in last7 }.sumOf { it.steps }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 116.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "insights_header") {
                if (onBack != null) {
                    AppleSubScreenHeader(
                        title = "Life Insights",
                        subtitle = "Analytics from your verified records",
                        onBack = onBack,
                        badgeType = LifeOsBadgeType.INSIGHTS
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LifeOsBadgeIcon(type = LifeOsBadgeType.INSIGHTS, size = 44.dp)
                        Column {
                            Text("Life Insights", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Analytics computed strictly from your verified records",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item(key = "yesterday_vs_today") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    SummaryStatCard(
                        title = "Yesterday",
                        value = state.yesterdayScore.scorePercent?.let { "$it%" } ?: "--",
                        subtitle = DateUtils.yesterdayIso(state.selectedDateIso),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryStatCard(
                        title = "Selected Day",
                        value = state.todayScore.scorePercent?.let { "$it%" } ?: "--",
                        subtitle = state.selectedDateIso,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item(key = "kpi_row") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    SummaryStatCard("Tasks Done", "$completedTasks", "All time", Modifier.weight(1f))
                    SummaryStatCard("Focus", "${totalFocusMins}m", "Logged", Modifier.weight(1f))
                    SummaryStatCard("7d Steps", "$totalStepsWeek", "Tracked", Modifier.weight(1f))
                }
            }

            item(key = "weekly_score_trend") {
                LiquidGlassCard(
                    cornerRadius = 24.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("7-Day Productivity Score (%)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        WeeklyTrendBarChart(
                            labels = state.weeklyScores.map { it.first },
                            values = state.weeklyScores.map { it.second },
                            maxValue = 100,
                            barColor = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            item(key = "weekly_focus_trend") {
                LiquidGlassCard(
                    cornerRadius = 24.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("7-Day Focus Minutes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        WeeklyTrendBarChart(
                            labels = weeklyFocusMins.map { it.first },
                            values = weeklyFocusMins.map { it.second },
                            maxValue = (weeklyFocusMins.maxOfOrNull { it.second } ?: 60).coerceAtLeast(30),
                            barColor = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            item(key = "weekly_water_trend") {
                LiquidGlassCard(
                    cornerRadius = 24.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("7-Day Hydration (ml)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        WeeklyTrendBarChart(
                            labels = weeklyWater.map { it.first },
                            values = weeklyWater.map { it.second },
                            maxValue = state.preferences.waterTargetMl.coerceAtLeast(1000),
                            barColor = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            item(key = "suggestions_title") {
                Text("Data-Driven Suggestions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(state.insights, key = { it.title }) { s ->
                LiquidGlassCard(
                    cornerRadius = 20.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ApplePillBadge(text = s.category.uppercase(), tint = MaterialTheme.colorScheme.primary)
                        Text(s.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(s.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStatCard(title: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    LiquidGlassCard(
        cornerRadius = 20.dp,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontFamily = JetBrainsMonoFamily, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
