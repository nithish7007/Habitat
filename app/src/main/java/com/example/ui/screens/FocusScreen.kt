package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DateUtils
import com.example.data.FocusMode
import com.example.ui.LifeOsUiState
import com.example.ui.LifeOsViewModel
import com.example.ui.components.AppleSubScreenHeader
import com.example.ui.components.LifeOsBadgeIcon
import com.example.ui.components.LifeOsBadgeType
import com.example.data.MascotMood
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.ReactiveMinimalMascot
import com.example.ui.theme.JetBrainsMonoFamily

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusScreen(
    state: LifeOsUiState,
    viewModel: LifeOsViewModel,
    onBack: (() -> Unit)? = null
) {
    val timer = state.timerState
    val elapsed = state.liveElapsedSeconds
    val targetSec = (if (timer.isBreakPhase) timer.breakMinutes else timer.plannedMinutes) * 60
    val displaySec = if (timer.mode == FocusMode.STOPWATCH) elapsed else (targetSec - elapsed).coerceAtLeast(0)
    val mins = displaySec / 60
    val secs = displaySec % 60

    val categories = listOf("Java", "SQL", "DSA", "React", "System Design", "Deep Work")
    var customMinInput by remember { mutableStateOf(timer.plannedMinutes.toString()) }

    val todayMinutes = state.focusSessions.filter { it.dateIso == DateUtils.todayIso() }.sumOf { it.actualSeconds } / 60
    val dailyTarget = state.preferences.focusTargetMinutes.coerceAtLeast(15)

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 116.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "focus_header") {
                if (onBack != null) {
                    AppleSubScreenHeader(
                        title = "Focus Studio",
                        subtitle = "Today: ${todayMinutes}m / ${dailyTarget}m goal",
                        onBack = onBack,
                        badgeType = LifeOsBadgeType.FOCUS,
                        trailing = {
                            ReactiveMinimalMascot(
                                mood = if (timer.running) MascotMood.HAPPY else MascotMood.NEUTRAL,
                                size = 46.dp
                            )
                        }
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            LifeOsBadgeIcon(type = LifeOsBadgeType.FOCUS, size = 44.dp)
                            Column {
                                Text("Focus Studio", style = MaterialTheme.typography.displaySmall)
                                Text(
                                    text = "Today: ${todayMinutes}m / ${dailyTarget}m goal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        ReactiveMinimalMascot(
                            mood = if (timer.running) MascotMood.HAPPY else MascotMood.NEUTRAL,
                            size = 54.dp
                        )
                    }
                }
            }

            item(key = "focus_modes") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FocusMode.entries.forEach { m ->
                        FilterChip(
                            selected = timer.mode == m,
                            onClick = {
                                val minsForMode = when (m) {
                                    FocusMode.POMODORO -> 25
                                    FocusMode.CUSTOM -> customMinInput.toIntOrNull() ?: 45
                                    FocusMode.COUNTDOWN -> 30
                                    FocusMode.STOPWATCH -> 0
                                }
                                viewModel.configureFocusTimer(m, timer.category, minsForMode.coerceAtLeast(1), 5)
                            },
                            label = { Text(m.name) }
                        )
                    }
                }
            }

            item(key = "focus_timer_card") {
                LiquidGlassCard(
                    cornerRadius = 28.dp,
                    accentTint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "${timer.mode.name} • ${timer.category}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "%02d:%02d".format(mins, secs),
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 54.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("focus_timer_display")
                        )
                        if (timer.mode != FocusMode.STOPWATCH && targetSec > 0) {
                            LinearProgressIndicator(
                                progress = { (elapsed.toFloat() / targetSec).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(999.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                strokeCap = StrokeCap.Round
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { viewModel.startOrPauseTimer() },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("focus_play_pause_button")
                            ) {
                                Icon(if (timer.running) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(if (timer.running) "Pause" else "Start")
                            }
                            FilledTonalButton(
                                onClick = { viewModel.finishAndSaveFocusSession() },
                                enabled = elapsed >= 10,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.height(50.dp).testTag("focus_save_session_button")
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Log")
                            }
                            OutlinedButton(
                                onClick = { viewModel.resetFocusTimer() },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.height(50.dp)
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = "Reset timer")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Distractions: ${timer.distractionsCount}",
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(onClick = { viewModel.logFocusDistraction() }) {
                                Icon(Icons.Filled.WarningAmber, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Log Distraction")
                            }
                        }
                    }
                }
            }

            item(key = "focus_categories_notes") {
                LiquidGlassCard(
                    cornerRadius = 22.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Study / Work Category", style = MaterialTheme.typography.titleSmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            categories.forEach { cat ->
                                FilterChip(
                                    selected = timer.category == cat,
                                    onClick = { viewModel.configureFocusTimer(timer.mode, cat, timer.plannedMinutes, timer.breakMinutes) },
                                    label = { Text(cat) }
                                )
                            }
                        }
                        if (timer.mode == FocusMode.CUSTOM || timer.mode == FocusMode.COUNTDOWN) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = customMinInput,
                                    onValueChange = { customMinInput = it.filter(Char::isDigit).take(3) },
                                    label = { Text("Minutes") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                FilledTonalButton(onClick = {
                                    val m = customMinInput.toIntOrNull()?.coerceIn(1, 240) ?: 25
                                    viewModel.configureFocusTimer(timer.mode, timer.category, m, timer.breakMinutes)
                                }) { Text("Apply") }
                            }
                        }
                        OutlinedTextField(
                            value = timer.sessionNotes,
                            onValueChange = { viewModel.updateFocusNotes(it) },
                            label = { Text("Session Notes (optional)") },
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item(key = "focus_history_title") {
                Text("Recent Focus Sessions (${state.focusSessions.size})", style = MaterialTheme.typography.titleMedium)
            }

            if (state.focusSessions.isEmpty()) {
                item(key = "focus_empty_history") {
                    Text(
                        text = "Complete at least 10 seconds and tap Log to build your focus history.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(state.focusSessions.take(10), key = { it.id }) { session ->
                    LiquidGlassCard(
                        cornerRadius = 18.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${session.category} • ${session.mode}", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${session.dateIso} • ${session.distractionsCount} distractions${if (session.notes.isNotBlank()) " • ${session.notes}" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${session.actualSeconds / 60}m ${session.actualSeconds % 60}s",
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
