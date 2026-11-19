package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.NotificationCategory
import com.example.ui.LifeOsUiState
import com.example.ui.LifeOsViewModel
import com.example.ui.components.ApplePillBadge
import com.example.ui.components.AppleSectionHeader
import com.example.ui.components.AppleSubScreenHeader
import com.example.ui.components.LifeOsBadgeIcon
import com.example.ui.components.LifeOsBadgeType
import com.example.ui.components.LiquidGlassCard
import com.example.ui.theme.LocalLifeOsGlass
import com.example.ui.theme.WarmAmber
import com.example.worker.LifeOsNotificationCenter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SmartNotificationCenterScreen(
    state: LifeOsUiState,
    viewModel: LifeOsViewModel,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val glass = LocalLifeOsGlass.current
    val prefs = state.notificationPrefs

    var hasPermission by remember { mutableStateOf(LifeOsNotificationCenter.hasNotificationPermission(context)) }
    val dndActive = remember(state.liveElapsedSeconds) { LifeOsNotificationCenter.isDoNotDisturbActive(context) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted || LifeOsNotificationCenter.hasNotificationPermission(context)
        feedbackMessage = if (hasPermission) {
            "Notification permission granted. Enabled categories are now active."
        } else {
            "Notification permission denied. You can enable it later in Android Settings."
        }
        if (hasPermission) {
            viewModel.updateNotificationPrefs { it }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("smart_notification_center_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 116.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            AppleSubScreenHeader(
                title = "Smart Notifications",
                subtitle = "13 categories • Quiet hours • Instant previews",
                onBack = onBack,
                badgeType = LifeOsBadgeType.NOTIFICATIONS
            )
        }

        // Permission & Master Pause Card
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                accentTint = if (hasPermission) scheme.tertiary else WarmAmber
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (hasPermission) Icons.Rounded.VerifiedUser else Icons.Rounded.NotificationsOff,
                                contentDescription = null,
                                tint = if (hasPermission) scheme.tertiary else WarmAmber
                            )
                            Column {
                                Text(
                                    text = if (hasPermission) "Notifications Enabled" else "Permission Required",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (dndActive) {
                                        "System Do Not Disturb is active — alerts paused."
                                    } else if (hasPermission) {
                                        "WorkManager schedules sync automatically with your toggles."
                                    } else {
                                        "Allow Android notifications to receive reminders and updates."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onSurfaceVariant
                                )
                            }
                        }
                        if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("request_notif_permission_button")
                            ) {
                                Text("Allow")
                            }
                        }
                    }

                    HorizontalDivider(thickness = 0.6.dp, color = glass.separator)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (prefs.pauseAll) "All Notifications Paused" else "Master Notification Switch",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (prefs.pauseAll) {
                                    "Tap switch to resume all scheduled LIFE OS reminders."
                                } else {
                                    "Pause all notifications temporarily with one tap."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = !prefs.pauseAll,
                            onCheckedChange = { active ->
                                viewModel.updateNotificationPrefs { it.copy(pauseAll = !active) }
                            },
                            modifier = Modifier.testTag("master_notification_switch")
                        )
                    }

                    if (!feedbackMessage.isNullOrBlank()) {
                        Text(
                            text = feedbackMessage.orEmpty(),
                            style = MaterialTheme.typography.labelMedium,
                            color = scheme.primary
                        )
                    }
                }
            }
        }

        // Schedule & Quiet Hours
        item {
            AppleSectionHeader(
                title = "Timing & Quiet Hours",
                subtitle = "Intervals, daily summary time, and audio preferences"
            )
        }

        item {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Water Reminder Interval",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(30 to "30m", 60 to "1 hr", 90 to "1.5 hr", 120 to "2 hr").forEach { (mins, label) ->
                            FilterChip(
                                selected = prefs.waterIntervalMinutes == mins,
                                onClick = {
                                    viewModel.updateNotificationPrefs { it.copy(waterIntervalMinutes = mins) }
                                },
                                label = { Text(label) }
                            )
                        }
                    }

                    HorizontalDivider(thickness = 0.6.dp, color = glass.separator)

                    Text(
                        text = "Daily Summary Time",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Triple(19, 0, "7:00 PM"),
                            Triple(20, 30, "8:30 PM"),
                            Triple(21, 30, "9:30 PM"),
                            Triple(22, 0, "10:00 PM")
                        ).forEach { (h, m, label) ->
                            FilterChip(
                                selected = prefs.dailySummaryHour == h && prefs.dailySummaryMinute == m,
                                onClick = {
                                    viewModel.updateNotificationPrefs {
                                        it.copy(dailySummaryHour = h, dailySummaryMinute = m)
                                    }
                                },
                                label = { Text(label) }
                            )
                        }
                    }

                    HorizontalDivider(thickness = 0.6.dp, color = glass.separator)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Quiet Hours (${prefs.quietStartHour}:00 – ${prefs.quietEndHour}:00)", style = MaterialTheme.typography.titleSmall)
                            Text("Suppress automated alerts while sleeping", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = prefs.quietHoursEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.updateNotificationPrefs { it.copy(quietHoursEnabled = enabled) }
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Notification Sound", style = MaterialTheme.typography.titleSmall)
                        Switch(
                            checked = prefs.soundEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.updateNotificationPrefs { it.copy(soundEnabled = enabled) }
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Vibration", style = MaterialTheme.typography.titleSmall)
                        Switch(
                            checked = prefs.vibrationEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.updateNotificationPrefs { it.copy(vibrationEnabled = enabled) }
                            }
                        )
                    }

                    Text(
                        text = "Note: Android WorkManager respects Doze and battery optimization. Exact delivery timing may shift by a few minutes to preserve battery.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }
        }

        // 13 Individual Notification Categories
        item {
            AppleSectionHeader(
                title = "Notification Categories (13)",
                subtitle = "Toggle any category and tap Preview to test deep links"
            )
        }

        item {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    NotificationCategory.entries.forEachIndexed { idx, category ->
                        val enabled = prefs.isCategoryEnabled(category)
                        val badge = badgeForCategory(category)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LifeOsBadgeIcon(type = badge, size = 38.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = category.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = category.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onSurfaceVariant
                                )
                                TextButton(
                                    onClick = {
                                        viewModel.sendNotificationPreview(category) { sent ->
                                            feedbackMessage = if (sent) {
                                                "Sent preview notification for ${category.label}."
                                            } else {
                                                "Grant notification permission first to see previews."
                                            }
                                        }
                                    },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(2.dp))
                                    Text("Preview", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            Switch(
                                checked = enabled,
                                enabled = !prefs.pauseAll,
                                onCheckedChange = { checked ->
                                    viewModel.toggleNotificationCategory(category, checked)
                                },
                                modifier = Modifier.testTag("notif_switch_${category.id}")
                            )
                        }
                        if (idx < NotificationCategory.entries.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 64.dp),
                                thickness = 0.6.dp,
                                color = glass.separator
                            )
                        }
                    }
                }
            }
        }

        // Notification History
        item {
            AppleSectionHeader(
                title = "Notification History",
                subtitle = "${state.notificationLogs.size} delivered alert(s)",
                actionLabel = if (state.notificationLogs.isNotEmpty()) "Clear All" else null,
                onAction = if (state.notificationLogs.isNotEmpty()) {
                    { viewModel.clearNotificationLogs() }
                } else {
                    null
                }
            )
        }

        if (state.notificationLogs.isEmpty()) {
            item {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = scheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(30.dp)
                        )
                        Text("No notifications delivered yet", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Tap Preview on any category above to test alerts and deep-link navigation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(state.notificationLogs, key = { it.id }) { log ->
                val formattedTime = remember(log.createdAtMs) {
                    SimpleDateFormat("MMM d • HH:mm", Locale.getDefault()).format(Date(log.createdAtMs))
                }
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp,
                    onClick = { onNavigate(log.deepLinkRoute) }
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ApplePillBadge(text = log.categoryId.uppercase(), tint = scheme.primary)
                            Text(
                                text = formattedTime,
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant
                            )
                        }
                        Text(log.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(log.message, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

private fun badgeForCategory(category: NotificationCategory): LifeOsBadgeType = when (category) {
    NotificationCategory.WATER -> LifeOsBadgeType.WATER
    NotificationCategory.STEPS -> LifeOsBadgeType.STEPS
    NotificationCategory.GOALS -> LifeOsBadgeType.GOALS
    NotificationCategory.TASKS -> LifeOsBadgeType.TASKS
    NotificationCategory.HABITS -> LifeOsBadgeType.STREAK
    NotificationCategory.TECH_NEWS -> LifeOsBadgeType.NEWS
    NotificationCategory.YOUTUBE_VIDEOS -> LifeOsBadgeType.VIDEO
    NotificationCategory.LEARNING -> LifeOsBadgeType.LEARNING
    NotificationCategory.CLASSES -> LifeOsBadgeType.CALENDAR
    NotificationCategory.INTERVIEWS -> LifeOsBadgeType.CAREER
    NotificationCategory.FINANCE -> LifeOsBadgeType.FINANCE
    NotificationCategory.MOTIVATION -> LifeOsBadgeType.MOTIVATION
    NotificationCategory.DAILY_SUMMARY -> LifeOsBadgeType.INSIGHTS
}
