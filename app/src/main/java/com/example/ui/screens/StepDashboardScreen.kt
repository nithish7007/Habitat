package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.SensorsOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.health.connect.client.PermissionController
import com.example.data.NotificationCategory
import com.example.ui.LifeOsUiState
import com.example.ui.LifeOsViewModel
import com.example.ui.components.AppleSectionHeader
import com.example.ui.components.AppleSegmentedControl
import com.example.ui.components.AppleSubScreenHeader
import com.example.ui.components.LifeOSGlassCard
import com.example.ui.components.LifeOsBadgeType
import com.example.ui.components.StepProgressCard
import com.example.ui.theme.WarmAmber
import java.util.Locale

@Composable
fun StepDashboardScreen(
    state: LifeOsUiState,
    viewModel: LifeOsViewModel,
    onBack: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val stepState = state.stepTracking
    val goal = stepState.dailyGoal
    var goalInput by remember(goal) { mutableStateOf(goal.toString()) }
    var historyTab by remember { mutableIntStateOf(0) } // 0 = Weekly, 1 = Monthly
    var permissionDeniedNote by remember { mutableStateOf(false) }

    val activityPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionDeniedNote = !granted
        viewModel.refreshStepSensor()
    }

    val healthConnectPermLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { _ ->
        viewModel.syncHealthConnectSteps()
    }

    LaunchedEffect(Unit) {
        viewModel.refreshStepSensor()
    }

    val requestSensorPermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activityPermLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        } else {
            viewModel.refreshStepSensor()
        }
    }

    val requestHealthConnect: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                healthConnectPermLauncher.launch(viewModel.healthConnectRequiredPermissions)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .testTag("step_dashboard_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 118.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "steps_header") {
                AppleSubScreenHeader(
                    title = "Automatic Steps",
                    subtitle = "Hardware sensor & Health Connect • Zero manual entry",
                    onBack = onBack,
                    badgeType = LifeOsBadgeType.STEPS
                )
            }

            // Primary Automatic Step Progress Ring & Live Metrics Card
            item(key = "steps_hero_card") {
                StepProgressCard(
                    stepState = stepState,
                    onEnableActivityPermission = if (stepState.sensorAvailable && !stepState.activityPermissionGranted) {
                        requestSensorPermission
                    } else null,
                    onConnectHealthConnect = if (stepState.healthConnectAvailable && !stepState.healthConnectGranted) {
                        requestHealthConnect
                    } else null
                )
            }

            // Key Summary Metrics Trio
            item(key = "steps_metrics_row") {
                LifeOSGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StepStatPill(
                            label = if (stepState.distanceIsMeasured) "Distance" else "Est. Distance",
                            value = stepState.distanceKm?.let {
                                String.format(Locale.getDefault(), "%.2f km", it)
                            } ?: "—"
                        )
                        StepStatPill(
                            label = "7-Day Avg",
                            value = String.format(Locale.getDefault(), "%,d", stepState.weeklyAverage)
                        )
                        StepStatPill(
                            label = "30-Day Total",
                            value = String.format(Locale.getDefault(), "%,d", stepState.monthlyTotal)
                        )
                    }
                }
            }

            // Hardware Sensor & Health Connect Source Status + Permission Rationale
            item(key = "steps_sources_card") {
                LifeOSGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentTint = when {
                        stepState.isTrackingUnavailable -> WarmAmber
                        stepState.sensorTrackingActive || stepState.healthConnectGranted -> scheme.tertiary
                        else -> scheme.primary
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (stepState.isTrackingUnavailable) Icons.Rounded.SensorsOff else Icons.Rounded.Sensors,
                                contentDescription = null,
                                tint = if (stepState.isTrackingUnavailable) WarmAmber else scheme.tertiary,
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Automatic Detection Sources",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stepState.statusMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onSurfaceVariant
                                )
                            }
                        }

                        if (permissionDeniedNote && !stepState.activityPermissionGranted) {
                            Text(
                                text = "Physical Activity permission was denied. LIFE OS needs this permission exclusively to read your device's built-in hardware step counter automatically. You can grant it anytime from here or Android Settings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = WarmAmber
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (stepState.sensorAvailable && !stepState.activityPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                Button(
                                    onClick = requestSensorPermission,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("grant_activity_recognition_button")
                                ) {
                                    Text("Allow Activity")
                                }
                            }
                            if (stepState.healthConnectAvailable && !stepState.healthConnectGranted) {
                                FilledTonalButton(
                                    onClick = requestHealthConnect,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("connect_health_connect_button")
                                ) {
                                    Icon(Icons.Rounded.HealthAndSafety, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Health Connect")
                                }
                            } else if (stepState.healthConnectGranted) {
                                FilledTonalButton(
                                    onClick = { viewModel.syncHealthConnectSteps() },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("sync_health_connect_button")
                                ) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Sync Health Connect")
                                }
                            }
                        }
                    }
                }
            }

            // Weekly / Monthly History Apple Segmented Control
            item(key = "steps_history_tabs") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppleSectionHeader(
                        title = "Step History",
                        subtitle = "Automatically recorded by local calendar day"
                    )
                    AppleSegmentedControl(
                        options = listOf("Weekly (7 Days)", "Monthly (30 Days)"),
                        selectedIndex = historyTab,
                        onSelect = { historyTab = it }
                    )
                }
            }

            if (historyTab == 0) {
                // Weekly Bar Chart
                item(key = "steps_weekly_chart") {
                    LifeOSGlassCard(modifier = Modifier.fillMaxWidth()) {
                        val maxSteps = maxOf(goal, stepState.weeklyHistory.maxOfOrNull { it.steps } ?: 1)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                                .height(152.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            stepState.weeklyHistory.forEach { rec ->
                                val frac = (rec.steps.toFloat() / maxSteps).coerceIn(0.06f, 1f)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (rec.steps >= 1000) "${rec.steps / 1000}k" else "${rec.steps}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = scheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.54f)
                                            .height((98.dp * frac).coerceAtLeast(8.dp))
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (rec.steps >= goal) scheme.tertiary else scheme.primary.copy(alpha = 0.74f)
                                            )
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(rec.dayLabel, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            } else {
                // 30-Day Monthly Intensity Grid
                item(key = "steps_monthly_grid") {
                    LifeOSGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "30-Day Consistency • ${String.format(Locale.getDefault(), "%,d", stepState.monthlyTotal)} total steps",
                                style = MaterialTheme.typography.labelLarge,
                                color = scheme.onSurface
                            )
                            stepState.monthlyHistory.chunked(7).forEach { week ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    week.forEach { rec ->
                                        val ratio = (rec.steps.toFloat() / goal).coerceIn(0f, 1f)
                                        val cellColor = when {
                                            ratio >= 1f -> scheme.tertiary
                                            ratio >= 0.5f -> scheme.tertiary.copy(alpha = 0.55f)
                                            ratio > 0f -> scheme.primary.copy(alpha = 0.35f)
                                            else -> scheme.surfaceVariant.copy(alpha = 0.55f)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(26.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(cellColor)
                                        )
                                    }
                                    repeat(7 - week.size) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Automatic Step Notifications Toggle
            item(key = "steps_notifications_card") {
                LifeOSGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = scheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Automatic Step Notifications",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Walking nudges, 50% progress, and 100% goal achieved alerts using real calculated steps.",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.notificationPrefs.stepsEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.toggleNotificationCategory(NotificationCategory.STEPS, enabled)
                            }
                        )
                    }
                }
            }

            // Daily Step Target Configuration (Goal setting only — zero manual step entry)
            item(key = "steps_goal_card") {
                LifeOSGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Daily Step Goal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Set your daily automatic walking goal. Steps are always counted automatically from your device sensor or Health Connect.",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = goalInput,
                                onValueChange = { goalInput = it.filter(Char::isDigit).take(6) },
                                label = { Text("Daily Target Steps") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    val next = goalInput.toIntOrNull()?.coerceIn(1000, 100_000) ?: 10_000
                                    viewModel.updatePreferences { it.copy(stepsTarget = next) }
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("save_step_goal_button")
                            ) {
                                Text("Save Goal")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepStatPill(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
